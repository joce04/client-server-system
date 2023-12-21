package cpen221.mp3.client;

import cpen221.mp3.entity.Entity;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;
import cpen221.mp3.handler.MessageHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.regex.*;

//REP INVARIANTS: clientId not null, serverIP not null, serverPort not null, email not null
public class Client {

    private final int clientId;
    private final String email;
    private final String serverIP;
    private final int serverPort;

    // Rep invariant: socket, in, out != null
    private final ServerSocket entitySocket; //acts as a server for an entity
    private Socket destinationSocket; //holds the socket of the server
    private final PrintWriter out;

    public final Map<Integer, Integer> eventsReceived;

    public ArrayList passedEvents;


    int entityPort;
    /**
     * creates a new instance of Client
     * @param clientId the client id
     * @param email the client email
     * @param serverIP the ip (hostname) of its server
     * @param serverPort the port of its server
     */
    public Client(int clientId, String email, String serverIP, int serverPort) {
        this.clientId = clientId;
        this.email = email;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.eventsReceived = new HashMap<>();
        this.passedEvents = new ArrayList();



        try {
            destinationSocket = new Socket(this.serverIP, this.serverPort);
            entitySocket = new ServerSocket(0); //basically assign a random port that is empty
            System.out.println("Assigned port: " + entitySocket.getLocalPort()); //print the port number
            out = new PrintWriter(new OutputStreamWriter(destinationSocket.getOutputStream()));

            //Set the entity port chosen by random 0 default val
            entityPort = entitySocket.getLocalPort();

            Thread startEntityServer = new Thread(() -> {
                try {
                    serve(); // start the entity Server
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            startEntityServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the id of this client
     * @return the id of this client
     */
    public int getClientId() {
        return clientId;
    }

    /**
     * Registers an entity for the client
     *
     * @return true if the entity is new and gets successfully registered, false if the Entity is already registered
     */
    public boolean addEntity(Entity entity) {
        System.out.println("adding entity");
        if (!this.eventsReceived.containsKey(entity.getId())) {
            if (entity.registerForClient(this.clientId)) {
                //successfully registered for this client
                this.eventsReceived.put(entity.getId(), 0);
                return true;
            }
        }
        return false;
     }

    /**
     * Sends an request to the server of the following structure
     * "Request {" +
     *      "timestamp=" + this.timeStamp +
     *      ",RequestType=" + this.requestType.toString() +
     *      ",RequestCommand,=" + this.requestCommand.toString() +
     *      ",requestData=" + this.requestData +
     * '}'
     *
     * @param request the request to be sent
     */
    public void sendRequest(Request request) {
        out.println(request.toString() + "\n");
        out.flush(); //makes sure that the server gets the request
    }

    public void sendEvent(Event event) {
        out.println(event.toString() + "," + this.clientId + "," + this.email + "\n");
        out.flush(); //makes sure that the server gets the request
    }

    /**
     * Parses an event from an entity
     *
     * @return the relevant event
     * @throws IOException if network or server failure
     * Public for testing purposes
     */
    //not sure if this is public or private...
    public Event parseEvent(String reply) throws IOException {
        try {
            //added \\s* for better tolerance to whitespace
            Pattern pattern = Pattern.compile(".*?(.+)\\{TimeStamp=\\s*([-+]?[0-9]*\\.?[0-9]+),\\s*ClientId\\s*=\\s*(\\d+),\\s*EntityId\\s*=\\s*(\\d+),\\s*EntityType\\s*=\\s*(.+?),\\s*Value\\s*=\\s*(.+?)\\}");

            Matcher matcher = pattern.matcher(reply);

            //parsing the event data
            if (matcher.find()) {
                if(matcher.group(1).trim().equalsIgnoreCase("ActuatorEvent")) {
                    //it is an actuator event

                    return new ActuatorEvent(Double.parseDouble(matcher.group(2)),
                            Integer.parseInt(matcher.group(3).trim()),
                            Integer.parseInt(matcher.group(4).trim()),
                            matcher.group(5).trim(),
                            Boolean.parseBoolean(matcher.group(6).trim()));
                } else {
                    //it is a sensor event
                    return new SensorEvent(Double.parseDouble(matcher.group(2)),
                            Integer.parseInt(matcher.group(3).trim()),
                            Integer.parseInt(matcher.group(4).trim()),
                            matcher.group(5).trim(),
                            Double.parseDouble(matcher.group(6).trim()));
                }


            }
        } catch (NumberFormatException e) {
            throw new IOException("misformatted reply: " + reply );
        }
        return null;
    }



    /**
     * Handles one entity connection, returns when the entity disconnects
     * @param socket the socket that the client connects to??
     * @throws IOException if the event is ill-formatted, we return 0
     */
    private void handle(Socket socket) throws IOException {
        System.out.println("client connected");

        BufferedReader input = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));

        PrintWriter out = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream())); //we may not need this, depends on how we communicate with actuators

        try {
            // each event is a single line
            for (String line = input.readLine(); line != null; line = input.readLine()) {

                System.out.println("request: " + line);
                try {
                    Event newEvent = parseEvent(line);
                    assert newEvent != null;
                    eventsReceived.put(newEvent.getEntityId(), eventsReceived.getOrDefault(newEvent.getEntityId(), 0) + 1);
                    passedEvents.add(newEvent);
                    sendEvent(newEvent);
                    if(newEvent instanceof ActuatorEvent) {
                        System.out.println("the event received is from an actuator.");
                    }
                } catch (NumberFormatException|Error e) {
                    // complain about ill-formatted request
                    System.out.println("reply: err");
                }
            }
        } finally {
            out.close();
            input.close();
        }
    }

    /**
     * Run the server, listening for connections and handling them.
     *
     * @throws IOException
     *             if the main server socket is broken
     */
    public void serve() throws IOException {
        while (true) {
            // block until a client connects
            Socket socket = entitySocket.accept();
            Thread entityServer = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        try {
                            handle(socket);
                        } finally {
                            socket.close();
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            });

            //start the thread
            entityServer.start();
        }
    }

    /**
     * Gets the server port of the client
     * @return the server port
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Gets the server IP of the client
     * @return the server IP
     */
    public String getServerIp() {
        return serverIP;
    }

    /**
     * Gets the entity port of the client
     * The port of that the server socket is listening on for the client
     * @return the entity port of the client
     */
    public int getPort() {
        return entityPort;
    }

    /**
     * Evaluates whether this object is equal to another object
     * @param o the object to be compared against
     * @return true if the object is of type Client and they have the same client ID, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Client) {
            Client other = (Client) o;
            return this.clientId == other.clientId;
        }
        return false;
    }

}