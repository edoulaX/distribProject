package cs451;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AckTracker {
    private final ConcurrentHashMap<String, Set<Integer>> ackTable = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Integer>> noAckTable = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Message> pendingMessages = new ConcurrentHashMap<>();

    // Track acks from processes
    public void addAck(String messageId, int processId) {
        ackTable.computeIfAbsent(messageId, k -> Collections.synchronizedSet(new HashSet<>())).add(processId);
    }

    // Track noacks with conflicting proposal sets
    public void addNoAck(String messageId, int processId, Set<Integer> proposalSet) {
        noAckTable.computeIfAbsent(messageId, k -> Collections.synchronizedSet(new HashSet<>())).addAll(proposalSet);
    }

    // Check if a message is fully acknowledged
    public boolean isFullyAcknowledged(String messageId, int totalProcesses) {
        return ackTable.getOrDefault(messageId, Collections.emptySet()).size() >= totalProcesses / 2 + 1;
    }

    // Check for noacks
    public Set<Integer> getConflictingProposals(String messageId) {
        return noAckTable.getOrDefault(messageId, Collections.emptySet());
    }

    // Remove a message once it has been decided or resolved
    public void removeMessage(String messageId) {
        ackTable.remove(messageId);
        noAckTable.remove(messageId);
        pendingMessages.remove(messageId);
    }

    // Fetch all unacknowledged messages for retransmission
    public Collection<Message> getUnacknowledgedMessages(int totalProcesses) {
        List<Message> unacknowledged = new ArrayList<>();
        for (Map.Entry<String, Message> entry : pendingMessages.entrySet()) {
            String messageId = entry.getKey();
            if (!isFullyAcknowledged(messageId, totalProcesses)) {
                unacknowledged.add(entry.getValue());
            }
        }
        return unacknowledged;
    }

    // Add a message to the pending list
    public void addPendingMessage(Message message) {
        pendingMessages.put(message.getId(), message);
    }

    // Fetch a pending message by its ID
    public Message getPendingMessage(String messageId) {
        return pendingMessages.get(messageId);
    }

    @Override
    public String toString() {
        return "AckTracker{" +
                "ackTable=" + ackTable +
                ", noAckTable=" + noAckTable +
                ", pendingMessages=" + pendingMessages +
                '}';
    }
}