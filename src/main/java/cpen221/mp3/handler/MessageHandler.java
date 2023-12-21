package cpen221.mp3.handler;

import cpen221.mp3.client.Client;
import cpen221.mp3.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//REP INVARIANTS: port != null
public class MessageHandler {
    public static final int MESSAGE_HANDLER_PORT = 1;
    private ServerSocket serverSocket;
    private int port;

    private HashSet<Server> servers;

    /**
     * The constructor for the message handler,
     * Starts a server when instantiated
     * @param port starts a server on this port
     */
    public MessageHandler(int port) {
        this.port = port;
        this.servers = new HashSet<>();
        //starts the server upon instantiation
        System.out.println("******SERVER IS STARTING******");
        Thread startServer = new Thread(new Runnable() {
            @Override
            public void run() {
                start();
            }
        });
        startServer.start();
    }

    /**
     * Used for testing purposes,
     * Adds a server into our collection of available servers for the clients to chose from
     * @param server the server to be added into our collection
     */
    public void addServer(Server server) {
        this.servers.add(server);
    }

    /**
     * Starts running a server on the specified port
     */
    public void start() {
        // the following is just to get you started
        // you may need to change it to fit your implementation
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            System.out.println(serverSocket.getInetAddress());

            while (true) {


                Socket incomingSocket = serverSocket.accept();
                System.out.println("Client/Entity connected: " + incomingSocket.getInetAddress().getHostAddress());

                // create a new thread to handle the client request or entity event
                Thread handlerThread = new Thread(new MessageHandlerThread(incomingSocket, servers));
                handlerThread.start();

            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main function starts a server, the server is started in the constructor
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        MessageHandler mh = new MessageHandler(MessageHandler.MESSAGE_HANDLER_PORT);
    }
}
