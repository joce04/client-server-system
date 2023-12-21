package cpen221.mp3.server;

import cpen221.mp3.event.Event;

import java.util.ArrayList;
import java.util.List;

enum DoubleOperator {
    EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN_OR_EQUALS
}

enum BooleanOperator {
    EQUALS,
    NOT_EQUALS
}

//REP INVARIANTS, composedFilters.size >= 1
public class Filter {
    // you can add private fields and methods to this class
    private BooleanOperator boolOperator;
    private DoubleOperator doubleOperator;
    private String field;
    private boolean boolValue;
    private double doubleValue;
    private List<Filter> composedFilters;

    private boolean isDoubleOperator;


    /**
     * Constructs a filter that compares the boolean (actuator) event value
     * to the given boolean value using the given BooleanOperator.
     * (X (BooleanOperator) value), where X is the event's value passed by satisfies or sift methods.
     * A BooleanOperator can be one of the following:
     *
     * BooleanOperator.EQUALS
     * BooleanOperator.NOT_EQUALS
     *
     * @param operator the BooleanOperator to use to compare the event value with the given value
     * @param value the boolean value to match
     */
    public Filter(BooleanOperator operator, boolean value) {
        // TODO: implement this method
        this.boolOperator = operator;
        this.boolValue = value;
        this.isDoubleOperator = false;
        this.composedFilters = new ArrayList<>();
        this.composedFilters.add(this);
    }

    /**
     * Constructs a filter that compares a double field in events
     * with the given double value using the given DoubleOperator.
     * (X (DoubleOperator) value), where X is the event's value passed by satisfies or sift methods.
     * A DoubleOperator can be one of the following:
     *
     * DoubleOperator.EQUALS
     * DoubleOperator.GREATER_THAN
     * DoubleOperator.LESS_THAN
     * DoubleOperator.GREATER_THAN_OR_EQUALS
     * DoubleOperator.LESS_THAN_OR_EQUALS
     *
     * For non-double (boolean) value events, the satisfies method should return false.
     *
     * @param field the field to match (event "value" or event "timestamp")
     * @param operator the DoubleOperator to use to compare the event value with the given value
     * @param value the double value to match
     *
     * @throws IllegalArgumentException if the given field is not "value" or "timestamp"
     */
    public Filter(String field, DoubleOperator operator, double value) {
        this.field = field;
        this.doubleOperator = operator;
        this.doubleValue = value;
        this.isDoubleOperator = true;
        this.composedFilters = new ArrayList<>();
        this.composedFilters.add(this);
    }

    /**
     * A filter can be composed of other filters.
     * in this case, the filter should satisfy all the filters in the list.
     * Constructs a complex filter composed of other filters.
     *
     * @param filters the list of filters to use in the composition
     */
    public Filter(List<Filter> filters) {
        this.composedFilters = filters;
    }

    /**
     * Evaluates the double passed in according to the DoubleOperator of the class
     * @param evaluatedDouble the double to be evaluated
     * @return true if the DoubleOperator is true, false otherwise
     */
    private boolean evaluateFilterDouble(double evaluatedDouble) {
        return switch (this.doubleOperator) {
            case EQUALS -> evaluatedDouble == this.doubleValue;
            case GREATER_THAN -> evaluatedDouble > this.doubleValue;
            case LESS_THAN -> evaluatedDouble < this.doubleValue;
            case GREATER_THAN_OR_EQUALS -> evaluatedDouble >= this.doubleValue;
            case LESS_THAN_OR_EQUALS -> evaluatedDouble <= this.doubleValue;
        };
    }
    /**
     * Evaluates the bool passed in according to the BooleanOperator of the class
     * @param evaluatedBool the boolean to be evaluated
     * @return true if the BooleanOperator is true, false otherwise
     */
    private boolean evaluateFilterBool(boolean evaluatedBool) {
        return switch (this.boolOperator) {
            case EQUALS -> evaluatedBool == this.boolValue;
            case NOT_EQUALS -> evaluatedBool != this.boolValue;
        };
    }

    /**
     * Returns true if the given event satisfies the filter criteria.
     *
     * @param event the event to check
     * @return true if the event satisfies the filter criteria, false otherwise
     */
    public boolean satisfies(Event event) {
        // Check if the event has a double value
        for (Filter filter : composedFilters) {
            if (filter.isDoubleOperator) {
                if (filter.field.equalsIgnoreCase("timestamp")) {
                    if (!filter.evaluateFilterDouble(event.getTimeStamp())) {
                        return false;
                    }
                } else if (filter.field.equalsIgnoreCase("value")) {
                    if(!filter.evaluateFilterDouble(event.getValueDouble())) {
                        return false;
                    }
                }
            } else {
                //there is no field inputted, so it is a boolean operator that we are comparing
                if (!evaluateFilterBool(event.getValueBoolean())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns true if the given list of events satisfies the filter criteria.
     *
     * @param events the list of events to check
     * @return true if every event in the list satisfies the filter criteria, false otherwise
     */
    public boolean satisfies(List<Event> events) {
        for (Event event : events) {
            if (!satisfies(event)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a new event if it satisfies the filter criteria.
     * If the given event does not satisfy the filter criteria, then this method should return null.
     *
     * @param event the event to sift
     * @return a new event if it satisfies the filter criteria, null otherwise
     */
    public Event sift(Event event) {
        if (satisfies(event)) {
            return event;
        }
        return null;
    }

    /**
     * Returns a list of events that contains only the events in the given list that satisfy the filter criteria.
     * If no events in the given list satisfy the filter criteria, then this method should return an empty list.
     *
     * @param events the list of events to sift
     * @return a list of events that contains only the events in the given list that satisfy the filter criteria
     *        or an empty list if no events in the given list satisfy the filter criteria
     */
    public List<Event> sift(List<Event> events) {
        List <Event> filteredEvents  = new ArrayList<>();
        for (Event event : events) {
            if (satisfies(event)) {
                filteredEvents.add(event);
            }
        }
        return filteredEvents;
    }

    /**
     * Converts the filter into a formatted string
     * @return the filter in String format
     */
    @Override
    public String toString() {
        String result = "";

        // bool filter
        if (boolOperator != null) {
            result = "Boolean Filter: "
                    + "Operator=" + boolOperator
                    + ", Value=" + boolValue;
        }

        //double filter
        else if (doubleOperator != null) {
            result = "Double Filter: "
                    + "Field=" + field
                    + ", Operator=" + doubleOperator
                    + ", Value=" + doubleValue;
        }

        //composite filter
        else if (composedFilters != null && !composedFilters.isEmpty()) {
            result = "Composite Filter: [";
            for (Filter filter : composedFilters) {
                result += filter.toString() + ", ";
            }
            // deleting the last ", "
            result = result.substring(0, result.length() - 2);
            result += "]";
        }

        // no filter is defined
        else {
            result = "Error: No filter defined";
        }

        return result;
    }
}
