package cpen221.mp3.server;

public class TimeWindow {
    // REP INVARIANT: the startTime and endTime are never null
    public final double startTime;
    public final double endTime;

    /**
     * Creates a new instance of TimeWindow
     * @param startTime the start of the time window
     * @param endTime the end of the time window, endTime >= startTime
     */
    public TimeWindow(double startTime, double endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Gets the start time of the time window
     * @return the start time
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Gets the end time of the time window
     * @return the end time
     */
    public double getEndTime() {
        return endTime;
    }

    /**
     * Converts the TimeWindow to String format
     * @return the formatted String
     */
    @Override
    public String toString() {
        return "TimeWindow{" +
               "StartTime=" + getStartTime() +
               ",EndTime=" + getEndTime() +
               '}';
    }
}
