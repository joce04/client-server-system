package cpen221.mp3.server;

import cpen221.mp3.client.Client;
import cpen221.mp3.client.Request;
import cpen221.mp3.client.RequestCommand;
import cpen221.mp3.client.RequestType;
import cpen221.mp3.entity.Actuator;
import cpen221.mp3.entity.Sensor;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;
import cpen221.mp3.CSVEventReader;

import java.util.*;

import cpen221.mp3.handler.MessageHandler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SystemTests {

    //NOTICE THE TESTS DO NOT RUN PROPERLY WHEN EXECUTED IN SUITE. THEY MUST BE RUN INDIVIDUALLY

    String csvFilePath = "data/tests/single_client_1000_events_out-of-order.csv";
    CSVEventReader eventReader = new CSVEventReader(csvFilePath);
    List<Event> eventList = eventReader.readEvents();
    MessageHandler mh = new MessageHandler(MessageHandler.MESSAGE_HANDLER_PORT);
    Client client = new Client(0, "test@test.com", "0.0.0.0", MessageHandler.MESSAGE_HANDLER_PORT);
    Server server = new Server(client);

    @Test
    public void testClientToServer() {
        Sensor sensor = new Sensor(34, client.getClientId(), "TempSensor", client.getServerIp(), client.getPort());
        Filter filter = new Filter("value", DoubleOperator.GREATER_THAN_OR_EQUALS, 20);
        Request request = new Request(RequestType.CONFIG, RequestCommand.CONTROL_NOTIFY_IF, filter.toString());
        client.sendRequest(request);
        System.out.println("Request: "+ request.getTimeStamp());
        mh.addServer(server);

        for(int i = 0; i < 5; i++) {
            client.sendEvent(new SensorEvent(System.currentTimeMillis(), client.getClientId(), sensor.getId(), "TempSensor", i * 10));
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("All Events: " +server.allEvents);
        System.out.println("Events to be processed: " + server.eventsToProcess);
        assertEquals(3, server.readLogs().size());

    }

    @Test
    public void testClientToServer1() {
        Actuator actuator1 = new Actuator(97, 0, "Switch", true, client.getServerIp(), client.getPort());
        Filter filter = new Filter(BooleanOperator.EQUALS, true);
        Request request = new Request(RequestType.CONFIG, RequestCommand.CONTROL_NOTIFY_IF, filter.toString());
        client.sendRequest(request);
        System.out.println("Request: "+ request.getTimeStamp());
        mh.addServer(server);

        for(int i = 0; i < 5; i++) {
            client.sendEvent(new ActuatorEvent(System.currentTimeMillis(), actuator1.getClientId(), actuator1.getId(), "Switch", i % 2 == 0));
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("All Events: " +server.allEvents);
        System.out.println("Events to be processed: " + server.eventsToProcess);
        assertEquals(3, server.readLogs().size());

    }

    @Test
    public void testEntityToClient() {
        Actuator actuator1 = new Actuator(97, 0, "Switch", true, client.getServerIp(), client.getPort());
        mh.addServer(server);


        try {
            Thread.sleep(11000); //10S + 1S for the server to process the event
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }



        assertEquals(2, client.passedEvents.size());

    }

    @Test
    public void testEntityToClient2() {
        Actuator actuator1 = new Actuator(97, 0, "Switch", true, client.getServerIp(), client.getPort());
        mh.addServer(server);

        try {
            Thread.sleep(23000); //0S + 3S for the server to process the with buffers
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }



        assertEquals(4, client.passedEvents.size());
        assertEquals(4, server.allEvents.size());

    }

    @Test
    public void testEntityToClient3() {
        Actuator actuator1 = new Actuator(99, 0, "Switch", true, client.getServerIp(), client.getPort());
        Actuator actuator2 = new Actuator(97, 0, "Switch", true, client.getServerIp(), client.getPort());
        mh.addServer(server);


        try {
            Thread.sleep(14000); //0S + 3S for the server to process the with buffers
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }



        assertEquals(4, client.passedEvents.size());
        assertEquals(4, server.allEvents.size());

    }


    @Test
    public void requestTest() {
        Actuator actuator1 = new Actuator(99, 0, "Switch", true, client.getServerIp(), client.getPort());
        mh.addServer(server);

        Request request = new Request(RequestType.CONTROL, RequestCommand.CONTROL_TOGGLE_ACTUATOR_STATE, "value,>=,20");

        try {
            Thread.sleep(14000); //0S + 3S for the server to process the with buffers
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        client.sendRequest(request);

        assertEquals(2, client.passedEvents.size());
        assertEquals(2, server.allEvents.size());

    }
}
