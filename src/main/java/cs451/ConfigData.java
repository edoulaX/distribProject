package cs451;

import java.util.*;

public class ConfigData {
    private final int proposalsPerProcess;
    private final int maxElementsPerProposal;
    private final int maxDistinctElements;
    private final List<Set<Integer>> proposals;

    public ConfigData(int proposalsPerProcess, int maxElementsPerProposal, int maxDistinctElements, List<Set<Integer>> proposals) {
        this.proposalsPerProcess = proposalsPerProcess;
        this.maxElementsPerProposal = maxElementsPerProposal;
        this.maxDistinctElements = maxDistinctElements;
        this.proposals = proposals;
    }

    public int getProposalsPerProcess() {
        return proposalsPerProcess;
    }

    public int getMaxElementsPerProposal() {
        return maxElementsPerProposal;
    }

    public int getMaxDistinctElements() {
        return maxDistinctElements;
    }

    public List<Set<Integer>> getProposals() {
        return proposals;
    }
}
