package cs451;

import java.util.*;

public class FIFOBuffer {
    private final Map<Integer, TreeMap<Integer, Message>> buffer; // Sender ID -> SeqNo -> Message
    private final Map<Integer, Integer> nextExpectedSeqNo;

    public FIFOBuffer(int totalProcesses) {
        this.buffer = new HashMap<>();
        this.nextExpectedSeqNo = new HashMap<>();
        for (int i = 1; i <= totalProcesses; i++) { // Initialize sender IDs (1-based indexing)
            this.buffer.put(i, new TreeMap<>());
            this.nextExpectedSeqNo.put(i, 1); // Default next expected sequence number is 1
        }
    }

    public void addMessage(Message message) {
        int senderId = message.getSenderId();
        int nextSeq = nextExpectedSeqNo.getOrDefault(senderId, 1);
        if (message.getSeqNo() < nextSeq) {
            //System.out.println("Ignoring duplicate message: " + message.getId());
            return;
        }
        buffer.get(senderId).put(message.getSeqNo(), message);
    }

    public List<Message> getDeliverableMessages() {
        List<Message> deliverable = new ArrayList<>();
        for (Map.Entry<Integer, TreeMap<Integer, Message>> entry : buffer.entrySet()) {
            int senderId = entry.getKey();
            TreeMap<Integer, Message> messages = entry.getValue();

            while (!messages.isEmpty() && messages.firstKey() == nextExpectedSeqNo.get(senderId)) {
                deliverable.add(messages.pollFirstEntry().getValue());
                nextExpectedSeqNo.put(senderId, nextExpectedSeqNo.get(senderId) + 1);
            }
        }
        return deliverable;
    }

    public boolean isDuplicate(Message message) {
        int senderId = message.getSenderId();
        int nextSeq = nextExpectedSeqNo.getOrDefault(senderId, 1); // Ensure default is returned if senderId is missing
        return message.getSeqNo() < nextSeq;
    }
}
