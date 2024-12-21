package cs451;

import java.util.HashSet;
import java.util.Set;

public class LatticeState {
    private final Set<Integer> currentState;

    public LatticeState() {
        this.currentState = new HashSet<>();
    }

    public synchronized void join(Set<Integer> proposal) {
        currentState.addAll(proposal); // Perform join operation (set union)
    }

    public synchronized Set<Integer> getState() {
        return new HashSet<>(currentState); // Return a copy of the current state
    }
}
