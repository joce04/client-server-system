package cpen221.mp3.entity;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cpen221.mp3.client.Client;
import cpen221.mp3.client.Request;
import cpen221.mp3.client.RequestCommand;
import cpen221.mp3.client.RequestType;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.handler.MessageHandler;
import cpen221.mp3.server.Server;
import cpen221.mp3.server.SeverCommandToActuator;

//REP INVARIANTS: id, type, init_state != null
public class Actuator implements Entity {
    private final int id;
    private int clientId;
    private final String type;
    private boolean state;
    private double eventGenerationFrequency = 0.2; // default value in Hz (1/s)

    // the following specifies the http endpoint that the actuator should send events to
    private String serverIP;
    private int entityPort;
    private int serverPort;


    private BufferedReader serverIn;
    private PrintWriter clientOutput;

    private String host = null;

    // Flag to indicate whether the actuator has sent an event since the last time hasSentEvent() was called
    private boolean hasSentEvent = false;


    // Socket to send events to
    private Socket eventSocket;

    // Socket to receive server messages
    private ServerSocket serverSocket;


    /**
     * Create a new actuator with the given id, type, and initial state and unset client Id.
     * @param id the unique id of the actuator
     * @param type the type of the actuator
     * @param init_state the initial state of the actuator
     */
    public Actuator(int id, String type, boolean init_state) {
        this.id = id;
        this.clientId = -1;         // remains unregistered
        this.type = type;
        this.state = init_state;
    }

    /**
     * Create a new actuator with the given id, type, initial state, and client id.
     * @param id the unique id of the actuator
     * @param clientId the id of the client that this actuator is registered for
     * @param type the type of the actuator
     * @param init_state the initial state of the actuator
     */
    public Actuator(int id, int clientId, String type, boolean init_state) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.state = init_state;

    }

    /**
     *  Create a new actuator with the given id, type, initial state, and client id.
     * @param id the unique id of the actuator
     * @param type the type of the actuator
     * @param init_state the initial state of the actuator
     * @param serverIP the IP address of the endpoint
     * @param entityPort the port number of the endpoint
     */
    public Actuator(int id, String type, boolean init_state, String serverIP, int entityPort) {
        this.id = id;
        this.clientId = -1;         // remains unregistered
        this.type = type;
        this.state = init_state;
        this.serverIP = serverIP;
        this.entityPort = entityPort;

        createServerSocket(0);

        createEntitySocket(serverIP, entityPort);
    }

    /**
     * Create a new actuator with the given id, type, initial state, and client id.
     * @param id the unique id of the actuator
     * @param clientId the id of the client that this actuator is registered for
     * @param type the type of the actuator
     * @param init_state the initial state of the actuator
     * @param serverIP the IP address of the endpoint
     * @param entityPort the port number of the endpoint
     */
    public Actuator(int id, int clientId, String type, boolean init_state, String serverIP, int entityPort) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.state = init_state;
        this.serverIP = serverIP;
        this.entityPort = entityPort;

        createServerSocket(0);
        createEntitySocket(serverIP, entityPort);

    }

    /** Create a new socket to send events to the entity endpoint
     *
     * @param serverIP the IP address of the endpoint
     * @param entityPort the port number of the endpoint
     */
    private void createEntitySocket(String serverIP, int entityPort){
        Socket socket = null;

        try{

            socket = new Socket(serverIP, entityPort);
            clientOutput = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));


            entityPort = socket.getLocalPort();

            Thread startActuator = new Thread(() -> {
                setEventGenerationFrequency(eventGenerationFrequency); // start the actuator event generation
            });
            startActuator.start();

        }
        catch (Exception e){
            System.out.println("ERROR setting Entity Constructor Endpoint: "+e);
        }


    }

    /**
     * Returns the ID of this entity.
     * @return the ID of this entity
     */
    public int getId() {
        return id;
    }

    /**
     * Create a new socket to receive server messages
     *
     * @param serverport the port number of the endpoint
     */
    public void createServerSocket(int serverport){
        try {

            this.serverPort = serverport;
            serverSocket = new ServerSocket(serverport);

            // start the entity Server
            Thread startActuatorServer = new Thread(this::start);

            startActuatorServer.start();



        } catch (IOException e) {
            System.out.println("ERROR setting Entity Constructor ServerSocket: "+e);
        }
    }


    /**
     * Returns the ID of the client that this entity is registered for.
     * @return the ID of the client that this entity is registered for
     */
    public int getClientId() {
        return clientId;
    }

    /**
     * Returns the type of this entity.
     * @return the type of this entity
     */
    public String getType() {
        return type;
    }

    /**
     * Returns true if this entity is an actuator and false otherwise.
     * @return true if this entity is an actuator and false otherwise
     */
    public boolean isActuator() {
        return true;
    }

    /**
     * Returns the state of this actuator.
     * @return the state of this actuator
     */
    public boolean getState() {
        return state;
    }

    /**
     * Returns the IP address of this entity.
     * @return the IP address of this entity
     */
    public String getIP() {
        return host;
    }

    /**
     * Returns the port number of this entity.
     * @return the port number of this entity
     */
    public int getPort() {
        return entityPort;
    }

    /**
     * Updates the state of this actuator.
     * @param new_state the new state of this actuator
     */
    public void updateState(boolean new_state) {
        this.state = new_state;
    }

    /**
     * Registers the actuator for the given client
     *
     * @return true if the actuator is new (clientID is -1 already) and gets successfully registered or if it is already registered for clientId, else false
     */
    public boolean registerForClient(int clientId) {
        if (this.clientId == -1) {
            this.clientId = clientId;
            return true;
        } else if (this.clientId == clientId) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets or updates the http endpoint that
     * the actuator should send events to
     *
     * @param serverIP the IP address of the endpoint
     * @param serverPort the port number of the endpoint
     */
    public void setEndpoint(String serverIP, int serverPort){
        createEntitySocket(serverIP, serverPort);
    }

    /**
     * Sets the frequency of event generation for the actuator and start the generation of events
     * @param frequency the frequency of event generation in Hz (1/s)
     */
    public void setEventGenerationFrequency(double frequency){
        int tryCount = 0;

        this.eventGenerationFrequency = frequency;

        double commandTime = System.currentTimeMillis();

        while (this.clientId != -1) {
            double currentTime = System.currentTimeMillis();
            if(currentTime-commandTime >=  1000/frequency) {
                //wait 5 seconds
                //Start by getting time at beginning of loop, this is used for while loop frequency
                commandTime = System.currentTimeMillis();

                if (tryCount >= 5) {
                    try {
                        System.out.println("WARNING -> Actuator: " + this.id + " has failed to connect to server 5 consecutive times, thread will sleep for 10s and retry ");
                        tryCount = 0;
                        Thread.sleep(10000); //sleep 10 * 1000 ms = 10s
                        break;

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {

                    tryCount = 0;
                    try {
                        sendEvent(new ActuatorEvent(commandTime, this.clientId, this.id, this.type, generateSwitchValue()));
                    } catch (Exception e) {
                        System.out.println("ERROR setting new Entity Endpoint, retrying connection..... " + e);
                        tryCount += 1;
                    }
                }
            }
        }
    }

    /**
     * Gets the server port
     * @return the server port
     */
    public int getServerPort(){
        return this.serverPort;
    }

    /**
     * Return the port number of the entity endpoint
     * @return the port number of the entity endpoint
     */
    public int getEntityPort(){
        return this.entityPort;
    }

    /**
     * Sends an event to the entity endpoint
     * @param event the event to be sent
     */
    public void sendEvent(Event event) {

        clientOutput.println(event.toString());
        clientOutput.flush();

    }

    /**
     * Returns true if the actuator has sent an event
     * @return true if the actuator has sent an event
     */
    public boolean hasSentEvent() {
        return hasSentEvent;
    }

    /** Process a message received from the server
     *
     * @param command the command to be processed as Request object
     * @throws IOException
     */
    public void processServerMessage(Request command) throws IOException {
        RequestCommand serverCommandToActuator = command.getRequestCommand();

        if (serverCommandToActuator.equals(RequestCommand.SET_STATE)){
            //set the state of the actuator
            boolean newState = Boolean.parseBoolean(command.getRequestData());
            updateState(newState);
        } else if (serverCommandToActuator.equals(RequestCommand.TOGGLE_STATE)){
            state = !state;
        }
    }

    /**
     * Closes all this object's the sockets
     * @throws IOException
     */
    public void close() throws IOException {
        serverIn.close();
        clientOutput.close();
        eventSocket.close();
        serverSocket.close();
    }

    /**
     * Returns a string representation of this actuator.
     * @return a string representation of this actuator
     */
    @Override
    public String toString() {

        return "Actuator{" +
                "getId=" + getId() +
                ",ClientId=" + getClientId() +
                ",EntityType=" + getType() +
                ",IP=" + getIP() +
                ",ServerPort=" + serverPort +
                ",EntityPort=" + entityPort +
                '}';
    }

    /** generates a random boolean value
     *
     * @return a random boolean value
     */
    public static boolean generateSwitchValue() {
        // Generate a random boolean (50-50% chance of true or false)
        // Return the generated value
        return Math.random()>0.5;
    }

    /**
     * Starts the actuator server so it starts reading from its socket
     */
    public void start() {

        try {
            System.out.println("Actuator started on port " + serverPort);
            System.out.println(serverSocket.getInetAddress());

            while (true) {
                Socket incomingSocket = serverSocket.accept();
                System.out.println("Server connected: " + incomingSocket.getInetAddress().getHostAddress());

                // deserializer ServerMessageToActuator
                serverIn = new BufferedReader(new InputStreamReader(incomingSocket.getInputStream()));
                try {
                    // each event is a single line
                    for (String line = serverIn.readLine(); line != null; line = serverIn.readLine()) {

                        System.out.println("request: " + line);

                        Pattern patternR = Pattern.compile("TimeStamp=([-+]?[0-9]+\\.[0-9]+),RequestType=(.+),RequestCommand=(.+),requestData=(.+), clientId=(.+), email=(.+)");
                        Matcher matcherR = patternR.matcher(line);

                        //parsing the event data
                        if (matcherR.find()) {

                            //Registering a new entity ?? NOT SURE IF THIS WORKS
                            Request request = new Request(Double.parseDouble(matcherR.group(1)),
                                    RequestType.valueOf(matcherR.group(2).trim()),
                                    RequestCommand.valueOf(matcherR.group(3).trim()),
                                    matcherR.group(4).trim());


                            processServerMessage(request);

                            break;

                        }
                    }
                } finally {
                    serverIn.close();

                }


            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}