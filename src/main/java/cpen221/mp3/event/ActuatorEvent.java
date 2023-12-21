package cpen221.mp3.event;

import java.text.DecimalFormat;

//REP INVARIANTS: Timestamp, ClientId, EntityId, EntityType, Value != null
public class ActuatorEvent implements Event {

    private double TimeStamp;
    private int ClientId;
    private int EntityId;
    private String EntityType;
    private boolean Value;

    private long timeArrived;

    /**
     * Constructs a new ActuatorEvent with the given parameters.
     * @param TimeStamp The time stamp of this event.
     * @param ClientId The ID of the client that generated this event.
     * @param EntityId The ID of the entity that generated this event.
     * @param EntityType The type of the entity that generated this event.
     * @param Value The value of this event.
     */
    public ActuatorEvent(double TimeStamp,
                         int ClientId,
                         int EntityId,
                         String EntityType,
                         boolean Value) {
        this.TimeStamp = TimeStamp;
        this.ClientId = ClientId;
        this.EntityId = EntityId;
        this.EntityType = EntityType;
        this.Value = Value;
        this.timeArrived = -1;
    }

    /**
     * Sets the time that this event arrived at the server.
     */
    public void setTimeArrived(long arrivalTime) {
        this.timeArrived = arrivalTime;
    }

    /**
     * Returns the time stamp of this event.
     */
    public double getTimeStamp() {
        return TimeStamp;
    }

    /**
     * Returns the ID of the client that generated this event.
     */
    public int getClientId() {
        return ClientId;
    }

    /**
     * Returns the ID of the entity that generated this event.
     */
    public int getEntityId() {
        return EntityId;
    }

    /**
     * Returns the type of the entity that generated this event.
     */
    public String getEntityType() {
        return EntityType;
    }


    /**
     * Returns the value of this event.
     */
    public boolean getValueBoolean() {
        return Value;
    }

    /**
     * Returns the time that this event arrived at the server.
     */
    public long getTimeArrived() { return this.timeArrived; }

    // Actuator events do not have a double value
    // no need to implement this method
    public double getValueDouble() {
        return -1;
    }

    /**
     * Converts the event into a string of format
     * ActuatorEvent{
     *     TimeStamp=timestamp,
     *     ClientID=clientId
     *     EntityId=entityId,
     *     EntityType=entityType,
     *     Value=value
     * }
     * @return the string representing this event
     */
    @Override
    public String toString() {
        DecimalFormat decimalFormat = new DecimalFormat("0.00000000");
        return "ActuatorEvent{" +
                "TimeStamp=" + decimalFormat.format(getTimeStamp()) +
                ",ClientId=" + getClientId() +
                ",EntityId=" + getEntityId() +
                ",EntityType=" + getEntityType() +
                ",Value=" + getValueBoolean() +
                '}';
    }
}
