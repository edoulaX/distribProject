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
    private final Timer retransmissionTimer = new Timer(true);
    private long retransmissionInterval = 500; // Initial interval (1 second)
    private static final int MIN_RETRANSMISSION_INTERVAL = 100;
    private static final int MAX_RETRANSMISSION_INTERVAL = 1000;

    private final Map<Integer, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Set<Integer> suspectedCrashes = ConcurrentHashMap.newKeySet();
    private static final int CRASH_THRESHOLD = 5; // Max failed attempts before marking as crashed

    private Set<Integer> currentLatticeState = ConcurrentHashMap.newKeySet();
    private final List<Set<Integer>> proposals;

    public Process(int processId, int totalProcesses, Host myHost, List<Host> hosts, String outputFile, ConfigData configData)
            throws IOException {
        this.processId = processId;
        this.totalProcesses = totalProcesses;
        this.myHost = myHost;
        this.hosts = hosts;
        this.ackTracker = new AckTracker(totalProcesses, processId, configData.getMaxDistinctElements());
        this.fifoBuffer = new FIFOBuffer(totalProcesses);
        this.networkSimulator = new NetworkSimulator();
        this.socket = new DatagramSocket(myHost.getPort());
        this.writer = new BufferedWriter(new FileWriter(outputFile));

        // Initialize lattice state and proposals
        this.proposals = configData.getProposals();
        for (Set<Integer> proposal : proposals) {
            currentLatticeState.addAll(proposal);
        }

        // Schedule crash retry task
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                retrySuspectedCrashes();
            }
        }, 0, 5000); // Retry every 5 seconds
    }

    public void start() throws IOException {
        // Start listening for incoming messages
        new Thread(this::listen).start();

        // Broadcast initial proposals
        int seqNo = 1;
        for (Set<Integer> proposal : proposals) {
            Message message = Message.createLatticeMessage(
                    processId, seqNo++, processId, proposal.toString(), proposal
            );
            fifoBuffer.addMessage(message);
            ackTracker.trackMessage(message);
            broadcast(message);
        }

        // Schedule retransmission task
        retransmissionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                retransmitUninformed();
            }
        }, 0, retransmissionInterval);
    }

    private void listen() {
        try {
            while (true) {
                byte[] buffer = new byte[2048];
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
        } else {
            if (!fifoBuffer.isDuplicate(message)) {
                fifoBuffer.addMessage(message);
                ackTracker.trackMessage(message);
                sendAcknowledgment(message);
            } else {
                // Handle duplicate message by updating acknowledgment and lattice state
                ackTracker.trackMessage(message);
                sendAcknowledgment(message);
            }

            // Merge lattice state
            currentLatticeState.addAll(message.getLatticeState());
        }

        deliverMessages();
    }

    private void deliverMessages() throws IOException {
        List<Message> deliverable = fifoBuffer.getDeliverableMessages(ackTracker, processId);

        if (!deliverable.isEmpty()) {
            int size = deliverable.size();
            StringBuilder batchOutput = new StringBuilder();
            for (int i = 0; i < size; i++) {
                Message message = deliverable.get(i);
                message.getLatticeState().forEach(x -> batchOutput.append(x).append(" "));
                //batchOutput.append("d ").append(message.getOriginalSenderId()).append(" ").append(message.getLatticeState());
                if (i < size - 1) {
                    batchOutput.append("\n"); // Add newline only between messages
                }
            }
            writeToOutput(batchOutput.toString());
        }
    }

    private void retransmitUninformed() {
        try {
            List<Message> toBroadcast = ackTracker.getMessagesForUninformed(processId);

            // Adjust retransmission interval dynamically
            int pendingMessages = toBroadcast.size();
            if (pendingMessages > totalProcesses * 2) { // High traffic
                retransmissionInterval = Math.max(MIN_RETRANSMISSION_INTERVAL, retransmissionInterval / 2);
            } else if (pendingMessages == 0) { // No pending messages
                retransmissionInterval = Math.min(MAX_RETRANSMISSION_INTERVAL, retransmissionInterval + 100);
            }

            // Send messages to uninformed processes
            for (Message message : toBroadcast) {
                sendToUninformed(message);
            }

        } catch (Exception e) {
            System.err.println("Error during retransmission in process " + processId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Reschedule retransmission with updated interval
            retransmissionTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    retransmitUninformed();
                }
            }, retransmissionInterval);
        }
    }

    private void broadcast(Message message) throws IOException {
        //writeToOutput("b " + message.getSeqNo());
        sendToUninformed(message);
    }

    private void sendAcknowledgment(Message message) {
        try {
            Message ack = Message.createAck(processId, message.getSeqNo(), message.getOriginalSenderId(), new HashSet<>(Collections.singleton(processId)));
            Host host = hosts.get(message.getSenderId() - 1);
            networkSimulator.send(ack, host.getAddress(), host.getPort());
        } catch (Exception e) {
            System.err.println("Error sending acknowledgment for message " + message.getId() + ": " + e.getMessage());
        }
    }

    private void sendToUninformed(Message message) {
        Set<Integer> informedProcesses = message.getAckList();

        for (int i = 1; i <= totalProcesses; i++) {
            if (suspectedCrashes.contains(i) || informedProcesses.contains(i)) {
                continue; // Skip crashed or already informed processes
            }

            Host host = hosts.get(i - 1);
            try {
                if (!ackTracker.isFullyAcknowledged(message.getId())) {
                    networkSimulator.send(message, host.getAddress(), host.getPort());
                    failedAttempts.put(i, 0); // Reset failed attempts on success
                }
            } catch (Exception e) {
                // Increment failure count
                failedAttempts.put(i, failedAttempts.getOrDefault(i, 0) + 1);

                // Mark process as crashed if threshold exceeded
                if (failedAttempts.get(i) >= CRASH_THRESHOLD) {
                    suspectedCrashes.add(i);
                    System.err.println("Process " + i + " suspected as crashed.");
                }
            }
        }
    }

    private void retrySuspectedCrashes() {
        Iterator<Integer> iterator = suspectedCrashes.iterator();
        while (iterator.hasNext()) {
            int processId = iterator.next();
            if (failedAttempts.getOrDefault(processId, 0) < CRASH_THRESHOLD) {
                iterator.remove(); // Remove from suspected crashes
                System.out.println("Process " + processId + " removed from suspected crashes.");
            }
        }
    }

    private synchronized void writeToOutput(String log) throws IOException {
        writer.write(log + "\n");
        writer.flush();
    }

    public void shutdown() throws IOException {
        retransmissionTimer.cancel();
        writer.close();
        socket.close();
    }
}
