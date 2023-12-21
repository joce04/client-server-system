package cpen221.mp3.server;

import cpen221.mp3.client.RequestCommand;
import cpen221.mp3.client.RequestType;
import cpen221.mp3.entity.Actuator;
import cpen221.mp3.client.Client;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.client.Request;

import java.io.*;
import java.net.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//REP INVARIANTS: BUFFER_TIME != null
public class Server {
    public static int BUFFER_TIME = 10; //in ms

    private Client client;
    private double maxWaitTime = 2; //in s

    //this is a list of all event (entity) ids that are logged (based on a filter)
    private List<Event> loggedEvents;

    //this is a buffer list of all the events to process
    //events are removed from this list as they are processed
    //the events to process list is resorted after BUFFER_TIME
    //Public for the purposes of testing
    public Queue<Event> eventsToProcess;
    //This is a buffer list of all the requests to process
    //request are removed from this list as they are processed
    //the requests to process list is resorted after BUFFER_TIME
    //Public for the purposes of testing
    public Queue<Request> requestsToProcess;

    //all events currently in the server that HAVE BEEN processed
    //in the order that they were processed
    public List<Event> allEvents;


    //keeps track of whether we are processing events
    public boolean isRunning;

    private Filter filter = null;
    private double recentTimestamp;

    /**
     * Create a server for a given client.
     *
     * @param client the client of that server
     */
    public Server(Client client) {
        // implement the Server constructor
        this.client = client;
        this.eventsToProcess = new LinkedList<>();
        this.requestsToProcess = new LinkedList<>();
        this.allEvents = new LinkedList<>();
        this.loggedEvents = new ArrayList<>();
        this.isRunning = false;
        this.recentTimestamp = 0;
    }
    /**
     * Create a new server
     */
    public Server() {
        // implement the Server constructor
        this.client = null;
        this.eventsToProcess = new LinkedList<>();   // thread-safe, https://www.geeksforgeeks.org/copyonwritearraylist-in-java/
        this.requestsToProcess = new LinkedList<>();
        this.allEvents = new LinkedList<>();
        this.loggedEvents = new ArrayList<>();
        this.isRunning = false;
        this.recentTimestamp = 0;
    }

    /**
     * Set the client of the server.
     */
    public void setClient(Client client) {
        this.client = client;
    }

    /**
     * Update the max wait time for the client.
     * The max wait time is the maximum amount of time
     * that the server can wait for before starting to process each event of the client:
     * It is the difference between the time the message was received on the server
     * (not the event timeStamp from above) and the time it started to be processed.
     *
     * @param maxWaitTime the new max wait time
     */
    public void updateMaxWaitTime(double maxWaitTime) {
        // check if its negative
        if (maxWaitTime < 0){
            throw new IllegalArgumentException("Max wait time can't be negative");
        }
        this.maxWaitTime = maxWaitTime;
    }

    /**
     * Set the actuator state if the given filter is satisfied by the latest event.
     * Here the latest event is the event with the latest timestamp not the event
     * that was received by the server the latest.
     * If the actuator is not registered for the client, then this method should do nothing.
     *
     * @param filter the filter to check
     * @param actuator the actuator to set the state of as true
     */
    public void setActuatorStateIf(Filter filter, Actuator actuator) {
        Event latestEvent = getLatestEvent();

        // Check if the latest event satisfies the filter
        if (latestEvent != null && filter.satisfies(latestEvent)) {
            // Check if the actuator is registered for this client
            if (actuator.getClientId() == this.client.getClientId()) {

                // Update the state of the actuator
                boolean newState = true; // Assuming we are setting the state to true

                String IP = actuator.getIP();
                int port = actuator.getServerPort();

                try{
                    Socket actSocket = new Socket(IP, port);
                    PrintWriter clientOutput = new PrintWriter(new OutputStreamWriter(actSocket.getOutputStream()));

                    Request request = new Request(RequestType.CONTROL, RequestCommand.SET_STATE, "true");

                    //Need to do this due to toString limitations. They will not be used in Actuator
                    request.setEmail("");
                    request.setClientId(0);

                    // Send the actuator event

                    clientOutput.print(request);  //Write to the output stream
                    clientOutput.flush();  //Close stream

                }catch (IOException e){
                    System.out.println("Error sending setActuatorState command: " + e.getMessage());
                    e.printStackTrace();
                }


            }
        }

    }

    /**
     * Get the latest event of the client.
     * Here the latest event is the event with the latest timestamp not the event
     * that was received by the server the latest.
     *
     * If no events exist for the client, then this method should return null.
     *
     * @return the latest event of the client
     */
    private Event getLatestEvent() {
        return this.allEvents.stream().max(Comparator.comparingDouble(Event::getTimeStamp)).orElse(null);
    }

    /**
     * Toggle the actuator state if the given filter is satisfied by the latest event.
     * Here the latest event is the event with the latest timestamp not the event
     * that was received by the server the latest.
     *
     * If the actuator has never sent an event to the server, then this method should do nothing.
     * If the actuator is not registered for the client, then this method should do nothing.
     *
     * @param filter the filter to check
     * @param actuator the actuator to toggle the state of (true -> false, false -> true)
     */
    public void toggleActuatorStateIf(Filter filter, Actuator actuator) {
        //TODO: fix the true default state

        // Check if the actuator has sent an event and is registered for this client
        if (actuator.hasSentEvent() && actuator.getClientId() == this.client.getClientId()) {
            Event latestEvent = getLatestEvent();

            // Check if the latest event satisfies the filter
            if (latestEvent != null && filter.satisfies(latestEvent)) {
                // Toggle the state of the actuator
                boolean newState = !actuator.getState();
                String IP = actuator.getIP();
                int port = actuator.getServerPort();



                try{
                    Socket actSocket = new Socket(IP, port);
                    PrintWriter clientOutput = new PrintWriter(new OutputStreamWriter(actSocket.getOutputStream()));

                    //new Request(ServerCommandToActuator, actuator.toString() + "," + filter.toString(), System.currentTimeMillis());

                    // Send the actuator event
                    clientOutput.println("CONTROL_SET_ACTUATOR_STATE");

                    clientOutput.print("toggle");  //Write to the output stream
                    clientOutput.flush();  //Close stream

                }catch (IOException e){
                    System.out.println("Error sending setActuatorState command: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Log the event ID for which a given filter was satisfied.
     * This method is checked for every event received by the server.
     *
     * @param filter the filter to check
     */
    public void logIf(Filter filter) {
        this.filter = filter;
        this.loggedEvents = new ArrayList<>();
    }

    /**
     * Return all the logs made by the "logIf" method so far.
     * If no logs have been made, then this method should return an empty list.
     * The list should be sorted in the order of event timestamps.
     * After the logs are read, they should be cleared from the server.
     *
     * @return list of event IDs
     */
     public List<Integer> readLogs() {

        List<Integer> readLogs = loggedEvents.stream()
                .sorted((x1, x2) -> Double.compare(x2.getTimeStamp(), x1.getTimeStamp()))
                .map(Event::getEntityId)
                .toList();

        //remove each logged event that has been read
        this.loggedEvents = new ArrayList<>();

        return readLogs;
    }


    /**
     * List all the events of the client that occurred in the given time window.
     * Here the timestamp of an event is the time at which the event occurred, not
     * the time at which the event was received by the server.
     * If no events occurred in the given time window, then this method should return an empty list.
     *
     * @param timeWindow the time window of events, inclusive of the start and end times
     * @return list of the events for the client in the given time window
     */
    public List<Event> eventsInTimeWindow(TimeWindow timeWindow) {
        // implement this method
        return allEvents.stream()
                .filter(event -> event.getTimeStamp() >= timeWindow.getStartTime())
                .filter(event -> event.getTimeStamp() <= timeWindow.getEndTime()).toList();
    }

    /**
     * Returns a set of IDs for all the entities of the client for which
     * we have received events so far.
     * Returns an empty list if no events have been received for the client.
     *
     * @return list of all the entities of the client for which we have received events so far
     */
    public List<Integer> getAllEntities() {
        // implement this method
        Set<Integer> uniqueEntityIds = new HashSet<>();  //Sets for unique IDs

        for (Event event : allEvents) {
            uniqueEntityIds.add(event.getEntityId());
        }

        // Convert set to list to match return type
        return new ArrayList<>(uniqueEntityIds);
    }

    /**
     * List the latest n events of the client.
     * Here the order is based on the original timestamp of the events, not the time at which the events were received by the server.
     * If the client has fewer than n events, then this method should return all the events of the client.
     * If no events exist for the client, then this method should return an empty list.
     * If there are multiple events with the same timestamp in the boundary,
     * the ones with largest EntityId should be included in the list.
     *
     * @param n the max number of events to list
     * @return list of the latest n events of the client
     */
    public List<Event> lastNEvents(int n) {
        // Sort the events in ascending order
        List<Event> sortedEvents = allEvents.stream()
                .sorted(Comparator.comparingDouble(Event::getTimeStamp)
                        .thenComparingInt(Event::getEntityId))
                .toList();

        // Get the last n events, or fewer if there aren't enough events
        int startIndex = Math.max(0, sortedEvents.size() - n);
        return sortedEvents.subList(startIndex, sortedEvents.size());
    }

    /**
     * returns the ID corresponding to the most active entity of the client
     * in terms of the number of events it has generated.
     *
     * If there was a tie, then this method should return the largest ID.
     *
     * @return the most active entity ID of the client
     */
    public int mostActiveEntity() {
        // implement this method
        Map<Integer, Integer> entityEventCounts = new HashMap<>();

        // Count the number of events generated by each entity
        for (Event event : allEvents) {
            int entityId = event.getEntityId();
            if (entityEventCounts.containsKey(entityId)) {
                // If the entity is already in the map, increment its count
                entityEventCounts.put(entityId, entityEventCounts.get(entityId) + 1);
            } else {
                // If the entity is not in the map, add it with a count of 1
                entityEventCounts.put(entityId, 1);
            }
        }
        return getMostActiveEntityId(entityEventCounts);
    }

    /**
     * Returns the ID corresponding to the most active entity of the client
     * in terms of the number of events it has generated.
     *
     * If there was a tie, then this method should return the largest ID.
     *
     * @param entityEventCounts a map of entity IDs to the number of events generated by each entity
     * @return the most active entity ID of the client
     */
    private static int getMostActiveEntityId(Map<Integer, Integer> entityEventCounts) {
        int mostActiveEntityId = -1; // Initialize to an invalid ID
        int maxEventCount = 0;

        // Find the entity with the highest event count
        for (Map.Entry<Integer, Integer> entry : entityEventCounts.entrySet()) {
            int entityId = entry.getKey();
            int eventCount = entry.getValue();

            if (eventCount > maxEventCount ||  (eventCount == maxEventCount && entityId > mostActiveEntityId)) {
                mostActiveEntityId = entityId;
                maxEventCount = eventCount;
            }
        }
        return mostActiveEntityId;
    }

    /**
     * the client can ask the server to predict what will be
     * the next n timestamps for the next n events
     * of the given entity of the client (the entity is identified by its ID).
     *
     * If the server has not received any events for an entity with that ID,
     * or if that Entity is not registered for the client, then this method should return an empty list.
     *
     * @param entityId the ID of the entity
     * @param n the number of timestamps to predict
     * @return list of the predicted timestamps
     */
    public List<Double> predictNextNTimeStamps(int entityId, int n) {
        // TODO: implement this method
        return null;
    }

    /**
     * the client can ask the server to predict what will be
     * the next n values of the timestamps for the next n events
     * of the given entity of the client (the entity is identified by its ID).
     * The values correspond to Event.getValueDouble() or Event.getValueBoolean()
     * based on the type of the entity. That is why the return type is List<Object>.
     *
     * If the server has not received any events for an entity with that ID,
     * or if that Entity is not registered for the client, then this method should return an empty list.
     *
     * @param entityId the ID of the entity
     * @param n the number of double value to predict
     * @return list of the predicted timestamps
     */
    public List<Object> predictNextNValues(int entityId, int n) {
        // TODO: implement this method
        return null;
    }

    /**
     * Adds an event to the processing queue
     * @param event the event to process, requires the event is not null
     */
    public void processIncomingEvent(Event event) {
        eventsToProcess.add(event);
        if(!this.isRunning) {
            //we are not running a processing thread anymore, so start processing events in a new thread
            Thread processEvents = new Thread(this::processAll);
            processEvents.start();
        }
    }

    /**
     * Adds the request to the processing queue
     * @param request the request to process, requires the requeset is not null
     */
    public void processIncomingRequest(Request request) {
        this.requestsToProcess.add(request);
        if(!this.isRunning) {
            Thread processEvents = new Thread(this::processAll);
            processEvents.start();
        }
    }

    /**
     * Start processing events and requests in their respective queues in another thread
     * This is where we ensure quality of service and that each event (and request) is processed in the order
     * that it is received
     */
    synchronized private void processAll() {
        this.isRunning = true;
        //run this while we still have events to process
        long bufferTime = System.currentTimeMillis();
        sortEvents();
        sortRequests();
        while(!this.eventsToProcess.isEmpty() || !this.requestsToProcess.isEmpty()) {
            //sort the events and requests if BUFFER_TIME has passed
            if(System.currentTimeMillis() - bufferTime >= BUFFER_TIME) {
                if (!this.eventsToProcess.isEmpty()) {
                    sortEvents();
                }

                if (!this.requestsToProcess.isEmpty()) {
                    sortRequests();
                }
                bufferTime = System.currentTimeMillis();
            }

            //gives the max wait time in seconds, with a little bit of wiggle room
            double waitTime = maxWaitTime * 1000 - 50;

            //get event or the request with the smaller timestamp
            if(!this.eventsToProcess.isEmpty() && !this.requestsToProcess.isEmpty()) {
                //both lists have requests and events
                assert this.eventsToProcess.peek() != null;
                assert this.requestsToProcess.peek() != null;
                if (this.eventsToProcess.peek().getTimeStamp() <= this.requestsToProcess.peek().getTimeStamp()) {
                    if (System.currentTimeMillis() - this.eventsToProcess.peek().getTimeArrived() < waitTime) {
                        //we haven't hit the maxWaitTime, skip over the rest of the code
                        continue;
                    }
                    //we have hit the max, run the event first
                    runEvent(eventsToProcess.remove());
                } else {
                    //check if the request has hit their max wait time
                    if (System.currentTimeMillis() - this.requestsToProcess.peek().getReceptionTime() < waitTime) {
                        //we haven't hit the max, so skip over the rest of the code
                        continue;
                    }
                    //we have hit the max, run the command
                    runRequestCommand(requestsToProcess.remove());
                }
            } else if (!eventsToProcess.isEmpty()) {
                //only the eventsToProcess queue has elements to process
                assert this.eventsToProcess.peek() != null;

                if (System.currentTimeMillis() - this.eventsToProcess.peek().getTimeArrived() < waitTime) {
                    //we haven't hit the maxWaitTime, skip over the rest of the code
                    continue;
                }
                //we have hit the max, run the event processing
                runEvent(eventsToProcess.remove());
            } else {
                //only the requestsToProcess queue has elements to process
                //check if the request has hit their max wait time
                assert this.requestsToProcess.peek() != null;
                if (System.currentTimeMillis() - this.requestsToProcess.peek().getReceptionTime() < waitTime) {
                    //we haven't hit the max, so skip over the rest of the code
                    continue;
                }
                //we have hit the max, run the command
                runRequestCommand(requestsToProcess.remove());
            }
        }

        this.isRunning = false;
    }

    /**
     * Processes the event that is sent in, initiates reprocessing if the event's timestamp
     * is earlier than the timestamp that was just processed. Also evaluates if the event
     * satisfies the filter.
     * @param event the event to be processed, requires event must not be null
     */
    private void runEvent(Event event) {
        System.out.println("Event processed");
        //check if this event has a time stamp earlier than the previous one
        if(event.getTimeStamp() < this.recentTimestamp) {
            //the event came earlier, we need to reprocess
            reprocessEvent(event);
        } else {
            //check if there is a filter
            if (this.filter != null) {
                //logging functionality
                if (this.filter.satisfies(event)) {
                    loggedEvents.add(event);
                }
            }
            this.allEvents.add(event);
        }
        this.recentTimestamp = event.getTimeStamp();
    }

    /**
     * Inserts the event in the correct location in our data storage based off its timestamp
     * @param event the event to be stored, requires event is not null
     * @return a list of all the events that occur after the event's timestamp sorted in descending order
     */
    private List<Event> destroyBadEvents(Event event) {
        List<Event> badEvents = new ArrayList<>();
        double timestamp = event.getTimeStamp();

        for(int k = this.allEvents.size() - 1; k >= 0; k--) {
            //start at the end of the array and search until we get to an event with a timestamp that is
            //less than the timestamp provided
            if(this.allEvents.get(k).getTimeStamp() < timestamp) {
                //the event occurred after this event
                allEvents.add(k+1, event);
                break;
            }
            badEvents.add(this.allEvents.get(k));
        }

        return badEvents;
    }

    /**
     * reprocessing the event, evaluates if the values after the event are in the filtered list
     * if they are, then we log the event in the correct location of the list. If they are not,
     * then we do not add the event to the loggedEvents list.
     * @param event the event that has caused the reprocessing, requires the event is not null
     */
    private void reprocessEvent(Event event) {
        //destroys instances of the bad events
        List<Event> badEvents = destroyBadEvents(event);

        //check if any logging needs to be updated
        if (filter != null) {
            int size = loggedEvents.size() - badEvents.size();
            boolean logged = true;
            int i = 0;
            for (int k = loggedEvents.size() - 1; k >= size; k--) {
                if (loggedEvents.get(k) != badEvents.get(i)) {
                    logged = false;
                }
                i++;
            }

            if (logged) {
                //add in the new event at the correct spot in the array
                if (this.filter.satisfies(event)) {
                    loggedEvents.add(size, event);
                }
            }
        }
    }

    /**
     * Sorts the events in the event processing queue based off their timestamps in ascending order
     */
    private void sortEvents() {
        this.eventsToProcess = this.eventsToProcess.stream()
                .sorted(Comparator.comparingDouble(Event::getTimeStamp))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Sorts the requests in the request processing queue based off their timestamps in ascending order
     */
    private void sortRequests() {
        this.requestsToProcess = this.requestsToProcess.stream()
                .sorted(Comparator.comparingDouble(Request::getTimeStamp))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /** deserializeFilter
     * This method takes a string representation of a filter and returns the corresponding Filter object
     * @param filterString the string representation of the filter
     * @return the Filter object
     */
    private Filter deserializeFilter(String filterString) {
        String boolFilterPattern = "Boolean Filter: Operator=(.*), Value=(.*)";
        String doubleFilterPattern = "Double Filter: Field=(.*), Operator=(.*), Value=(.*)";
        String compositeFilterPattern = "Composite Filter: \\[(.*)]";

        // Match the input against each pattern
        Matcher boolMatcher = Pattern.compile(boolFilterPattern).matcher(filterString);
        Matcher doubleMatcher = Pattern.compile(doubleFilterPattern).matcher(filterString);
        Matcher compositeMatcher = Pattern.compile(compositeFilterPattern).matcher(filterString);

        if (boolMatcher.matches()) {
            // Extract values and reconstruct the object for Boolean Filter
            BooleanOperator boolOperator = BooleanOperator.valueOf(boolMatcher.group(1));
            boolean boolValue = Boolean.parseBoolean(boolMatcher.group(2));
            return new Filter(boolOperator, boolValue);

        } else if (doubleMatcher.matches()) {
            // Extract values and reconstruct the object for Double Filter
            String field = doubleMatcher.group(1);
            DoubleOperator doubleOperator = DoubleOperator.valueOf(doubleMatcher.group(2));
            double doubleValue = Double.parseDouble(doubleMatcher.group(3));
            return new Filter(field, doubleOperator, doubleValue);

        } else if (compositeMatcher.matches()) {
            // Extract values and reconstruct the object for Composite Filter
            String composedFilters = compositeMatcher.group(1);
            List<Filter> filters = parseComposedFilters(composedFilters);
            return new Filter(filters);
        } else {
            // Handle the case where the input string doesn't match any expected pattern
            throw new IllegalArgumentException("Invalid input format");
        }
    }

    /** This is a helper method to parse a string representation of a list of filters
     * @param composedFilters the string representation of the list of filters
     * @return the list of filters
     */
    private List<Filter> parseComposedFilters(String composedFilters) {
        // Split the input string into individual filters
        String[] filterStrings = composedFilters.split(", ");

        // Parse each filter string and add it to the list of filters
        List<Filter> filters = new ArrayList<>();
        for (String filterString : filterStrings) {
            filters.add(deserializeFilter(filterString));
        }
        return filters;
    }

    /** deserializeActuator
     * This method takes a string representation of an actuator and returns the corresponding Actuator object
     * @param actuatorString the string representation of the actuator
     * @return the Actuator object
     */
    private Actuator deserializeActuator(String actuatorString){
        String actuatorPattern = "Actuator\\{getId=(\\d+),ClientId=(\\d+),EntityType=(.*),IP=(.*),ServerPort=(\\d+),EntityPort=(\\d+)}";

        // Match the input against the pattern
        Matcher actuatorMatcher = Pattern.compile(actuatorPattern).matcher(actuatorString);

        if (actuatorMatcher.matches()) {
            // Extract values and reconstruct the object for Actuator
            int id = Integer.parseInt(actuatorMatcher.group(1));
            int clientId = Integer.parseInt(actuatorMatcher.group(2));
            String type = actuatorMatcher.group(3);
            String ip = actuatorMatcher.group(4);
            int serverPort = Integer.parseInt(actuatorMatcher.group(5));
            int entityPort = Integer.parseInt(actuatorMatcher.group(6));

            Actuator actuator = new Actuator(id, clientId, type, false, ip, entityPort);
            actuator.createServerSocket(serverPort);

            return actuator;
        } else {
            // Handle the case where the input string doesn't match the expected pattern
            throw new IllegalArgumentException("Invalid input format for Actuator");
        }

    }

    /** deserializeTimeWindow
     * This method takes a string representation of a time window and returns the corresponding TimeWindow object
     * @param timeWindowString the string representation of the time window
     * @return the TimeWindow object
     */
    private TimeWindow deserializeTimeWindow(String timeWindowString){
        String timeWindowPattern = "TimeWindow\\{StartTime=(.*),EndTime=(.*)\\}";

        // Match the input against the pattern
        Matcher timeWindowMatcher = Pattern.compile(timeWindowPattern).matcher(timeWindowString);

        if (timeWindowMatcher.matches()) {
            // Extract values and reconstruct the object for TimeWindow
            double startTime = Double.parseDouble(timeWindowMatcher.group(1));
            double endTime = Double.parseDouble(timeWindowMatcher.group(2));

            return new TimeWindow(startTime, endTime);
        } else {
            // Handle the case where the input string doesn't match the expected pattern
            throw new IllegalArgumentException("Invalid input format for TimeWindow");
        }
    }

    /**
     * Runs the command of the given request inside server
     * @param request the request to run
     */
    private void runRequestCommand(Request request) {
        System.out.println("Request processed");
        String data = request.getRequestData();
        switch(request.getRequestCommand()){
            case CONFIG_UPDATE_MAX_WAIT_TIME:
                double maxWaitTime = Double.parseDouble(request.getRequestData());
                updateMaxWaitTime(maxWaitTime);
                break;


            case CONTROL_SET_ACTUATOR_STATE: //Assume that we need to have data = "Filter.toString()" + "Actuator.toString()

                Pattern patternR = Pattern.compile("(.+),(.+)");
                Matcher matcherR = patternR.matcher(data);

                if (matcherR.find()) {

                    Filter filter = deserializeFilter(matcherR.group(1));
                    Actuator actuator = deserializeActuator(matcherR.group(2));

                    assert actuator != null;
                    assert filter != null;

                    setActuatorStateIf(filter, actuator);
                }

                break;

            case CONTROL_TOGGLE_ACTUATOR_STATE:

                Pattern pattern2 = Pattern.compile("(.+),(.+)");
                Matcher matcher2 = pattern2.matcher(data);

                if (matcher2.find()) {

                    Filter filter = deserializeFilter(matcher2.group(1));
                    Actuator actuator = deserializeActuator(matcher2.group(2));

                    assert actuator != null;
                    assert filter != null;
                    toggleActuatorStateIf(filter, actuator);
                }

                break;
            case CONTROL_NOTIFY_IF:
                Pattern pattern3 = Pattern.compile("(.+)");
                Matcher matcher3 = pattern3.matcher(data);

                if (matcher3.find()) {

                    Filter filter = deserializeFilter(matcher3.group(1));


                    assert filter != null;

                    logIf(filter);
                }

                break;
            case ANALYSIS_GET_EVENTS_IN_WINDOW:
                Pattern pattern4 = Pattern.compile("(.+)");
                Matcher matcher4 = pattern4.matcher(data);
                if (matcher4.find()) {
                    TimeWindow timeWindow = deserializeTimeWindow(matcher4.group(1));

                    eventsInTimeWindow(timeWindow);
                }
                break;

            case ANALYSIS_GET_ALL_ENTITIES:
                getAllEntities();

                break;
            case ANALYSIS_GET_LATEST_EVENTS:
                getLatestEvent();

                break;
            case ANALYSIS_GET_MOST_ACTIVE_ENTITY:
                mostActiveEntity();

                break;
            case PREDICT_NEXT_N_TIMESTAMPS:
                Pattern pattern5 = Pattern.compile("(\\d), (\\d)");
                Matcher matcher5 = pattern5.matcher(data);
                if (matcher5.find()) {
                    int entityId = Integer.parseInt(matcher5.group(1));
                    int n = Integer.parseInt(matcher5.group(2));

                    predictNextNTimeStamps(entityId, n);
                }
                break;

            case PREDICT_NEXT_N_VALUES:
                Pattern pattern6 = Pattern.compile("(\\d), (\\d)");
                Matcher matcher6 = pattern6.matcher(data);
                if (matcher6.find()) {
                    int entityId = Integer.parseInt(matcher6.group(1));
                    int n = Integer.parseInt(matcher6.group(2));

                    predictNextNValues(entityId, n);
                }
                break;


        }
    }

    /**
     * Evaluates whether a different object is equal to this project
     * @param obj the object to determine if it is the same as this object
     * @return true if that object is equal to this object, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Server) {
            Server other = (Server) obj;
            return this.client.equals(other.client);
        }
        return false;
    }

}