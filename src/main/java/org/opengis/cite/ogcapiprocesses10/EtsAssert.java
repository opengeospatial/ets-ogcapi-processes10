package org.opengis.cite.ogcapiprocesses10;

/**
 * Provides a set of custom assertion methods.
 *
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class EtsAssert {

	/**
	 * <p>
	 * assertTrue.
	 * </p>
	 * @param valueToAssert the boolean to assert to be <code>true</code>
	 * @param failureMsg the message to throw in case of a failure, should not be
	 * <code>null</code>
	 */
	public static void assertTrue(boolean valueToAssert, String failureMsg) {
		if (!valueToAssert)
			throw new AssertionError(failureMsg);
	}

	/**
	 * <p>
	 * assertFalse.
	 * </p>
	 * @param valueToAssert the boolean to assert to be <code>false</code>
	 * @param failureMsg the message to throw in case of a failure, should not be
	 * <code>null</code>
	 */
	public static void assertFalse(boolean valueToAssert, String failureMsg) {
		if (valueToAssert)
			throw new AssertionError(failureMsg);
	}

}
