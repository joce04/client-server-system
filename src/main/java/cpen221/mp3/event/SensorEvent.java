package cpen221.mp3.event;

import java.text.DecimalFormat;

//REP INVARIANTS: Timestamp, ClientId, EntityId, EntityType, Value != null
public class SensorEvent implements Event {
    private double TimeStamp;
    private int ClientId;
    private int EntityId;
    private String EntityType;
    private double Value;
    private long timeArrived;

    /**
     * Constructs a new SensorEvent with the given parameters.
     * @param TimeStamp the timestamp of the event
     * @param ClientId the id of the client associated with the event
     * @param EntityId the entity id
     * @param EntityType the entity type, a String
     * @param Value the value sent
     */

    public SensorEvent(double TimeStamp,
                       int ClientId,
                       int EntityId,
                       String EntityType,
                       double Value) {
        this.TimeStamp = TimeStamp;
        this.ClientId = ClientId;
        this.EntityId = EntityId;
        this.EntityType = EntityType;
        this.Value = Value;
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
     *  Returns the ID of the entity that generated this event.
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
    public double getValueDouble() {
        return Value;
    }

    // Sensor events do not have a boolean value
    // no need to implement this method
    public boolean getValueBoolean() {
        return false;
    }


    /**
     * Returns the time that this event arrived at the server.
     */
    public long getTimeArrived() { return this.timeArrived; }


    /**
     * Sets the time that this event arrived at the server.
     */
    public void setTimeArrived(long arrivalTime) {this.timeArrived = arrivalTime; }

    /**
     * Converts the event into a string of format
     * SensorEvent{
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
        return "SensorEvent{" +
                "TimeStamp=" + decimalFormat.format(getTimeStamp()) +
                ",ClientId=" + getClientId() +
                ",EntityId=" + getEntityId() +
                ",EntityType=" + getEntityType() +
                ",Value=" + getValueDouble() +
                '}';
    }


}
