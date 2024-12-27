package cs451;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AckTracker {
    private final Map<Integer, Set<Integer>> ackMap; // ProposalId -> Set of processIds that sent ACK
    private final Map<Integer, Set<Integer>> nackMap; // ProposalId -> Set of processIds that sent NACK
    private final Map<Integer, Set<Integer>> pendingHostsMap; // ProposalId -> Set of pending processIds
    private final int totalProcesses;

    public AckTracker(int totalProcesses) {
        this.totalProcesses = totalProcesses;
        this.ackMap = new ConcurrentHashMap<>();
        this.nackMap = new ConcurrentHashMap<>();
        this.pendingHostsMap = new ConcurrentHashMap<>();
    }

    /**
     * Initializes tracking for a new proposal.
     */
    public void reset(int proposalId) {
        ackMap.put(proposalId, ConcurrentHashMap.newKeySet());
        nackMap.put(proposalId, ConcurrentHashMap.newKeySet());
        pendingHostsMap.put(proposalId, initPendingHosts());
    }

    private Set<Integer> initPendingHosts() {
        Set<Integer> pendingHosts = ConcurrentHashMap.newKeySet();
        for (int i = 1; i <= totalProcesses; i++) {
            pendingHosts.add(i);
        }
        return pendingHosts;
    }

    /**
     * Adds an ACK for a proposal from a specific process.
     */
    public void addAck(int proposalId, int senderId) {
        ackMap.computeIfAbsent(proposalId, k -> ConcurrentHashMap.newKeySet()).add(senderId);
        pendingHostsMap.computeIfAbsent(proposalId, k -> initPendingHosts()).remove(senderId);
    }

    /**
     * Adds a NACK for a proposal from a specific process.
     */
    public void addNoAck(int proposalId, int senderId) {
        nackMap.computeIfAbsent(proposalId, k -> ConcurrentHashMap.newKeySet()).add(senderId);
        pendingHostsMap.computeIfAbsent(proposalId, k -> initPendingHosts()).remove(senderId);
    }

    /**
     * Checks if the proposal can be decided based on ACKs and NACKs.
     * This happens when we have `f+1` ACKs, where `f` is the maximum tolerated failures.
     */
    public boolean canDecide(int proposalId) {
        int quorumSize = (totalProcesses / 2) + 1; // Quorum size (f+1)
        return ackMap.getOrDefault(proposalId, Collections.emptySet()).size() >= quorumSize;
    }

    /**
     * Determines whether the proposal can be retried based on the number of ACKs and NACKs received.
     * A proposal can be retried if:
     * - Number of NACKs + ACKs >= f+1, where f = totalProcesses / 2
     * - The proposal is still active and undecided.
     */
    public boolean canPropose(int proposalId) {
        int f = totalProcesses / 2; // Maximum tolerated failures
        int ackCount = ackMap.getOrDefault(proposalId, Collections.emptySet()).size();
        int nackCount = nackMap.getOrDefault(proposalId, Collections.emptySet()).size();

        return (ackCount + nackCount >= f + 1) && pendingHostsMap.containsKey(proposalId);
    }

    /**
     * Returns the set of hosts that have not yet responded (still pending).
     */
    public Set<Integer> getPendingHostIds(int proposalId) {
        return new HashSet<>(pendingHostsMap.getOrDefault(proposalId, Collections.emptySet()));
    }

    /**
     * Removes tracking data for a decided proposal.
     */
    public void removeMessage(int proposalId) {
        ackMap.remove(proposalId);
        nackMap.remove(proposalId);
        pendingHostsMap.remove(proposalId);
    }
}