package cs451;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Process {
    private final int processId;
    private final int totalProcesses;
    private final Host myHost;
    private final List<Host> hosts;
    private final AckTracker ackTracker;
    private final FIFOBuffer fifoBuffer;
    private final NetworkSimulator networkSimulator;
    private final DatagramSocket socket;
    private final int messageCount;
    private final ConcurrentHashMap<String, AtomicInteger> retransmissionCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> nextRetransmissionTime = new HashMap<>();
    private static final int MAX_RETRANSMISSIONS = 20;
    private static final long BASE_RETRANSMISSION_INTERVAL = 1000; // 1 second in milliseconds
    private final BufferedWriter writer;
    private final Timer retransmissionTimer = new Timer(true); // Single-threaded timer for retransmissions

    public Process(int processId, int totalProcesses, Host myHost, List<Host> hosts, String outputFile, int messageCount)
            throws IOException {
        this.processId = processId;
        this.totalProcesses = totalProcesses;
        this.myHost = myHost;
        this.hosts = hosts;
        this.ackTracker = new AckTracker();
        this.fifoBuffer = new FIFOBuffer(totalProcesses);
        this.networkSimulator = new NetworkSimulator();
        this.socket = new DatagramSocket(myHost.getPort());
        this.messageCount = messageCount;
        this.writer = new BufferedWriter(new FileWriter(outputFile));

        // Start retransmission checking
        retransmissionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                retransmitUnacknowledged();
            }
        }, 0, BASE_RETRANSMISSION_INTERVAL); // Check every second
    }

    public void start() throws IOException {
        //System.out.println("Process " + processId + " started on " + myHost.getAddress() + ":" + myHost.getPort());
        new Thread(this::listen).start();

        //System.out.println("Process " + processId + " is broadcasting " + messageCount + " messages.");
        for (int i = 1; i <= messageCount; i++) {
            broadcast(i);
        }
        //System.out.println("Process " + processId + " finished broadcasting.");
    }

    private void listen() {
        try {
            //System.out.println("Process " + processId + " is now listening for incoming messages.");
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
                ackTracker.removeMessage(message.getId());
                retransmissionCounts.remove(message.getId());
                nextRetransmissionTime.remove(message.getId());
                //System.out.println("Process " + processId + " received full acknowledgment for " + message.getId());
            }
        } else {
            if (fifoBuffer.isDuplicate(message)) {
                //System.out.println("Process " + processId + " ignored duplicate message " + message.getId());
                return;
            }

            fifoBuffer.addMessage(message);
            Message ack = Message.createAck(processId, message.getSeqNo());
            Host sender = hosts.get(message.getSenderId() - 1);

            try {
                networkSimulator.send(ack, sender.getAddress(), sender.getPort());
                //System.out.println("Process " + processId + " sent acknowledgment for message " + message.getId());
            } catch (Exception e) {
                System.err.println("Error sending acknowledgment for message " + message.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        deliverMessages();
    }

    private void broadcast(int seqNo) throws IOException {
        Message message = new Message(processId, seqNo, "m" + seqNo, false);
        ackTracker.addAck(message.getId(), processId);
        ackTracker.addPendingMessage(message); // Add to pending messages
        retransmissionCounts.putIfAbsent(message.getId(), new AtomicInteger(0));
        nextRetransmissionTime.put(message.getId(), System.currentTimeMillis() + BASE_RETRANSMISSION_INTERVAL);

        writeToOutput("b " + seqNo);

        for (Host host : hosts) {
            try {
                networkSimulator.send(message, host.getAddress(), host.getPort());
                //System.out.println("Process " + processId + " sent message " + message.getId() + " to " + host.getAddress() + ":" + host.getPort());
            } catch (Exception e) {
                System.err.println("Error sending message " + message.getId() + " to " + host.getAddress() + ":" + host.getPort() + ": " + e.getMessage());
            }
        }
    }

    private void retransmitUnacknowledged() {
        long now = System.currentTimeMillis();
        List<Message> toRetransmit = new ArrayList<>();

        // Identify messages to retransmit
        for (Map.Entry<String, Long> entry : nextRetransmissionTime.entrySet()) {
            String messageId = entry.getKey();
            long nextTime = entry.getValue();

            if (now >= nextTime) {
                int retries = retransmissionCounts.get(messageId).incrementAndGet();
                if (retries > MAX_RETRANSMISSIONS) {
                    System.err.println("Process " + processId + " reached max retransmissions for message " + messageId);
                    nextRetransmissionTime.remove(messageId);
                    retransmissionCounts.remove(messageId);
                    continue;
                }

                Message message = ackTracker.getPendingMessage(messageId); // Fetch the pending message
                if (message != null) {
                    toRetransmit.add(message);
                    nextRetransmissionTime.put(messageId, now + (BASE_RETRANSMISSION_INTERVAL * (1L << (retries - 1))));
                }
            }
        }

        // Retransmit the identified messages
        for (Message message : toRetransmit) {
            for (Host host : hosts) {
                try {
                    networkSimulator.send(message, host.getAddress(), host.getPort());
                    //System.out.println("Process " + processId + " retransmitted message " + message.getId() + " to " + host.getAddress() + ":" + host.getPort());
                } catch (Exception e) {
                    System.err.println("Error retransmitting message " + message.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    private void deliverMessages() throws IOException {
        List<Message> deliverable = fifoBuffer.getDeliverableMessages();
        for (Message message : deliverable) {
            writeToOutput("d " + message.getSenderId() + " " + message.getSeqNo());
            //System.out.println("Process " + processId + " delivered message " + message.getId());
        }
    }

    private synchronized void writeToOutput(String log) throws IOException {
        writer.write(log + "\n");
        writer.flush();
    }

    public void shutdown() throws IOException {
        retransmissionTimer.cancel();
        writer.close();
    }
}
