package cs451.perfectLink;

public class Timeout {
    private static final float MULT_INCREASE = 2f;     // Multiplier for increasing the timeout
    private static final float MULT_DECREASE = 0.75f;  // Multiplier for decreasing the timeout
    private static final int MAX_INCREASE = 10;        // Max multiplier limit for timeout increase
    private static final int MIN = 50;                 // Minimum timeout in milliseconds
    private static final int MAX = MIN * (int) Math.pow(2, MAX_INCREASE);  // Maximum allowable timeout

    private int timeout = MIN;  // Initial timeout value

    // Increases the timeout by a multiplier, up to the maximum limit
    public void increase() {
        timeout = (int) Math.min(timeout * MULT_INCREASE, MAX);
        System.out.println("Increasing timeout " + timeout);
    }

    // Decreases the timeout by a multiplier, down to the minimum limit
    public void decrease() {
        timeout = (int) Math.max(timeout * MULT_DECREASE, MIN);
        System.out.println("Decreasing timeout " + timeout);
    }

    // Returns the current timeout value
    public int get() { return timeout; }
}
