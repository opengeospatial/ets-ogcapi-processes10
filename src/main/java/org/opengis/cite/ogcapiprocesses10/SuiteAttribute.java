package org.opengis.cite.ogcapiprocesses10;

import java.io.File;
import java.net.URI;
import java.util.List;

import com.reprezen.kaizen.oasparser.model3.OpenApi3;

import jakarta.ws.rs.client.Client;

/**
 * An enumerated type defining ISuite attributes that may be set to constitute a shared
 * test fixture.
 */
@SuppressWarnings("rawtypes")
public enum SuiteAttribute {

	/**
	 * A client component for interacting with HTTP endpoints.
	 */
	CLIENT("httpClient", Client.class),

	/**
	 * The root URL.
	 */
	IUT("instanceUnderTest", URI.class),

	/**
	 * A File containing the test subject or a description of it.
	 */
	TEST_SUBJ_FILE("testSubjectFile", File.class),

	/**
	 * The number of collections to test.
	 */
	NO_OF_COLLECTIONS("noOfCollections", Integer.class),

	/**
	 * The id of the echo process.
	 */
	ECHO_PROCESS_ID("echoProcessId", String.class),

	/**
	 * Boolean indicating whether all processes should be tested against the OGC Process
	 * Description Conformance Class.
	 */
	TEST_ALL_PROCESSES("testAllProcesses", Boolean.class),

	/**
	 * Number of processes that should be tested against the OGC Process Description
	 * Conformance Class.
	 */
	PROCESS_TEST_LIMIT("processTestLimit", Integer.class),

	/**
	 * Parsed OpenApi3 document resource /api; Added during execution.
	 */
	API_MODEL("apiModel", OpenApi3.class),

	/**
	 * Use local OpenAPI schema included in ETS.
	 */
	USE_LOCAL_SCHEMA("useLocalSchema", Boolean.class),

	/**
	 * Requirement classes parsed from /conformance; Added during execution.
	 */
	REQUIREMENTCLASSES("requirementclasses", List.class);

	private final Class attrType;

	private final String attrName;

	SuiteAttribute(String attrName, Class attrType) {
		this.attrName = attrName;
		this.attrType = attrType;
	}

	public Class getType() {
		return attrType;
	}

	public String getName() {
		return attrName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(attrName);
		sb.append('(').append(attrType.getName()).append(')');
		return sb.toString();
	}

}
