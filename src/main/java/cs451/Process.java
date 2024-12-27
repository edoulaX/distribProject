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
    private final NetworkSimulator networkSimulator;
    private final DatagramSocket socket;
    private final BufferedWriter writer;
    private final Timer retryTimer = new Timer(true);
    private final List<Set<Integer>> proposals;
    private final List<Integer> proposalNb;
    private final List<Boolean> decided;
    private int currentId;

    private static final int MAX_RETRIES = 5;
    private static final long RETRY_INTERVAL = 1000; // Retry interval in milliseconds
    private static final int BUFFER_SIZE = 1024; // Datagram packet buffer size

    public Process(int processId, int totalProcesses, Host myHost, List<Host> hosts, String outputFile, List<Set<Integer>> proposals) throws IOException {
        this.processId = processId;
        this.totalProcesses = totalProcesses;
        this.myHost = myHost;
        this.hosts = hosts;
        this.ackTracker = new AckTracker(totalProcesses);
        this.networkSimulator = new NetworkSimulator();
        this.socket = new DatagramSocket(myHost.getPort());
        this.proposals = proposals;
        this.proposalNb = new ArrayList<>(Collections.nCopies(proposals.size(), 0));
        this.decided = new ArrayList<>(Collections.nCopies(proposals.size(), false));
        this.writer = new BufferedWriter(new FileWriter(outputFile));
        this.currentId = 0;
    }

    public void start() throws IOException {
        new Thread(this::listen).start();

        for (int id = 0; id < proposals.size(); id++) {
            propose(id);
        }

        retryTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                retryProposals();
            }
        }, 0, RETRY_INTERVAL);
    }

    private void listen() {
        while (true) {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Message message = Message.fromBytes(packet.getData());
                handleMessage(message);
            } catch (SocketException e) {
                System.err.println("Socket error in process " + processId + ": " + e.getMessage());
                break; // Exit loop for unrecoverable socket errors
            } catch (IOException e) {
                System.err.println("Error in process " + processId + " while listening: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void handleMessage(Message message) throws IOException {
        int receiveProposalId = message.getProposalId();
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
        decide(receiveProposalId);
        if (ackTracker.canPropose(receiveProposalId)) {
            propose(receiveProposalId);
        }
    }

    private void handleAck(Message message) {
        int proposalId = message.getProposalId();
        int proposalNb = message.getProposalNb();
        if (!isValidMessage(proposalId, proposalNb)) {
            return;
        }
        ackTracker.addAck(proposalId, message.getSenderId());
    }

    private void handleNack(Message message) {
        int proposalId = message.getProposalId();
        int proposalNb = message.getProposalNb();
        if (!isValidMessage(proposalId, proposalNb)) {
            return;
        }
        ackTracker.addNoAck(proposalId, message.getSenderId());
        synchronized (proposals) {
            proposals.get(proposalId).addAll(message.getProposalSet());
        }
    }

    private void handleProposition(Message message) throws IOException {
        int proposalId = message.getProposalId();
        int proposalNb = message.getProposalNb();
        Set<Integer> receivedProposedValues = message.getProposalSet();
        int senderId = message.getSenderId();

        synchronized (proposals) {
            Set<Integer> currentProposal = proposals.get(proposalId);
            if (receivedProposedValues.containsAll(currentProposal)) {
                currentProposal.addAll(receivedProposedValues);
                sendAck(senderId, proposalId, proposalNb);
            } else {
                currentProposal.addAll(receivedProposedValues);
                sendNoAck(senderId, proposalId, proposalNb, currentProposal);
            }
        }
    }

    private void decide(int id) throws IOException {
        if (!ackTracker.canDecide(id) || decided.get(id)) {
            return;
        }
        synchronized (decided) {
            decided.set(id, true);
        }
        ackTracker.removeMessage(id);
        writeDecision();
    }

    private void propose(int id) {
        int newProposalNb;
        synchronized (proposalNb) {
            newProposalNb = proposalNb.get(id) + 1;
            proposalNb.set(id, newProposalNb);
        }

        Message message;
        synchronized (proposals) {
            message = Message.createProposal(processId, id, proposals.get(id), newProposalNb);
        }

        ackTracker.reset(id);
        ackTracker.addAck(id, processId); // Self-acknowledge
        broadcast(message);
    }

    private void retryProposals() {
        for (int id = currentId; id < proposals.size(); id++) {
            Set<Integer> pendingHosts = ackTracker.getPendingHostIds(id);
            for (int hostId : pendingHosts) {
                int proposalNumber;
                synchronized (proposalNb) {
                    proposalNumber = proposalNb.get(id);
                }

                Message message;
                synchronized (proposals) {
                    message = Message.createProposal(processId, id, proposals.get(id), proposalNumber);
                }

                send(message, hostId);
            }
        }
    }

    private void sendAck(int senderId, int proposalId, int proposalNb) {
        Message message = Message.createAck(processId, proposalId, proposalNb);
        send(message, senderId);
    }

    private void sendNoAck(int senderId, int proposalId, int proposalNb, Set<Integer> proposalSet) {
        Message message = Message.createNoAck(processId, proposalId, proposalNb, proposalSet);
        send(message, senderId);
    }

    private void broadcast(Message message) {
        for (Host host : hosts) {
            if (host.getId() != processId) { // Skip self
                send(message, host.getId());
            }
        }
    }

    private void send(Message message, int senderId) {
        try {
            Host host = hosts.get(senderId - 1);
            networkSimulator.send(message, host.getAddress(), host.getPort());
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    private synchronized void writeDecision() throws IOException {
        synchronized (decided) {
            while (currentId < decided.size() && decided.get(currentId)) {
                Set<Integer> proposal = proposals.get(currentId);
                String result = String.join(" ", proposal.stream().map(String::valueOf).toArray(String[]::new));
                writer.write(result + "\n");
                currentId++;
            }
        }
        writer.flush();
    }

    private boolean isValidMessage(int proposalId, int proposalNumber) {
        synchronized (proposalNb) {
            return proposalNumber == this.proposalNb.get(proposalId) && !decided.get(proposalId);
        }
    }

    public void shutdown() throws IOException {
        retryTimer.cancel();
        writer.close();
    }
}
