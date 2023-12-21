package cpen221.mp3.server;

import cpen221.mp3.client.Client;
import cpen221.mp3.entity.Actuator;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;
import cpen221.mp3.CSVEventReader;

import java.util.*;

import cpen221.mp3.handler.MessageHandler;
import cpen221.mp3.*;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.function.Try;

import static org.junit.jupiter.api.Assertions.*;

public class ServerTests {

    String csvFilePath = "data/tests/single_client_1000_events_out-of-order.csv";
    CSVEventReader eventReader = new CSVEventReader(csvFilePath);
    List<Event> eventList = eventReader.readEvents();

    Client client = new Client(0, "test@test.com", "0.0.0.0", 1);
    Actuator actuator1 = new Actuator(97, 0, "Switch", true);



    @Test
    public void testSetActuatorStateIf() {
        Server server = new Server(client);
        actuator1.setEndpoint(client.getServerIp(), client.getPort());

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
        TimeWindow tw = new TimeWindow(1, 3);
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
        assertEquals(25, result.size());
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
        assertEquals(185, result.get(1).getEntityId());
    }

    @Test
    public void testLastNEventsAllEvents() {
        Server server = new Server(client);
        for (Event value : eventList) {
            value.setTimeArrived(System.currentTimeMillis());
            server.processIncomingEvent(value);
        }
        try {
            Thread.sleep(2000);
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
        assertEquals(90, result.get(4).getEntityId());
    }

    @Test
    public void testUpdateWaitTime() {
        Server server = new Server(client);
        server.updateMaxWaitTime(10);
        for (Event value : eventList) {
            value.setTimeArrived(System.currentTimeMillis());
            server.processIncomingEvent(value);
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
        assertTrue(server.isRunning);
    }

    @Test
    public void testReadLogs() {
        Server server = new Server(client);
        server.logIf(new Filter(BooleanOperator.EQUALS, false));
        for (int i = 0; i < 10 ; i++) {
            eventList.get(i).setTimeArrived(System.currentTimeMillis());
            server.processIncomingEvent(eventList.get(i));
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
        assertEquals(8, server.readLogs().size());
    }

    @Test
    public void eventsInTimeWindow() {
        Server server = new Server(client);
        for (Event value : eventList) {
            value.setTimeArrived(System.currentTimeMillis());
            server.processIncomingEvent(value);
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
        assertEquals(57, server.eventsInTimeWindow(new TimeWindow(4.3,10.2)).size());
    }


}