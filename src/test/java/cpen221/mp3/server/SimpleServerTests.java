package cpen221.mp3.server;

import cpen221.mp3.client.Client;
import cpen221.mp3.entity.Actuator;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;
import cpen221.mp3.CSVEventReader;

import java.util.*;

import cpen221.mp3.handler.MessageHandler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SimpleServerTests {

    String csvFilePath = "data/tests/single_client_1000_events_in-order.csv";
    CSVEventReader eventReader = new CSVEventReader(csvFilePath);
    List<Event> eventList = eventReader.readEvents();

    Client client = new Client(0, "test@test.com", "0.0.0.0", MessageHandler.MESSAGE_HANDLER_PORT);
    Actuator actuator1 = new Actuator(97, 0, "Switch", true);


    @Test
    public void testSetActuatorStateIf() {
        Server server = new Server(client);
        for (int i = 0; i < 10; i++) {
            eventList.get(i).setTimeArrived(System.currentTimeMillis());
            server.processIncomingEvent(eventList.get(i));
        }
        Filter sensorValueFilter = new Filter("value", DoubleOperator.GREATER_THAN_OR_EQUALS, 23);
        server.setActuatorStateIf(sensorValueFilter, actuator1);
        assertTrue(actuator1.getState());
    }

    @Test
    public void testToggleActuatorStateIf() {
        Server server = new Server(client);
        for (int i = 0; i < 10; i++) {
            eventList.get(i).setTimeArrived(System.currentTimeMillis());
            server.processIncomingEvent(eventList.get(i));
        }
        Filter sensorValueFilter = new Filter("value", DoubleOperator.GREATER_THAN_OR_EQUALS, 23);
        server.toggleActuatorStateIf(sensorValueFilter, actuator1);
        assertTrue(actuator1.getState());
    }

    @Test
    public void testEventsInTimeWindow() {
        Server server = new Server(client);
        TimeWindow tw = new TimeWindow(0.2, 1);
        for (int i = 0; i < 100; i++) {
            eventList.get(i).setTimeArrived(System.currentTimeMillis());
            server.processIncomingEvent(eventList.get(i));
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<Event> result = server.eventsInTimeWindow(tw);
        assertEquals(9, result.size());
    }

    @Test
    public void testLastNEvents() {
        Server server = new Server(client);
        for (int i = 0; i < 10; i++) {
            eventList.get(i).setTimeArrived(System.currentTimeMillis());
            server.processIncomingEvent(eventList.get(i));
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<Event> result = server.lastNEvents(2);
        //print the result
        for (Event event : result) {
            System.out.println(event.getTimeStamp()+" "+event.getEntityId());
        }
        assertEquals(2, result.size());
        assertEquals("PressureSensor", result.get(1).getEntityType());
        assertEquals(144, result.get(1).getEntityId());
    }

    @Test
    public void testLastNEvents2() {
        Server server = new Server(client);
        for (int i = 0; i < 20; i++) {
            eventList.get(i).setTimeArrived(System.currentTimeMillis());
            server.processIncomingEvent(eventList.get(i));
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<Event> result = server.lastNEvents(5);
        //print the result
        for (Event event : result) {
            System.out.println(event.getTimeStamp()+" "+event.getEntityId()+ " "+event.getEntityType());
        }
        assertEquals(5, result.size());
        assertEquals("Switch", result.get(4).getEntityType());
        assertEquals(48, result.get(0).getEntityId());
    }

    @Test
    public void testMostActiveEntity() {
        Server server = new Server(client);
        Event event1 = new ActuatorEvent(0.00010015, 0, 11,"Switch", true);
        Event event2 = new SensorEvent(0.000111818, 0, 1,"TempSensor", 1.0);
        Event event3 = new ActuatorEvent(0.00015, 0, 5,"Switch", false);
        Event event4 = new SensorEvent(0.00022, 0, 1,"TempSensor", 11.0);
        Event event5 = new ActuatorEvent(0.00027, 0, 11,"Switch", true);
        Event event6 = new ActuatorEvent(0.00047, 0, 11,"Switch", true);
        List<Event> simulatedEvents = new ArrayList<>();
        simulatedEvents.add(event1);
        simulatedEvents.add(event2);
        simulatedEvents.add(event3);
        simulatedEvents.add(event4);
        simulatedEvents.add(event5);
        simulatedEvents.add(event6);
        for (int i = 0; i < simulatedEvents.size(); i++) {
            eventList.get(i).setTimeArrived(System.currentTimeMillis());
            server.processIncomingEvent(simulatedEvents.get(i));
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        int mostActiveEntity = server.mostActiveEntity();
        assertEquals(11, mostActiveEntity);
    }

    @Test
    public void testGetAllEntities() {
        Server server = new Server(client);
        Event event1 = new ActuatorEvent(0.00010015, 0, 11,"Switch", true);
        Event event2 = new SensorEvent(0.000111818, 0, 1,"TempSensor", 1.0);
        Event event3 = new ActuatorEvent(0.00015, 0, 5,"Switch", false);
        Event event4 = new SensorEvent(0.00022, 0, 1,"TempSensor", 11.0);
        Event event5 = new ActuatorEvent(0.00027, 0, 11,"Switch", true);
        Event event6 = new ActuatorEvent(0.00047, 0, 11,"Switch", true);
        List<Event> simulatedEvents = new ArrayList<>();
        simulatedEvents.add(event1);
        simulatedEvents.add(event2);
        simulatedEvents.add(event3);
        simulatedEvents.add(event4);
        simulatedEvents.add(event5);
        simulatedEvents.add(event6);
        Set<Integer> expected = new HashSet<>();
        for (int i = 0; i < simulatedEvents.size(); i++) {
            eventList.get(i).setTimeArrived(System.currentTimeMillis());
            System.out.println(eventList.get(i).getTimeArrived());
            server.processIncomingEvent(simulatedEvents.get(i));
            int entityId = (simulatedEvents.get(i)).getEntityId();
            expected.add(entityId);
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Set<Integer> result = new HashSet<>(server.getAllEntities());
        assertEquals(expected,result);
    }
}