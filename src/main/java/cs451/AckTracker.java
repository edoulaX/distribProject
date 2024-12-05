package cs451;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AckTracker {
    private final ConcurrentHashMap<String, Set<Integer>> ackTable = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Message> pendingMessages = new ConcurrentHashMap<>();

    public void addAck(String messageId, int processId) {
        ackTable.computeIfAbsent(messageId, k -> Collections.synchronizedSet(new HashSet<>())).add(processId);
    }

    public boolean isFullyAcknowledged(String messageId, int totalProcesses) {
        return ackTable.getOrDefault(messageId, Collections.emptySet()).size() == totalProcesses;
    }

    public void removeMessage(String messageId) {
        ackTable.remove(messageId);
        pendingMessages.remove(messageId);
    }

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

    public void addPendingMessage(Message message) {
        pendingMessages.put(message.getId(), message);
    }

    public Message getPendingMessage(String messageId) {
        return pendingMessages.get(messageId); // Fetch pending message by ID
    }
}
