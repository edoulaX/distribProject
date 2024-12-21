package cs451;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AckTracker {
    private final ConcurrentHashMap<String, Set<Integer>> ackTable; // Maps messageId to set of acknowledging processes
    private final ConcurrentHashMap<String, Message> pendingMessages; // Maps messageId to the actual Message
    private final ConcurrentHashMap<String, Set<Integer>> latticeStates; // Maps messageId to the lattice states
    private final int totalProcesses;
    private final int currentProcessId;
    private final int maxDistinctElements;

    // Constructor
    public AckTracker(int totalProcesses, int currentProcessId, int maxDistinctElements) {
        this.ackTable = new ConcurrentHashMap<>();
        this.pendingMessages = new ConcurrentHashMap<>();
        this.latticeStates = new ConcurrentHashMap<>();
        this.totalProcesses = totalProcesses;
        this.currentProcessId = currentProcessId;
        this.maxDistinctElements = maxDistinctElements;
    }

    // Add an acknowledgment for a message from a process
    public void addAck(String messageId, int processId) {
        ackTable.computeIfAbsent(messageId, key -> ConcurrentHashMap.newKeySet()).add(processId);
    }

    // Track a pending message and merge acknowledgment lists if already exists
    public synchronized void trackMessage(Message message) {
        String messageId = message.getId();

        // Ensure pendingMessages is updated atomically
        pendingMessages.compute(messageId, (key, existingMessage) -> {
            if (existingMessage == null) {
                // Add new message and acknowledgment list
                Set<Integer> ackList = ackTable.computeIfAbsent(messageId, k -> ConcurrentHashMap.newKeySet());
                ackList.add(currentProcessId); // Acknowledge by current process
                ackList.addAll(message.getAckList());

                // Add lattice state
                latticeStates.put(messageId, new HashSet<>(message.getLatticeState()));

                return new Message(
                        message.getSenderId(),
                        message.getSeqNo(),
                        message.getOriginalSenderId(),
                        message.getContent(),
                        message.isAck(),
                        ackList,
                        message.getLatticeState()
                );
            } else {
                // Merge acknowledgment lists
                ackTable.computeIfAbsent(messageId, k -> ConcurrentHashMap.newKeySet()).addAll(message.getAckList());

                // Merge lattice states
                latticeStates.computeIfAbsent(messageId, k -> new HashSet<>()).addAll(message.getLatticeState());
                if (latticeStates.get(messageId).size() > maxDistinctElements) {
                    throw new IllegalStateException("Lattice state exceeds maximum distinct elements. Current size: "
                            + latticeStates.get(messageId).size() + ", Max allowed: " + maxDistinctElements);
                }

                return existingMessage; // Keep existing message
            }
        });
    }

    // Get the count of acknowledgments for a specific message
    public int getAckCount(String messageId) {
        Set<Integer> ackList = ackTable.get(messageId);
        return ackList == null ? 0 : ackList.size();
    }

    // Check if a message has been acknowledged by a majority of processes
    public boolean hasMajorityAck(String messageId) {
        return getAckCount(messageId) >= (totalProcesses / 2) + 1; // Majority threshold
    }

    // Check if a message is fully acknowledged by all processes
    public boolean isFullyAcknowledged(String messageId) {
        return getAckCount(messageId) == totalProcesses;
    }

    // Retrieve messages that need to be rebroadcasted to uninformed processes
    public List<Message> getMessagesForUninformed(int currentProcessId) {
        List<Message> toBroadcast = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Message> entry : pendingMessages.entrySet()) {
            String messageId = entry.getKey();
            Message message = entry.getValue();
            Set<Integer> ackList = ackTable.getOrDefault(messageId, ConcurrentHashMap.newKeySet());

            // Remove fully acknowledged messages
            if (isFullyAcknowledged(messageId)) {
                toRemove.add(messageId);
                continue;
            }

            // Identify uninformed processes
            for (int processId = 1; processId <= totalProcesses; processId++) {
                if (processId == currentProcessId || ackList.contains(processId)) {
                    continue;
                }

                // Create a new message for uninformed process
                toBroadcast.add(new Message(
                        currentProcessId,
                        message.getSeqNo(),
                        message.getOriginalSenderId(),
                        message.getContent(),
                        message.isAck(),
                        ackList,
                        latticeStates.getOrDefault(messageId, new HashSet<>())
                ));
            }
        }

        // Remove fully acknowledged messages
        for (String messageId : toRemove) {
            removeMessage(messageId);
        }

        return toBroadcast;
    }

    // Remove tracking for a fully acknowledged message
    public void removeMessage(String messageId) {
        ackTable.remove(messageId);
        pendingMessages.remove(messageId);
        latticeStates.remove(messageId);
    }

    // Check if a lattice state is stable for a given message ID
    public boolean isLatticeStateStable(String messageId, Set<Integer> stateToCheck) {
        Set<Integer> currentLatticeState = latticeStates.getOrDefault(messageId, new HashSet<>());
        return currentLatticeState.containsAll(stateToCheck); // Ensure the state to check is included in the current state
    }

    public void cleanupAcknowledgedMessages() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Set<Integer>> entry : ackTable.entrySet()) {
            if (isFullyAcknowledged(entry.getKey())) {
                toRemove.add(entry.getKey());
            }
        }
        for (String messageId : toRemove) {
            removeMessage(messageId);
        }
    }
}
