package cpen221.mp3.client;

//RequestType, RequestCommand, requestData, timestamp != null
public class Request {
    private final double timeStamp;
    private final RequestType requestType;
    private final RequestCommand requestCommand;

    private final String requestData;
    private int clientId;

    private String email;

    private long receptionTime;

    /**
     * Initializes a new request
     *
     * @param requestType    the RequestType of the request
     * @param requestCommand the corresponding function to be run by the server
     * @param requestData    only the data required for the specific function to be run, in
     *                       the same order as the parameters
     *
     */
    public Request(RequestType requestType, RequestCommand requestCommand, String requestData) {
        this.timeStamp = System.currentTimeMillis();
        this.requestType = requestType;
        this.requestCommand = requestCommand;
        this.requestData = requestData;
    }

    /**
     * Initializes a new request
     * @param timestamp the timestamp of the request
     * @param requestType the RequestType of the request
     * @param requestCommand the corresponding function to be run by the server
     * @param requestData the parameters of the function to be run, in that specific order
     */
    public Request(double timestamp, RequestType requestType, RequestCommand requestCommand, String requestData) {
        this.timeStamp = timestamp;
        this.requestType = requestType;
        this.requestCommand = requestCommand;
        this.requestData = requestData;
    }

    /**
     * Initializes a new request
     * @param timestamp the timestamp of the request
     * @param requestType the RequestType of the request
     * @param requestCommand the corresponding function to be run by the server
     * @param requestData the parameters of the function to be run, in that specific order
     * @param clientId the clientID of the client associated with this request
     * @param email the email of the client
     */
    public Request(double timestamp, RequestType requestType, RequestCommand requestCommand, String requestData, int clientId, String email) {
        this.timeStamp = timestamp;
        this.requestType = requestType;
        this.requestCommand = requestCommand;
        this.requestData = requestData;
        this.clientId = clientId;
        this.email = email;
    }



    /**
     * Gets the timestamp of the request
     * @return the time the request was created
     */
    public double getTimeStamp() {
        return timeStamp;
    }

    /**
     * Gest the request type of the request
     * @return the request type
     */
    public RequestType getRequestType() {
        return requestType;
    }

    /**
     * gets the request command of the request
     * @return the request command
     */
    public RequestCommand getRequestCommand() {
        return requestCommand;
    }

    /**
     * gets the request data of the request
     * @return the requestData
     */
    public String getRequestData() {
        return requestData;
    }

    /**
     * Gets the reception time of the request
     * @return the reception time
     */
    public long getReceptionTime() {
        return receptionTime;
    }

    /**
     * Sets the reception time of the request
     * @param timestamp the time the reception time is set to, must be >= 0
     */
    public void setReceptionTime(long timestamp) {
        this.receptionTime =  timestamp;
    }

    /**
     * Sets the email of the request
     * @param email the email it should be set to
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Sets the clientID of the request
     * @param clientId the clientID it should be set to
     */
    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    /**
     * Writes the request in the following string format:
     * "Request{
     *     timestamp=<value of getTimeStamp()>,
     *     RequestType=<value of getRequestType().toString()>,
     *     RequestCommand=<value of getRequestCommand().toString()>,
     *     requestData=<value of getRequestData()>
     * }
     * @return the request in string format as specified above
     */
    @Override
    public String toString() {
        return "Request{" +
                "TimeStamp=" + this.timeStamp +
                ",RequestType=" + this.requestType.toString() +
                ",RequestCommand=" + this.requestCommand.toString() +
                ",requestData={" + this.requestData + "}" +
                ",clientId=" + this.clientId +
                ",email=" + this.email +
                '}';
    }

    /**
     * Evaluates whether this object is equal to another object
     * @param obj the object to be compared against
     * @return true if the object is of the same class and they have the same String format, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() == this.getClass()) {
            Request newRequest = (Request) obj;
            return this.toString().equals(newRequest.toString());
        }
        return false;
    }
}