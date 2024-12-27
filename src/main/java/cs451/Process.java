package cs451;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Process {
    private final int processId;
    private final int totalProcesses;
    private final Host myHost;
    private final List<Host> hosts;
    private final AckTracker ackTracker;
    private final FIFOBuffer fifoBuffer;
    private final NetworkSimulator networkSimulator;
    private final DatagramSocket socket;
    private final BufferedWriter writer;
    private final Timer retryTimer = new Timer(true);
    private final int numberOfProposals;
    // TODO: make everything concurrent
    private final List<Set<Integer>> proposals;
    private final List<Integer> proposalNb; // (ProposalId -> ProposalNb)
    private final List<Boolean> decided; // (ProposalId -> Decided (True: decided; False: not decided)
    private int currentId;

    private static final int MAX_RETRIES = 5;
    private static final long RETRY_INTERVAL = 1000;

    public Process(int processId, int totalProcesses, Host myHost, List<Host> hosts, String outputFile, List<Set<Integer>> proposals) throws IOException {
        this.processId = processId;
        this.totalProcesses = totalProcesses;
        this.myHost = myHost;
        this.hosts = hosts;
        this.ackTracker = new AckTracker();
        this.fifoBuffer = new FIFOBuffer(totalProcesses);
        this.networkSimulator = new NetworkSimulator();
        this.socket = new DatagramSocket(myHost.getPort());
        this.proposals = proposals;
        this.numberOfProposals = proposals.size();
        // TODO initialize proposalNb and decided to 0 for each index
        this.proposalNb = new ArrayList<>();
        this.decided = new ArrayList<>();
        this.writer = new BufferedWriter(new FileWriter(outputFile));

        this.currentId = 0;

    }

    public void start() throws IOException {
        new Thread(this::listen).start();

        for (int id = 0; id < proposals.size(); id++) {
            propose(id);
            ackTracker.addAck(id, processId);
        }

        retryTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //TODO
                retryProposals();
            }
        }, 0, RETRY_INTERVAL);
    }

    private void listen() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Message message = Message.fromBytes(packet.getData());
                handleMessage(message);
            }
        } catch (Exception e) {
            System.err.println("Error in process " + processId + " while listening: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void handleMessage(Message message) throws IOException {
        switch (message.getType()) {
            case ACK:
                handleAck(message);
                break;

            case NACK:
                handleNack(message);
                break;

            case PROPOSAL:
                handleProposition(message);
                break;
        }
        int receiveProposalId = message.getProposalId();
        decide(receiveProposalId);
        if(ackTracker.canPropose(receiveProposalId)){
            propose(receiveProposalId);
        }
    }

    private void handleAck(Message message) throws IOException {
        int receiveProposalId = message.getProposalId();
        int receivedProposalNb = message.getProposalNb();
        if ( !(proposalNb.get(receiveProposalId) == receivedProposalNb) || decided.get(receiveProposalId) ){
            return;
        }
        ackTracker.addAck(receiveProposalId, message.getSenderId());
    }

    private void handleNack(Message message) throws IOException {
        int receiveProposalId = message.getProposalId();
        int receivedProposalNb = message.getProposalNb();
        if ( !(proposalNb.get(receiveProposalId) == receivedProposalNb) || decided.get(receiveProposalId) ){
            return;
        }
        ackTracker.addNoAck(receiveProposalId, message.getSenderId());

        Set<Integer> proposedValues = message.getProposalSet();
        proposals.get(receiveProposalId).addAll(proposedValues);
    }

    private void handleProposition(Message message) throws IOException {
        int proposalId = message.getProposalId();
        int proposalNb = message.getProposalNb();
        int senderId = message.getSenderId();
        Set<Integer> receivedProposedValues = message.getProposalSet();
        Set<Integer> currentProposal = proposals.get(proposalId);
        if( receivedProposedValues.containsAll(currentProposal) ){
            currentProposal.addAll(proposedValues);
            sendAck(senderId, proposalId, proposalNb);
        } else {
            currentProposal.addAll(proposedValues);
            sendNoAck(senderId, proposalId, proposalNb, currentProposal);
        }
        proposals.set(proposalId, currentProposal);
    }

    private void decide(int id) throws IOException {
        if ( !ackTracker.canDecide(id) ){
            return;
        }
        decided.set(id, true);
        ackTracker.removeMessage(id);
        writeDecision();
    }

    private void propose(int id){
        int newProposalNb = proposalNb.get(id) + 1;
        proposalNb.set(id, newProposalNb);
        Message message = Message.createProposal(processId, id, proposals.get(id), newProposalNb);
        ackTracker.reset(processId); // reset ack and nack count without getting rid of the self ack
        send(message);
    }


    private void retryProposals() {
        // TODO, from the acktracker get the list of hosts that did not send an ack or nack and send them again the proposal

    }

    private void sendAck(int senderId, int proposalId, int proposalNb) throws IOException {
        Message message = Message.createAck(processId, proposalId, proposalNb);
        send(message, senderId);
    }

    private void sendNoAck(int senderId, int proposalId, int proposalNb, Set<Integer> proposalSet) throws IOException {
        Message message = Message.createNoAck(processId, proposalId, proposalNb, proposalSet);
        send(message, senderId);
    }

    private void send(Message message, int senderId) {
        try {
            Host host = hosts.get(senderId - 1);
            networkSimulator.send(message, host.getAddress(), host.getPort());
        } catch (Exception e) {
            System.err.println("Error sending acknowledgment for message " + message.getId() + ": " + e.getMessage());
        }
    }

    private synchronized void writeDecision() throws IOException {
        while (decided.get(currentId)){
            writer.write(proposals.get(currentId) + "\n");
            currentId++;
        }
        writer.flush();
    }

    public void shutdown() throws IOException {
        retryTimer.cancel();
        writer.close();
    }
}
