package cpen221.mp3.handler;

import cpen221.mp3.client.Client;
import cpen221.mp3.client.Request;
import cpen221.mp3.client.RequestCommand;
import cpen221.mp3.client.RequestType;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;
import cpen221.mp3.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cpen221.mp3.server.Server.*;


//REP INVARIANTS: Socket != null
class MessageHandlerThread implements Runnable {
    private final Socket incomingSocket;


    HashSet<Server> servers;

    /**
     * Creates an instance of MessageHandlerThread
     * @param incomingSocket the socket associated with the message to process
     */
    public MessageHandlerThread(Socket incomingSocket) {
        this.incomingSocket = incomingSocket;
    }

    /**
     * Creates an instance of MessageHandlerThread, also gives it access to a collection of servers shared
     * among all instances of MessageHandlerThread to ensure that each client only has one associated server
     * @param incomingSocket the socket associated with the message to process
     * @param servers the collection of servers shared among all instances of MessageHandlerThread
     */
    public MessageHandlerThread(Socket incomingSocket, HashSet<Server> servers) {
        this.incomingSocket = incomingSocket;
        this.servers = servers;
    }

    /**
     * Handles the client request or entity event
     */
    @Override
    public void run() {

        try {
            BufferedReader receivedObject = new BufferedReader(new InputStreamReader(
                    incomingSocket.getInputStream()));

            for (String line = receivedObject.readLine(); line != null; line = receivedObject.readLine()) {
                if(line.equals("exit") || line.equals("")){
                    continue;
                }
                messageHandler(line);
            }

            // Close the socket when done
            incomingSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the server associated with a client
     * @param server the server associated with a specific client
     * @return the shared server for the single client
     */
    private synchronized Server getMapServer(Server server) {
        for (Server s : servers) {
            if (s.equals(server)) {
                return s;
            }
        }
        servers.add(server);
        return server;
    }

    /**
     * Helper function to parse the message received
     * @param reply the message to be parsed
     * @throws IOException if the message is not formatted correctly
     */
    private void messageHandler(String reply) throws IOException {
        try {
            long timestamp = System.currentTimeMillis();

            Pattern inputType = Pattern.compile("^(SensorEvent|ActuatorEvent|Request)\\b");

            // Create a Matcher object
            Matcher matcherType = inputType.matcher(reply);

            if (matcherType.find()) {
                // Get the matched word
                String eventType = matcherType.group(1);

                switch (eventType) {
                    case "SensorEvent", "ActuatorEvent":
                        //,ClientId=(\d+),EntityId=(\d+),EntityType=(.+),Value=(.+)}(.+)(.+)
                        Pattern patternSE = Pattern.compile("TimeStamp=([-+]?\\d+\\.\\d+E?(\\d+)?),ClientId=(\\d+),EntityId=(\\d+),EntityType=(.+),Value=(.+)},(.+),(.+)");
                        Matcher matcherSE = patternSE.matcher(reply);

                        //Match 1: TimeStamp (double)
                        //Match 2: Exponent (int)
                        //Match 3: ClientId (int)
                        //Match 4: EntityId (int)
                        //Match 5: EntityType (String)
                        //Match 6: Value (double)
                        //Match 7: clientId (int)
                        //Match 8: email (String)


                        //parsing the event data
                        if (matcherSE.find()) {
                            Event event = null;

                            Client client = new Client(Integer.parseInt(matcherSE.group(7).trim()), matcherSE.group(8).trim(), incomingSocket.getLocalAddress().getHostAddress(), incomingSocket.getLocalPort());
                            Server server = new Server(client);

                            server = getMapServer(server);

                            if (eventType.equalsIgnoreCase("ActuatorEvent")) {
                                //it is an actuator event
                                //Math.pow(Double.parseDouble(matcherSE.group(1)),Integer.parseInt(matcherSE.group(2))
                                //We skip group 2 because it is the exponent of the time stamp
                                event = new ActuatorEvent(Double.parseDouble(matcherSE.group(1)),
                                        Integer.parseInt(matcherSE.group(3).trim()),
                                        Integer.parseInt(matcherSE.group(4).trim()),
                                        matcherSE.group(5).trim(),
                                        Boolean.parseBoolean(matcherSE.group(6).trim()));

                                event.setTimeArrived(System.currentTimeMillis());
                            } else {
                                //it is a sensor event
                                event = new SensorEvent(Double.parseDouble(matcherSE.group(1)),
                                        Integer.parseInt(matcherSE.group(3).trim()),
                                        Integer.parseInt(matcherSE.group(4).trim()),
                                        matcherSE.group(5).trim(),
                                        Double.parseDouble(matcherSE.group(6).trim()));

                                event.setTimeArrived(System.currentTimeMillis());
                            }



                            event.setTimeArrived(System.currentTimeMillis());
                            server.processIncomingEvent(event);


                        }
                        break;

                    case "Request":
                        //\s?RequestCommand=(.+),\s?requestData=\{(.+)},\s?clientId=(\d+),\s?email=(.+)

                        Pattern patternR = Pattern.compile("TimeStamp=([-+]?\\d+\\.\\d+E?(\\d+)?),RequestType=(.+),RequestCommand=(.+),requestData=\\{(.+)},clientId=(\\d+),email=(.+)}");
                        Matcher matcherR = patternR.matcher(reply);

                        //Match 1: TimeStamp (double)
                        //Match 2: Exponent (int)
                        //Match 3: RequestType (String)
                        //Match 4: RequestCommand (String)
                        //Match 5: requestData (String)
                        //Match 6: clientId (int)
                        //Match 7: email (String)

                        //parsing the event data
                        if (matcherR.find()) {
                            Client client = new Client(Integer.parseInt(matcherR.group(6).trim()), matcherR.group(7).trim(), incomingSocket.getLocalAddress().getHostAddress(), incomingSocket.getLocalPort());
                            Server server = new Server(client);

                            server = getMapServer(server);


                            //Registering a new entity ?? NOT SURE IF THIS WORKS
                            Request request = new Request(Double.parseDouble(matcherR.group(1)),
                                    RequestType.valueOf(matcherR.group(3).trim()),
                                    RequestCommand.valueOf(matcherR.group(4).trim()),
                                    matcherR.group(5).trim());

                            request.setReceptionTime(timestamp); //set reception timestamp

                            server.processIncomingRequest(request);

                            break;

                        }

                }
            }
        } catch (NumberFormatException e) {
            throw new IOException("misformatted reply: " + reply);
        }
    }
}