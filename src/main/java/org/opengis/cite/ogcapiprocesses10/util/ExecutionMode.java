/**
 *
 */
package org.opengis.cite.ogcapiprocesses10.util;

/**
 * <p>
 * ExecutionMode class.
 * </p>
 *
 * @author bpr
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

	/**
	 * <p>
	 * Getter for the field <code>stringRepresentation</code>.
	 * </p>
	 * @return a {@link java.lang.String} object
	 */
	private final String stringRepresentation;

	public final String getStringRepresentation() {
		return stringRepresentation;
	}

	ExecutionMode(String stringRepresentation) {
		this.stringRepresentation = stringRepresentation;
	}

	/**
	 * <p>
	 * fromString.
	 * </p>
	 * @param type a {@link java.lang.String} object
	 * @return a {@link org.opengis.cite.ogcapiprocesses10.util.ExecutionMode} object
	 */
	public static ExecutionMode fromString(String type) {
		for (ExecutionMode c : ExecutionMode.values()) {
			if (c.getStringRepresentation().equalsIgnoreCase(type)) {
				return c;
			}
		}
		throw new IllegalArgumentException(type);
	}

	/**
	 * <p>
	 * toString.
	 * </p>
	 * @param type a {@link org.opengis.cite.ogcapiprocesses10.util.ExecutionMode} object
	 * @return a {@link java.lang.String} object
	 */
	public static String toString(ExecutionMode type) {
		return type.stringRepresentation;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return toString(this);
	}

}
