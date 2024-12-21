package cs451;

import java.util.*;

public class FIFOBuffer {
    private final Map<Integer, TreeMap<Integer, Message>> buffer; // Sender ID -> SeqNo -> Message
    private final Map<Integer, Integer> nextExpectedSeqNo;        // Sender ID -> Next expected sequence number

    // Constructor
    public FIFOBuffer(int totalProcesses) {
        this.buffer = new HashMap<>();
        this.nextExpectedSeqNo = new HashMap<>();

        for (int i = 1; i <= totalProcesses; i++) {
            this.buffer.put(i, new TreeMap<>());
            this.nextExpectedSeqNo.put(i, 1); // Start with sequence number 1
        }
    }

    // Add a message to the buffer, ensuring FIFO order
    public void addMessage(Message message) {
        int originalSenderId = message.getOriginalSenderId();
        int seqNo = message.getSeqNo();

        int nextSeq = nextExpectedSeqNo.getOrDefault(originalSenderId, 1);
        if (seqNo < nextSeq) {
            // Ignore out-of-order or duplicate messages
            return;
        }

        // Add the message to the buffer
        buffer.get(originalSenderId).put(seqNo, message);
    }

    // Retrieve deliverable messages based on majority acknowledgment and lattice agreement
    public List<Message> getDeliverableMessages(AckTracker ackTracker, int processId) {
        List<Message> deliverable = new ArrayList<>();

        for (Map.Entry<Integer, TreeMap<Integer, Message>> entry : buffer.entrySet()) {
            int senderId = entry.getKey();
            TreeMap<Integer, Message> messages = entry.getValue();
            int nextSeq = nextExpectedSeqNo.get(senderId);

            while (!messages.isEmpty() && messages.firstKey() == nextSeq) {
                Message message = messages.firstEntry().getValue();

                // Check for majority acknowledgment
                String messageId = message.getId();
                if (message.getOriginalSenderId() == processId && !ackTracker.hasMajorityAck(messageId)) {
                    break; // Skip if majority acknowledgment is not achieved
                }

                // Check lattice agreement consistency
                if (!isLatticeStateConsistent(message, ackTracker)) {
                    break; // Skip if lattice state is not consistent
                }

                // Deliver the message
                deliverable.add(messages.pollFirstEntry().getValue());
                nextExpectedSeqNo.put(senderId, nextSeq + 1); // Update sequence number
                nextSeq++;
            }
        }

        return deliverable;
    }

    // Check if the lattice state is consistent with current agreements
    private boolean isLatticeStateConsistent(Message message, AckTracker ackTracker) {
        // Retrieve the lattice state associated with this message
        Set<Integer> latticeState = message.getLatticeState();

        // Verify if the lattice state is stable (implementation-dependent)
        return ackTracker.isLatticeStateStable(message.getId(), latticeState);
    }

    // Check if a message is duplicate or already delivered
    public boolean isDuplicate(Message message) {
        int senderId = message.getOriginalSenderId();
        int seqNo = message.getSeqNo();

        int nextSeq = nextExpectedSeqNo.getOrDefault(senderId, 1);
        return seqNo < nextSeq; // Duplicate if older than next expected
    }
}
