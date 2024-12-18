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
    private final List<Set<Integer>> proposals;
    private final Set<Integer> decisionSet = new HashSet<>(); // Decided values
    private final ConcurrentHashMap<String, Integer> proposalRetries = new ConcurrentHashMap<>();
    private final BufferedWriter writer;
    private final Timer retryTimer = new Timer(true);

    private static final int MAX_RETRIES = 10;
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
        this.writer = new BufferedWriter(new FileWriter(outputFile));

        retryTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                retryProposals();
            }
        }, 0, RETRY_INTERVAL);
    }

    public void start() throws IOException {
        new Thread(this::listen).start();

        for (int i = 0; i < proposals.size(); i++) {
            propose(proposals.get(i), i + 1);
        }
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
        if (message.isAck()) {
            ackTracker.addAck(message.getId(), message.getSenderId());
            if (ackTracker.isFullyAcknowledged(message.getId(), totalProcesses)) {
                decide(message);
            }
        } else if (message.isNoAck()) {
            ackTracker.addNoAck(message.getId(), message.getSenderId(), message.getProposalSet());
            retryProposal(message.getId());
        } else {
            fifoBuffer.addMessage(message);
            Host sender = hosts.get(message.getSenderId() - 1);
            Message ack = Message.createAck(processId, message.getSeqNo());
            networkSimulator.send(ack, sender.getAddress(), sender.getPort());
        }
        deliverMessages();
    }

    private void propose(Set<Integer> proposal, int seqNo) throws IOException {
        Message message = new Message(processId, seqNo, null, false, proposal, false);
        ackTracker.addPendingMessage(message);
        proposalRetries.put(message.getId(), 0);

        broadcast(message);
    }

    private void retryProposal(String messageId) throws IOException {
        Message message = ackTracker.getPendingMessage(messageId);
        if (message == null) return;

        int retries = proposalRetries.getOrDefault(messageId, 0);
        if (retries >= MAX_RETRIES) {
            System.err.println("Process " + processId + " reached max retries for proposal " + messageId);
            ackTracker.removeMessage(messageId);
            return;
        }

        Set<Integer> conflicting = ackTracker.getConflictingProposals(messageId);
        Set<Integer> updatedProposal = new HashSet<>(message.getProposalSet());
        updatedProposal.addAll(conflicting);

        Message newMessage = new Message(processId, message.getSeqNo(), null, false, updatedProposal, false);
        ackTracker.addPendingMessage(newMessage);
        proposalRetries.put(newMessage.getId(), retries + 1);

        broadcast(newMessage);
    }

    private void retryProposals() {
        try {
            for (Message message : ackTracker.getUnacknowledgedMessages(totalProcesses)) {
                retryProposal(message.getId());
            }
        } catch (IOException e) {
            System.err.println("Error during proposal retries: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void decide(Message message) throws IOException {
        decisionSet.addAll(message.getProposalSet());
        ackTracker.removeMessage(message.getId());
        writeDecision();
    }

    private void deliverMessages() throws IOException {
        List<Message> deliverable = fifoBuffer.getDeliverableMessages();
        for (Message message : deliverable) {
            writeToOutput("d " + message.getSenderId() + " " + message.getSeqNo());
        }
    }

    private void broadcast(Message message) throws IOException {
        for (Host host : hosts) {
            networkSimulator.send(message, host.getAddress(), host.getPort());
        }
        writeToOutput("b " + message.getSeqNo());
    }

    private synchronized void writeToOutput(String log) throws IOException {
        writer.write(log + "\n");
        writer.flush();
    }

    private synchronized void writeDecision() throws IOException {
        writer.write("Decision: " + decisionSet + "\n");
        writer.flush();
    }

    public void shutdown() throws IOException {
        retryTimer.cancel();
        writer.close();
    }
}
