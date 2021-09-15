/**
 * 
 */
package org.opengis.cite.ogcapiprocesses10.util;

/**
 * @author bpr
 *
 */
public enum ExecutionMode {
	
	    /**
	     * Synchronous execution mode.
	     */
	    SYNC("sync"),
	    /**
	     * Asynchronous execution mode.
	     */
	    ASYNC("async"),
	    /**
	     * Auto execution mode.
	     */
	    AUTO("auto");

	    private final String stringRepresentation;

	    public final String getStringRepresentation() {
	        return stringRepresentation;
	    }

	    ExecutionMode(String stringRepresentation) {
	        this.stringRepresentation = stringRepresentation;
	    }

	    public static ExecutionMode fromString(String type) {
	        for (ExecutionMode c : ExecutionMode.values()) {
	            if (c.getStringRepresentation().equalsIgnoreCase(type)) {
	                return c;
	            }
	        }
	        throw new IllegalArgumentException(type);
	    }

	    public static String toString(ExecutionMode type) {
	        return type.stringRepresentation;
	    }

	    @Override
	    public String toString() {
	        return toString(this);
	    }

}
