package org.opengis.cite.ogcapiprocesses10.jobs;

import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openapi4j.operation.validator.model.Response;
import org.openapi4j.operation.validator.model.impl.Body;
import org.openapi4j.operation.validator.model.impl.DefaultResponse;
import org.openapi4j.operation.validator.validation.OperationValidator;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.openapi4j.parser.model.v3.Path;
import org.openapi4j.schema.validator.ValidationData;
import org.opengis.cite.ogcapiprocesses10.CommonFixture;
import org.opengis.cite.ogcapiprocesses10.SuiteAttribute;
import org.opengis.cite.ogcapiprocesses10.util.TestSuiteLogger;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 *
 * A.2.6. JobList {root}/jobs
 *
 * @author <a href="mailto:b.pross@52north.org">Benjamin Pross</a>
 */
public class Jobs extends CommonFixture {

	private static final String OPERATION_ID_GET_JOBS = "getJobs";

	private static final String OPERATION_ID_GET_STATUS = "getStatus";

	private static final String OPERATION_ID_GET_RESULT = "getResult";

	private static final String OPERATION_ID_EXECUTE = "execute";

	private static final String JOB_CONTROL_OPTIONS_KEY = "jobControlOptions";

	private static final Object JOB_CONTROL_OPTIONS_SYNC = "sync-execute";

	private static final Object JOB_CONTROL_OPTIONS_ASYNC = "async-execute";

	private static final String SCHEMA_KEY = "schema";

	private static final String RESPONSE_KEY = "response";

	private static final String RESPONSE_VALUE_DOCUMENT = "document";

	private static final String RESPONSE_VALUE_RAW = "raw";

	private static final String TEST_STRING_INPUT = "teststring";

	private static final Object TYPE_DEFINITION_OBJECT = "object";

	private static final String EXCEPTION_SCHEMA_URL = "https://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/exception.yaml";

	private static final String STATUS_SCHEMA_URL = "https://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/statusInfo.yaml";

	private static final String ASYNC_MODE_NOT_SUPPORTED_MESSAGE = "This test is skipped because the server has not declared support for asynchronous execution mode.";

	private static final String GEOTIFF_URL = "https://raw.githubusercontent.com/opengeospatial/ets-ogcapi-processes10/master/src/main/resources/org/opengis/cite/testdata/testgeotiff.tiff";

	private static final Object TYPE_DEFINITION_ARRAY = "array";

	private static final CharSequence ISSUE_54_MESSAGE_TEXT = "More than 1 schema is valid.";

	private int attempts = 0;

	private static final int MAX_ATTEMPTS = 4;

	private static final int ASYNC_LOOP_WAITING_PERIOD = 5000;

	private OpenApi3 openApi3;

	private String getJobsListPath = "/jobs";

	// private String getJobPath = "/jobs";

	private String getProcessListPath = "/processes";

	private OperationValidator getJobsValidator;

	private OperationValidator getStatusValidator;

	private OperationValidator getResultValidator;

	private OperationValidator executeValidator;

	private URL getJobsListURL;

	private URL getProcessesListURL;

	private URL getInvalidJobURL;

	private URL getInvalidJobResultURL;

	private String echoProcessId;

	private String echoProcessPath;

	private List<Input> inputs;

	private List<Output> outputs;

	private ObjectMapper objectMapper = new ObjectMapper();

	// private CloseableHttpClient client;

	private String executeEndpoint;

	private HttpClientBuilder clientBuilder;

	private SupportedExecutionModes supportedExecutionModes;

	enum SupportedExecutionModes {

		/**
		 * Only synchronous execution mode is supported.
		 */
		ONLY_SYNC,
		/**
		 * Only asynchronous execution mode is supported.
		 */
		ONLY_ASYNC,
		/**
		 * Either execution mode is supported.
		 */
		EITHER;

	}

	@BeforeClass
	public void setup(ITestContext testContext) {
		inputs = new ArrayList<Input>();
		outputs = new ArrayList<Output>();
		String processListEndpointString = rootUri.toString() + getProcessListPath;
		String jobsListEndpointString = rootUri.toString() + getJobsListPath;
		try {
			echoProcessId = (String) testContext.getSuite().getAttribute(SuiteAttribute.ECHO_PROCESS_ID.getName());
			echoProcessPath = getProcessListPath + "/" + echoProcessId;
			executeEndpoint = rootUri + echoProcessPath + "/execution";
			parseEchoProcess();
			openApi3 = new OpenApi3Parser().parse(specURL, false);
			final Path path = openApi3.getPathItemByOperationId(OPERATION_ID_GET_JOBS);
			final Operation operation = openApi3.getOperationById(OPERATION_ID_GET_JOBS);
			getJobsValidator = new OperationValidator(openApi3, path, operation);
			final Path executePath = openApi3.getPathItemByOperationId(OPERATION_ID_EXECUTE);
			final Operation executeOperation = openApi3.getOperationById(OPERATION_ID_EXECUTE);
			executeValidator = new OperationValidator(openApi3, executePath, executeOperation);
			final Path getStatusPath = openApi3.getPathItemByOperationId(OPERATION_ID_GET_STATUS);
			final Operation getStatusOperation = openApi3.getOperationById(OPERATION_ID_GET_STATUS);
			getStatusValidator = new OperationValidator(openApi3, getStatusPath, getStatusOperation);
			final Path getResultPath = openApi3.getPathItemByOperationId(OPERATION_ID_GET_RESULT);
			final Operation getResultOperation = openApi3.getOperationById(OPERATION_ID_GET_RESULT);
			getResultValidator = new OperationValidator(openApi3, getResultPath, getResultOperation);
			getJobsListURL = new URL(jobsListEndpointString);
			getProcessesListURL = new URL(processListEndpointString);
			getInvalidJobURL = new URL(jobsListEndpointString + "/invalid-job-" + UUID.randomUUID());
			getInvalidJobResultURL = new URL(jobsListEndpointString + "/invalid-job-" + UUID.randomUUID() + "/results");
			clientBuilder = HttpClientBuilder.create();
		}
		catch (Exception e) {
			Assert.fail("Could set up endpoint: " + processListEndpointString + ". Exception: " + e);
		}
	}

	private boolean echoProcessSupportsAsync() {
		boolean supportsAsync = false;
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpUriRequest request = new HttpGet(rootUri + echoProcessPath);
			request.setHeader("Accept", "application/json");
			HttpResponse httpResponse = client.execute(request);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			JsonNode inputsNode = responseNode.get("jobControlOptions");

			if (inputsNode instanceof ArrayNode) {
				ArrayNode inputsArrayNode = (ArrayNode) inputsNode;

				for (int i = 0; i < inputsArrayNode.size(); i++) {
					if (inputsArrayNode.get(i).asText().equals("async-execute"))
						supportsAsync = true;
				}

			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return supportsAsync;
	}

	private void parseEchoProcess() {

		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpUriRequest request = new HttpGet(rootUri + echoProcessPath);
			request.setHeader("Accept", "application/json");
			HttpResponse httpResponse = client.execute(request);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			JsonNode inputsNode = responseNode.get("inputs");
			if (inputsNode instanceof ArrayNode) {
				ArrayNode inputsArrayNode = (ArrayNode) inputsNode;
			}
			else {
				Iterator<String> inputNames = inputsNode.fieldNames();
				while (inputNames.hasNext()) {
					String id = (String) inputNames.next();
					JsonNode inputNode = inputsNode.get(id);
					JsonNode schemaNode = inputNode.get(SCHEMA_KEY);
					Input input = createInput(schemaNode, id);
					inputs.add(input);
				}
			}
			JsonNode outputsNode = responseNode.get("outputs");
			if (outputsNode instanceof ArrayNode) {
				ArrayNode outputsArrayNode = (ArrayNode) outputsNode;
			}
			else {
				Iterator<String> outputNames = outputsNode.fieldNames();
				while (outputNames.hasNext()) {
					String id = (String) outputNames.next();
					JsonNode outputNode = outputsNode.get(id);
					JsonNode schemaNode = outputNode.get(SCHEMA_KEY);
					Output output = createOutput(schemaNode, id);
					outputs.add(output);
				}
			}
			JsonNode jobControlOptionsNode = responseNode.get(JOB_CONTROL_OPTIONS_KEY);
			if (jobControlOptionsNode != null && !jobControlOptionsNode.isMissingNode()) {
				if (jobControlOptionsNode instanceof ArrayNode) {
					ArrayNode jobControlOptionsArrayNode = (ArrayNode) jobControlOptionsNode;
					boolean syncSupported = false;
					boolean aSyncSupported = false;
					for (int i = 0; i < jobControlOptionsArrayNode.size(); i++) {
						JsonNode jobControlOptionsArrayChildNode = jobControlOptionsArrayNode.get(i);
						if (jobControlOptionsArrayChildNode.asText().equals(JOB_CONTROL_OPTIONS_SYNC)) {
							syncSupported = true;
						}
						else if (jobControlOptionsArrayChildNode.asText().equals(JOB_CONTROL_OPTIONS_ASYNC)) {
							aSyncSupported = true;
						}
					}
					if (syncSupported && !aSyncSupported) {
						supportedExecutionModes = SupportedExecutionModes.ONLY_SYNC;
					}
					if (aSyncSupported && !syncSupported) {
						supportedExecutionModes = SupportedExecutionModes.ONLY_ASYNC;
					}
					if (syncSupported && aSyncSupported) {
						supportedExecutionModes = SupportedExecutionModes.EITHER;
					}
				}
			}
		}
		catch (IOException e) {
			Assert.fail("Could not parse echo process.");
		}

	}

	private ObjectNode createExecuteJsonNode(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		// executeNode.set("id", new TextNode(echoProcessId));
		for (Input input : inputs) {
			addInput(input, inputsNode);
		}
		for (Output output : outputs) {
			addOutput(output, outputsNode);
		}
		executeNode.set("inputs", inputsNode);
		if (inputsNode.isEmpty()) {
			throw new AssertionError("No supported input found. Only plain string input is supported.");
		}
		executeNode.set("outputs", outputsNode);
		return executeNode;
	}

	private void addOutput(Output output, ObjectNode outputsNode) {
		ObjectNode outputNode = objectMapper.createObjectNode();
		outputNode.set("transmissionMode", new TextNode("value"));
		outputsNode.set(output.getId(), outputNode);
	}

	private void addInput(Input input, ObjectNode inputsNode) {
		List<Type> types = input.getTypes();
		ObjectNode inputNode = objectMapper.createObjectNode();

		for (Type type : types) {
			if (type.getTypeDefinition().equals("string")) {
				if (input.getFormat() == null && type.getContentMediaType() == null) {
					inputsNode.set(input.getId(), new TextNode(TEST_STRING_INPUT));
				}
			}
			else if (input.isBbox()) {
				inputNode.set("crs", new TextNode("urn:ogc:def:crs:EPSG:6.6:4326"));
				ArrayNode arrayNode = objectMapper.createArrayNode();
				arrayNode.add(345345345);
				arrayNode.add(345345345);
				arrayNode.add(345345345);
				arrayNode.add(345345345);
				inputNode.set("bbox", arrayNode);
				inputsNode.set(input.getId(), inputNode);
			}
			else if (type.getTypeDefinition().equals(TYPE_DEFINITION_OBJECT)) {
				addObjectInput(input, inputsNode);
			}
		}
	}

	/**
	 * <pre>
	* Abstract Test 17: /conf/core/job-creation-auto-execution-mode
	* Test Purpose: Validate that the server correctly handles the execution mode for a process.
	* Requirement: /req/core/job-creation-op
	* Test Method:
	* 1.  Setting the HTTP `Prefer` header to include the `respond-sync` preference, construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request
	* 2.  For processes that are supposed to execute asynchronously according to the req_core_job-creation-auto-execution-mode,/req/core/job-creation-auto-execution-mode requirement, verify the successful execution according to the ats_core_job-creation-success-async,/conf/core/job-creation-success-async test
	* 3.  For processes that are supposed to execute synchronously according to the req_core_job-creation-auto-execution-mode,/req/core/job-creation-auto-execution-mode requirement, verify the successful execution according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* 4.  For processes that may execute either synchronously or asynchronously according to the req_core_job-creation-auto-execution-mode,/req/core/job-creation-auto-execution-mode requirement, verify that successful execution according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-op ")
	public void testJobCreationAutoExecutionMode() {

		if (echoProcessSupportsAsync()) {
			// create async job
			JsonNode executeNode = createExecuteJsonNode(echoProcessId);
			try {
				// send execute request with prefer header
				HttpResponse httpResponse = sendPostRequestASync(executeNode);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				if (supportedExecutionModes.equals(SupportedExecutionModes.ONLY_SYNC)) {
					Assert.assertTrue(statusCode == 200, "Got unexpected status code: " + statusCode);
				}
				if (supportedExecutionModes.equals(SupportedExecutionModes.ONLY_ASYNC)) {
					Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);
				}
				if (supportedExecutionModes.equals(SupportedExecutionModes.EITHER)) {
					Assert.assertTrue(statusCode == 201 || statusCode == 200,
							"Got unexpected status code: " + statusCode);
				}
			}
			catch (Exception e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}
		else {
			throw new SkipException(Jobs.ASYNC_MODE_NOT_SUPPORTED_MESSAGE
					+ " Also note that the specification does not mandate that servers create a job as a result of executing a process synchronously (See Clause 7.11.4 of OGC 18-062r2)");
		}
	}

	/**
	 * <pre>
	* Abstract Test 18: /conf/core/job-creation-default-execution-mode
	* Test Purpose: Validate that the server correctly handles the default execution mode for a process.
	* Requirement: /req/core/job-creation-op
	* Test Method:
	* 1.  Without setting the HTTP `Prefer` header, construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request
	* 2.  For processes that are supposed to execute asynchronously according to the req_core_job-creation-default-execution-mode,/req/core/job-creation-default-execution-mode requirement, verify the successful execution according to the ats_core_job-creation-success-async,/conf/core/job-creation-success-async test
	* 3.  For processes that are supposed to execute synchronously according to the req_core_job-creation-auto-execution-mode,/req/core/job-creation-auto-execution-mode requirement, verify the successful synchronous execution according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-op ")
	public void testJobCreationDefaultExecutionMode() {
		// create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		try {
			// send execute request without prefer header
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if (supportedExecutionModes.equals(SupportedExecutionModes.ONLY_SYNC)
					|| supportedExecutionModes.equals(SupportedExecutionModes.EITHER)) {
				Assert.assertTrue(statusCode == 200, "Got unexpected status code: " + statusCode);
			}
			if (supportedExecutionModes.equals(SupportedExecutionModes.ONLY_ASYNC)) {
				Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
			}
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 27: /conf/core/job-creation-default-outputs
	* Test Purpose: Validate that the server correctly handles the case where no `outputs` parameter is specified on an execute request.
	* Requirement: /req/core/job-creation-op
	* Test Method:
	* 1.  For each process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request taking care to omit the `outputs` parameter
	* 2.  Verify that each processes executed successfully according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* 3.  Verify that each process includes all the outputs, as defined in the sc_process_description,process description, in the response
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-op ")
	public void testJobCreationDefaultOutputs() {
		// create job
		JsonNode executeNode = createExecuteJsonNodeNoOutputs(echoProcessId);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	private JsonNode createExecuteJsonNodeNoOutputs(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		// executeNode.set("id", new TextNode(echoProcessId));
		for (Input input : inputs) {
			addInput(input, inputsNode);
		}
		executeNode.set("inputs", inputsNode);
		return executeNode;
	}

	/**
	 * <pre>
	* Abstract Test 23: /conf/core/job-creation-input-array
	* Test Purpose: Verify that the server correctly recognizes the encoding of parameter values for input parameters with a maximum cardinality greater than one.
	* Requirement: /req/core/job-creation-input-array
	* Test Method:
	* 1.  Get a description of each process offered by the server using test /conf/core/process.
	* 2.  Inspect the description of each process and identify the list of processes that have inputs with a maximum cardinality greater that one.
	* 3.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request taking care to encode the inputs with maximum cardinality  1 according to the requirement req_core_job-creation-input-array,/req/core/job-creation-input-array
	* 4.  Verify that each process executes successfully according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-input-array ")
	public void testJobCreationInputArray() {
		// create job
		JsonNode executeNode = createExecuteJsonNodeWithInputArray(echoProcessId);
		TestSuiteLogger.log(Level.INFO, executeNode.toString());
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 27: /conf/core/job-creation-input-inline-bbox
	* Test Purpose: Validate that inputs with a bounding box schema encoded in-line in an execute request are correctly processed.
	* Requirement: /req/core/job-creation-input-inline-bbox
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request taking care to encode values for the identified bounding box inputs in-line in the execute request
	* 2.  Verify that each process executes successfully according to the ats_job-creation-success,relevant requirement based on the combination of execute parameters
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-input-inline-bbox ")
	public void testJobCreationInputInlineBbox() {
		// create job
		JsonNode executeNode = createExecuteJsonNodeWithBBox(echoProcessId);
		TestSuiteLogger.log(Level.INFO, executeNode.toString());
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 26: /conf/core/job-creation-input-inline-binary
	* Test Purpose: Validate that binary input values encoded as base-64 string in-line in an execute request are correctly processes.
	* Requirement: /req/core/job-creation-input-binary
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request taking care to encode binary input values in-line in the execute request according to requirement req_core_job-creation-input-inline-binary,/req/core/job-creation-input-inline-binary
	* 2.  Verify that each process executes successfully according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-input-binary ")
	public void testJobCreationInputInlineBinary() {
		// create job
		JsonNode executeNode = createExecuteJsonNodeWithBinaryInput(echoProcessId);
		TestSuiteLogger.log(Level.INFO, executeNode.toString());
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 25: /conf/core/job-creation-input-inline-mixed
	* Test Purpose: Validate that inputs of mixed content encoded in-line in an execute request are correctly processed.
	* Requirement: /req/core/job-creation-input-inline-mixed
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request taking care to encode the identified mix-content inputs in-line in the execute request according to requirement req_core_job-creation-input-inline-mixed,/req/core/job-creation-input-inline-mixed
	* 2.  Verify that each process executes successfully according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-input-inline-mixed ")
	public void testJobCreationInputInlineMixed() {
		// create job
		JsonNode executeNode = createExecuteJsonNodeWithMixedInput(echoProcessId);
		TestSuiteLogger.log(Level.INFO, executeNode.toString());
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 23: /conf/core/job-creation-input-inline-object
	* Test Purpose: Validate that inputs with a complex object schema encoded in-line in an execute request are correctly processed.
	* Requirement: /req/core/job-creation-input-inline-object
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request taking care to encode the identified object inputs in-line in the execute request according to requirement req_core_job-creation-input-inline-object,/req/core/job-creation-input-inline-object
	* 2.  Verify that each process executes successfully according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* |===
	*
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-input-inline-object ")
	public void testJobCreationInputInlineObject() {
		// create job
		JsonNode executeNode = createExecuteJsonNodeWithObject(echoProcessId);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	private JsonNode createExecuteJsonNodeWithBBox(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		boolean foundBBoxInput = false;
		for (Input input : inputs) {
			boolean inputIsBBox = false;
			if (foundBBoxInput) {
				addInput(input, inputsNode);
				continue;
			}
			if (input.isBbox()) {
				addBBoxInput(input, inputsNode);
				foundBBoxInput = true;
				inputIsBBox = true;
				continue;
			}
			if (!inputIsBBox) {
				addInput(input, inputsNode);
			}
		}
		for (Output output : outputs) {
			if (output.isBbox()) {
				addOutput(output, outputsNode);
			}
		}
		if (!foundBBoxInput) {
			throw new SkipException("No input of type bounding box found.");
		}
		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		return executeNode;
	}

	private void addBBoxInput(Input input, ObjectNode inputsNode) {
		ObjectNode inputObjectNode = objectMapper.createObjectNode();
		ArrayNode bboxArrayNode = objectMapper.createArrayNode();
		bboxArrayNode.add(51.9);
		bboxArrayNode.add(7);
		bboxArrayNode.add(52);
		bboxArrayNode.add(7.1);
		inputObjectNode.set("bbox", bboxArrayNode);
		inputObjectNode.set("crs", new TextNode("http://www.opengis.net/def/crs/OGC/1.3/CRS84"));
		inputsNode.set(input.getId(), inputObjectNode);
	}

	private JsonNode createExecuteJsonNodeWithObject(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		// executeNode.set("id", new TextNode(echoProcessId));
		boolean foundObjectInput = false;
		for (Input input : inputs) {
			boolean inputIsObject = false;
			List<Type> types = input.getTypes();
			if (foundObjectInput) {
				addInput(input, inputsNode);
				continue;
			}
			for (Type type : types) {
				if (type.getTypeDefinition().equals(TYPE_DEFINITION_OBJECT)) {
					addObjectInput(input, inputsNode);
					foundObjectInput = true;
					inputIsObject = true;
					continue;
				}
			}
			if (!inputIsObject) {
				addInput(input, inputsNode);
			}
		}
		for (Output output : outputs) {
			addOutput(output, outputsNode);
		}
		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		return executeNode;
	}

	private JsonNode createExecuteJsonNodeWithInputArray(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		boolean foundArrayInput = false;
		for (Input input : inputs) {
			boolean inputIsArray = false;
			if (foundArrayInput) {
				addInput(input, inputsNode);
				continue;
			}
			List<Type> types = input.getTypes();
			for (Type type : types) {
				if (type.getTypeDefinition().equals(TYPE_DEFINITION_ARRAY)) {
					addArrayInput(input, inputsNode);
					foundArrayInput = true;
					inputIsArray = true;
					continue;
				}
			}
			if (!inputIsArray) {
				addInput(input, inputsNode);
			}
		}
		boolean foundArrayOutput = false;
		for (Output output : outputs) {
			if (foundArrayOutput) {
				break;
			}
			List<Type> types = output.getTypes();
			if (types == null) {
				continue;
			}
			for (Type type : types) {
				if (type.getTypeDefinition().equals(TYPE_DEFINITION_ARRAY)) {
					addOutput(output, outputsNode);
					foundArrayOutput = true;
					break;
				}
			}
		}
		if (!foundArrayInput) {
			throw new SkipException("No input of type array found.");
		}
		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		return executeNode;
	}

	private void addArrayInput(Input input, ObjectNode inputsNode) {
		Optional<Type> firstItemType = input.getTypes().stream().filter(p -> p != null)
				.filter(p -> !p.getTypeDefinition().equals("array")).findFirst();
		try {
			String itemType = firstItemType.get().getTypeDefinition();
			ArrayNode arrayNode = objectMapper.createArrayNode();
			switch (itemType) {
			case "integer":
				arrayNode.add(1);
				arrayNode.add(2);
				arrayNode.add(3);
				break;
			case "double":
				arrayNode.add(1.1);
				arrayNode.add(2.2);
				arrayNode.add(3.3);
				break;
			case "float":
				arrayNode.add(1.1f);
				arrayNode.add(2.2f);
				arrayNode.add(3.3f);
				break;
			case "string":
				arrayNode.add("test1");
				arrayNode.add("test2");
				arrayNode.add("test3");
				break;
			default:
				break;
			}
			inputsNode.set(input.getId(), arrayNode);
		}
		catch (Exception e) {
			throw new NoSuchElementException(
					String.format("No item type for array input \"%s\" specified.", input.getId()));
		}
	}

	private void addBinaryInput(Input input, ObjectNode inputsNode) {
		List<Type> types = input.getTypes();
		ObjectNode inputNode = objectMapper.createObjectNode();
		byte[] inputFileAsByteArray = null;
		try {
			URL fileUrl = getClass().getClassLoader().getResource("org/opengis/cite/testdata/testgeotiff.tiff");
			File inputFile = new File(fileUrl.getFile());
			InputStream in = new FileInputStream(inputFile);
			inputFileAsByteArray = new byte[(int) inputFile.length()];
			in.read(inputFileAsByteArray);
			in.close();
		}
		catch (IOException e) {
			return;
		}
		String base64EncodedString = Base64.encodeBase64String(inputFileAsByteArray);

		inputNode.set("value", new TextNode(base64EncodedString));

		ObjectNode formatNode = objectMapper.createObjectNode();

		for (Type type : types) {
			if (type.getTypeDefinition().equals("string")) {
				if (type.getContentMediaType() != null && type.getContentMediaType().contains("tiff"))
					formatNode.set("mediaType", new TextNode(type.getContentMediaType()));
			}
		}
		formatNode.set("encoding", new TextNode("base64"));

		inputNode.set("format", formatNode);

		inputsNode.set(input.getId(), inputNode);
	}

	private JsonNode createExecuteJsonNodeWithBinaryInput(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		boolean foundBinaryInput = false;
		for (Input input : inputs) {
			boolean inputIsBinary = false;
			if (foundBinaryInput) {
				addInput(input, inputsNode);
				continue;
			}
			List<Type> types = input.getTypes();
			for (Type type : types) {
				if (type.isBinary()) {
					addBinaryInput(input, inputsNode);
					foundBinaryInput = true;
					inputIsBinary = true;
					continue;
				}
			}
			if (!inputIsBinary) {
				addInput(input, inputsNode);
			}
		}
		for (Output output : outputs) {
			addOutput(output, outputsNode);
		}
		if (inputsNode.isEmpty()) {
			throw new SkipException("No input of type binary found.");
		}
		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		return executeNode;
	}

	private JsonNode createExecuteJsonNodeWithMixedInput(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		// executeNode.set("id", new TextNode(echoProcessId));
		boolean foundObjectInput = false;
		for (Input input : inputs) {
			boolean inputIsObject = false;
			List<Type> types = input.getTypes();
			if (foundObjectInput) {
				addInput(input, inputsNode);
				continue;
			}
			for (Type type : types) {
				if (type.getTypeDefinition().equals(TYPE_DEFINITION_OBJECT)) {
					addObjectInput(input, inputsNode);
					foundObjectInput = true;
					inputIsObject = true;
					continue;
				}
			}
			if (!inputIsObject) {
				addInput(input, inputsNode);
			}
		}
		for (Output output : outputs) {
			addOutput(output, outputsNode);
		}
		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		return executeNode;
	}

	private JsonNode createExecuteJsonNodeRawMixedMulti(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		// executeNode.set("id", new TextNode(echoProcessId));
		boolean foundObjectInput = false;
		for (Input input : inputs) {
			boolean inputIsObject = false;
			List<Type> types = input.getTypes();
			if (foundObjectInput) {
				addInput(input, inputsNode);
				continue;
			}
			for (Type type : types) {
				if (type.getTypeDefinition().equals(TYPE_DEFINITION_OBJECT)) {
					addObjectInput(input, inputsNode);
					foundObjectInput = true;
					inputIsObject = true;
					continue;
				}
			}
			if (!inputIsObject) {
				addInput(input, inputsNode);
			}
		}
		for (Output output : outputs) {
			addOutput(output, outputsNode);
		}
		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		return executeNode;
	}

	private JsonNode createExecuteJsonNodeRawRef(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		// executeNode.set("id", new TextNode(echoProcessId));
		boolean foundObjectInput = false;
		for (Input input : inputs) {
			boolean inputIsObject = false;
			List<Type> types = input.getTypes();
			if (foundObjectInput) {
				addInput(input, inputsNode);
				continue;
			}
			for (Type type : types) {
				if (type.getTypeDefinition().equals(TYPE_DEFINITION_OBJECT)) {
					addObjectInput(input, inputsNode);
					foundObjectInput = true;
					inputIsObject = true;
					continue;
				}
			}
			if (!inputIsObject) {
				addInput(input, inputsNode);
			}
		}
		for (Output output : outputs) {
			addOutput(output, outputsNode);
		}
		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		return executeNode;
	}

	private JsonNode createExecuteJsonNodeRawValueMulti(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		// executeNode.set("id", new TextNode(echoProcessId));
		boolean foundObjectInput = false;
		for (Input input : inputs) {
			boolean inputIsObject = false;
			List<Type> types = input.getTypes();
			if (foundObjectInput) {
				addInput(input, inputsNode);
				continue;
			}
			for (Type type : types) {
				if (type.getTypeDefinition().equals(TYPE_DEFINITION_OBJECT)) {
					addObjectInput(input, inputsNode);
					foundObjectInput = true;
					inputIsObject = true;
					continue;
				}
			}
			if (!inputIsObject) {
				addInput(input, inputsNode);
			}
		}
		for (Output output : outputs) {
			addOutput(output, outputsNode);
		}
		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		return executeNode;
	}

	private void addObjectInput(Input input, ObjectNode inputsNode) {
		ObjectNode inputObjectNode = objectMapper.createObjectNode();
		inputObjectNode.set("value", new TextNode(TEST_STRING_INPUT));
		inputsNode.set(input.getId(), inputObjectNode);
	}

	/**
	 * <pre>
	* Abstract Test 21: /conf/core/job-creation-input-inline
	* Test Purpose: Validate in-line process input values are validated against the corresponding schema from the process description.
	* Requirement: /req/core/job-creation-input-inline
	* Test Method:
	* 1.  Verify that each process executes successfully according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-input-inline ")
	public void testJobCreationInputInline() {
		// create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 22: /conf/core/job-creation-input-ref
	* Test Purpose: Validate that input values specified by reference in an execute request are correctly processed.
	* Requirement: /req/core/job-creation-input-ref
	* Test Method:
	* 1.  Verify that each process executes successfully according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-input-ref ")
	public void testJobCreationInputRef() {
		// create job

		JsonNode executeNode = createExecuteJsonNodeWithHref(echoProcessId);

		ValidationData<Void> data = new ValidationData<>();

		HttpResponse httpResponse = null;

		try {
			httpResponse = sendPostRequestSync(executeNode);
		}
		catch (IOException e2) {
			Assert.fail(e2.getLocalizedMessage());
		}
		Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
		String responseContentTypeValue = responseContentType.getValue();
		if (responseContentTypeValue == null) {
			throw new SkipException("Got empty response content type header.");
		}
		if (!responseContentTypeValue.startsWith("multipart/related")) {

			try {
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				switch (statusCode) {
				case 200:
					validateResponse(httpResponse, executeValidator, data);
					break;
				case 201:
					Header locationHeader = httpResponse.getFirstHeader("location");
					String locationString = locationHeader.getValue();
					httpResponse = sendGetRequest(locationString, "application/json");
					validateResponse(httpResponse, getStatusValidator, data);
					break;
				default:
					Assert.fail("Got unexpected status code: " + statusCode);
					break;
				}
			}
			catch (Exception e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}
		else if (responseContentTypeValue.startsWith("multipart/related")) {

			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();

			try {
				IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			}
			catch (Exception e1) {
				Assert.fail(e1.getLocalizedMessage());
			}

			String responsePayload = writer.toString();

			validateMultipartResponse(responsePayload, executeNode);

		}

	}

	private JsonNode createExecuteJsonNodeWithHref(String echoProcessId2) throws SkipException {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		boolean foundBinaryInput = false;
		for (Input input : inputs) {
			boolean inputIsBinary = false;
			if (foundBinaryInput) {
				addInput(input, inputsNode);
				continue;
			}
			List<Type> types = input.getTypes();
			for (Type type : types) {
				if (type.isBinary()) {
					addHrefInput(input, inputsNode);
					foundBinaryInput = true;
					inputIsBinary = true;
					continue;
				}
			}
			if (!inputIsBinary) {
				addInput(input, inputsNode);
			}
		}
		for (Output output : outputs) {
			addOutput(output, outputsNode);
		}
		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		return executeNode;
	}

	private void addHrefInput(Input input, ObjectNode inputsNode) {
		List<Type> types = input.getTypes();
		ObjectNode inputNode = objectMapper.createObjectNode();
		inputNode.set("href", new TextNode(GEOTIFF_URL));

		ObjectNode formatNode = objectMapper.createObjectNode();

		for (Type type : types) {
			if (type.getTypeDefinition().equals("string")) {
				if (type.getContentMediaType() != null && type.getContentMediaType().contains("tiff"))
					formatNode.set("mediaType", new TextNode(type.getContentMediaType()));
			}
		}

		inputNode.set("format", formatNode);

		inputsNode.set(input.getId(), inputNode);

	}

	/**
	 * <pre>
	* Abstract Test null: /conf/core/job-creation-input-validation
	* Test Purpose: Verify that the server correctly validates process input values according to the definition obtained from the sc_process_description,process description.
	* Requirement: /req/core/job-creation-input-validation
	* Test Method:
	* 1.  For each process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request taking care to encode the input values according to the schema from the definition of each input
	* 2.  Verify that each process executes successfully according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* 3.  For each process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request taking care to encode some of the input values in violation of the schema from the definition of the selected input
	* 4.  Verify that each process generates an exception report that identifies the improperly specified input value(s)
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-input-validation ")
	public void testJobCreationInputValidation() {
		// create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		ValidationData<Void> data = new ValidationData<>();

		HttpResponse httpResponse = null;

		try {
			httpResponse = sendPostRequestSync(executeNode);
		}
		catch (IOException e) {
			Assert.fail(e.getLocalizedMessage());
		}

		Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
		String responseContentTypeValue = responseContentType.getValue();
		if (responseContentTypeValue == null) {
			throw new SkipException("Got empty response content type header.");
		}

		if (responseContentTypeValue.startsWith("multipart/related")) {
			if (responseContentType.getValue().startsWith("multipart/related")) {
				String responsePayload = null;
				try {
					responsePayload = parseRawResponse(httpResponse);
					this.rspEntity = responsePayload;

				}
				catch (Exception ee) {
					Assert.fail(ee.getLocalizedMessage());
				}
				validateMultipartResponse(responsePayload, executeNode);
			}
			else {
				throw new SkipException(
						"The value of the Content-Type header of the response is " + responseContentType.getValue()
								+ " but the response payload states Content-Type: multipart/related");
			}
		}
		else {
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			try {
				switch (statusCode) {
				case 200:
					validateResponse(httpResponse, executeValidator, data);
					break;
				case 201:
					Header locationHeader = httpResponse.getFirstHeader("location");
					String locationString = locationHeader.getValue();
					httpResponse = sendGetRequest(locationString, "application/json");
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					httpResponse.getEntity().writeTo(baos);
					String responseContentString = baos.toString();
					baos.close();
					httpResponse.getEntity().getContent().close();
					ObjectMapper objectMapper = new ObjectMapper();
					JsonNode statusNode = objectMapper.readTree(responseContentString);
					loopOverStatus(statusNode);
					break;
				default:
					Assert.fail("Got unexpected status code: " + statusCode);
					break;
				}
			}
			catch (Exception e) {
				Assert.fail(e.getMessage());
			}
		}

		executeNode = createExecuteJsonNodeWithWrongInput(echoProcessId);
		data = new ValidationData<>();
		try {
			httpResponse = sendPostRequestSync(executeNode, false);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			this.rspEntity = writer.toString();
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			Body body = Body.from(responseNode);
			responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode == 201) {
				Header locationHeader = httpResponse.getFirstHeader("location");
				String locationString = locationHeader.getValue();
				httpResponse = sendGetRequest(locationString, "application/json");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				httpResponse.getEntity().writeTo(baos);
				String responseContentString = baos.toString();
				baos.close();
				httpResponse.getEntity().getContent().close();
				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode statusNode = objectMapper.readTree(responseContentString);
				assertTrue(loopOverStatusReturnsFailed(statusNode));
			}
			else {
				Assert.assertTrue(
						statusCode == HttpStatus.SC_BAD_REQUEST || statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR,
						"Was expecting a Status Code of 400 or 500 but found " + statusCode
								+ " (See Table 10 of OGC API - Processes - Part 1, OGC 18-062r2).");
				Assert.assertTrue(
						responseContentType.getValue().equals("application/json")
								|| responseContentType.getValue().startsWith("application/json;"),
						"Was expecting a Status Code of 400 or 500 but found " + statusCode
								+ " (See Table 10 of OGC API - Processes - Part 1, OGC 18-062r2).");
				assertTrue(
						validateResponseAgainstSchema(EXCEPTION_SCHEMA_URL,
								body.getContentAsNode(null, null, null).toString()),
						"Unable to validate the response document against: " + EXCEPTION_SCHEMA_URL);
			}
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}

	}

	private boolean checkForIssue54(ValidationData<Void> data) {
		try {
			// Validity of schemas needs to be checked by OAPIP SWG.
			// See https://github.com/opengeospatial/ogcapi-processes/issues/350
			// Validation results containing message "More than 1 schema is
			// valid." will not be regarded as errors until
			// ogcapi-processes/issues/350 is fixed/closed.
			if (data.results() != null && data.results().items().size() > 0) {
				if (data.results().items().get(0).message().contains(ISSUE_54_MESSAGE_TEXT)) {
					return true;
				}
			}
		}
		catch (Exception e) {
			return false;
		}
		return false;
	}

	private HttpResponse sendPostRequestSync(JsonNode executeNode, boolean checkForStatusCode) throws IOException {
		HttpResponse httpResponse = clientBuilder.build().execute(createPostRequest(executeNode));
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if (checkForStatusCode) {
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		}
		return httpResponse;
	}

	private HttpResponse sendPostRequestSync(JsonNode executeNode) throws IOException {
		return sendPostRequestSync(executeNode, false);
	}

	private HttpPost createPostRequest(JsonNode executeNode) {
		HttpPost request = new HttpPost(executeEndpoint);
		this.reqEntity = request;
		request.setHeader("Accept", "application/json");
		ContentType contentType = ContentType.APPLICATION_JSON;
		request.setEntity(new StringEntity(executeNode.toString(), contentType));
		return request;
	}

	private HttpResponse sendPostRequestASync(JsonNode executeNode) throws IOException {
		HttpPost request = new HttpPost(executeEndpoint);
		this.reqEntity = request;
		request.setHeader("Accept", "application/json");
		request.setHeader("Prefer", "respond-async");
		ContentType contentType = ContentType.APPLICATION_JSON;
		request.setEntity(new StringEntity(executeNode.toString(), contentType));
		HttpResponse httpResponse = clientBuilder.build().execute(request);
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		// https://github.com/opengeospatial/ets-ogcapi-processes10/issues/52
		// Allow also 200 responses if process supports both sync and async execution
		switch (supportedExecutionModes) {
		case EITHER:
			Assert.assertTrue(statusCode == 201 || statusCode == 200, "Got unexpected status code: " + statusCode);
			break;
		case ONLY_ASYNC:
			Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);
			break;
		case ONLY_SYNC:
			break;
		default:
			break;
		}
		return httpResponse;
	}

	private JsonNode createExecuteJsonNodeWithWrongInput(String echoProcessId) throws SkipException {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		// executeNode.set("id", new TextNode(echoProcessId));
		boolean foundTestableInput = false;
		for (Input input : inputs) {
			if (checkForFormat(input.getTypes()) != null) {
				foundTestableInput = true;
				addInputWithWrongFormat(input, inputsNode);
			}
			else
				addInput(input, inputsNode);
		}
		if (!foundTestableInput) {
			throw new SkipException("No input with specified format found, skipping test.");
		}
		for (Output output : outputs) {
			addOutput(output, outputsNode);
		}
		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		return executeNode;
	}

	private void addInputWithWrongFormat(Input input, ObjectNode inputsNode) {
		ObjectNode inputNode = objectMapper.createObjectNode();
		inputNode.set("type", new TextNode("wrong/type_" + UUID.randomUUID()));
		inputsNode.set(input.getId(), inputNode);
	}

	private Type checkForFormat(List<Type> types) {
		for (Type type : types) {
			if (type.getContentMediaType() != null) {
				return type;
			}
		}
		return null;
	}

	/**
	 * <pre>
	* Abstract Test 20: /conf/core/job-creation-inputs
	* Test Purpose: Validate that servers can accept input values both inline and by reference.
	* Requirement: /req/core/job-creation-inputs
	* Test Method:
	
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-inputs ")
	public void testJobCreationInputs() {
		// create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 16: /conf/core/job-creation-op
	* Test Purpose: Validate the creation of a new job.
	* Requirement: /req/core/job-creation-op
	* Test Method:
	* 1.  Validate the creation of the job according the requirements req_core_job-creation-default-execution-mode,
	* /req/core/job-creation-default-execution-mode,
	* req_core_job-creation-auto-execution-mode,/req/core/job-creation-auto-execution-mode
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-op ")
	public void testJobCreationOp() {

		HttpResponse httpResponse = null;

		if (echoProcessSupportsAsync()) {
			// create async job
			JsonNode executeNode = createExecuteJsonNode(echoProcessId);
			final ValidationData<Void> data = new ValidationData<>();
			try {
				HttpClient client = HttpClientBuilder.create().build();
				String executeEndpoint = rootUri + echoProcessPath + "/execution";
				HttpPost request = new HttpPost(executeEndpoint);
				this.reqEntity = request;
				request.setHeader("Accept", "application/json");
				request.setHeader("Prefer", "respond-async ");
				ContentType contentType = ContentType.APPLICATION_JSON;
				request.setEntity(new StringEntity(executeNode.toString(), contentType));
				httpResponse = client.execute(request);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
				Header locationHeader = httpResponse.getFirstHeader("location");
				String locationString = locationHeader.getValue();
				client = HttpClientBuilder.create().build();
				HttpGet statusRequest = new HttpGet(locationString);
				request.setHeader("Accept", "application/json");
				httpResponse = client.execute(statusRequest);
				StringWriter writer = new StringWriter();
				String encoding = StandardCharsets.UTF_8.name();
				IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
				JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
				Body body = Body.from(responseNode);
				Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
				Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body)
						.header(CONTENT_TYPE, responseContentType.getValue()).build();
				getStatusValidator.validateResponse(response, data);
				Assert.assertTrue(data.isValid(), printResults(data.results()));
			}
			catch (Exception e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}
		else {
			// create sync job
			JsonNode executeNode = createExecuteJsonNode(echoProcessId);
			final ValidationData<Void> data = new ValidationData<>();
			try {
				httpResponse = sendPostRequestSync(executeNode);
				String responsePayload = parseRawResponse(httpResponse);
				if (responsePayload.contains("Content-Type: multipart/related")) {
					Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
					if (responseContentType.getValue().startsWith("multipart/related")) {
						validateMultipartResponse(responsePayload, executeNode);
					}
					else {
						throw new SkipException("The value of the Content-Type header of the response is "
								+ responseContentType.getValue()
								+ " but the response payload states Content-Type: multipart/related");
					}
				}
				else {
					try {
						JsonNode responseNode = new ObjectMapper().readTree(responsePayload);
						Body body = Body.from(responseNode);
						Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
						Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode())
								.body(body).header(CONTENT_TYPE, responseContentType.getValue()).build();
						getStatusValidator.validateResponse(response, data);
						Assert.assertTrue(data.isValid(), printResults(data.results()));
					}
					catch (IOException e) {
						Assert.fail(e.getLocalizedMessage());
					}
				}
			}
			catch (Exception ee) {
				Assert.fail(ee.getLocalizedMessage());
			}

		}
	}

	/**
	 * <pre>
	* Abstract Test 19: /conf/core/job-creation-request
	* Test Purpose: Validate that the body of a job creation operation complies with the required structure and contents.
	* Requirement: /req/core/job-creation-request
	* Test Method:
	
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-request ")
	public void testJobCreationRequest() {
		// create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		final ValidationData<Void> data = new ValidationData<>();
		HttpResponse httpResponse = null;
		String responsePayload = null;
		try {
			if (echoProcessSupportsAsync()) {
				httpResponse = sendPostRequestASync(executeNode);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
				Header locationHeader = httpResponse.getFirstHeader("location");
				String locationString = locationHeader.getValue();
				httpResponse = sendGetRequest(locationString, "application/json");

			}
			else {

				httpResponse = sendPostRequestSync(executeNode);

			}

			responsePayload = parseRawResponse(httpResponse);

		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}

		if (responsePayload.contains("Content-Type: multipart/related")) {
			Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
			if (responseContentType.getValue().startsWith("multipart/related")) {
				validateMultipartResponse(responsePayload, executeNode);
			}
			else {
				throw new SkipException(
						"The value of the Content-Type header of the response is " + responseContentType.getValue()
								+ " but the response payload states Content-Type: multipart/related");
			}
		}
		else {
			try {
				JsonNode responseNode = new ObjectMapper().readTree(responsePayload);
				Body body = Body.from(responseNode);
				Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
				Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body)
						.header(CONTENT_TYPE, responseContentType.getValue()).build();
				getStatusValidator.validateResponse(response, data);
				Assert.assertTrue(data.isValid(), printResults(data.results()));
			}
			catch (IOException e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}

	}

	/**
	 * <pre>
	* Abstract Test 34: /conf/core/job-creation-success-async
	* Test Purpose: Validate the results of a job that has been created using the `async` execution mode.
	* Requirement: /req/core/job-creation-success-async
	* Test Method:
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-success-async ")
	public void testJobCreationSuccessAsync() {
		if (echoProcessSupportsAsync()) {
			// create job
			JsonNode executeNode = createExecuteJsonNode(echoProcessId);
			final ValidationData<Void> data = new ValidationData<>();
			try {
				HttpResponse httpResponse = sendPostRequestASync(executeNode);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
				Header locationHeader = httpResponse.getFirstHeader("location");
				String locationString = locationHeader.getValue();
				httpResponse = sendGetRequest(locationString, "application/json");
				validateResponse(httpResponse, getStatusValidator, data);
			}
			catch (Exception e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}
		else {
			throw new SkipException(ASYNC_MODE_NOT_SUPPORTED_MESSAGE);
		}
	}

	private void validateResponse(HttpResponse httpResponse, OperationValidator validator, ValidationData<Void> data)
			throws IOException {
		JsonNode responseNode = parseResponse(httpResponse);
		Body body = Body.from(responseNode);
		// https://github.com/opengeospatial/ets-ogcapi-processes10/issues/14
		// Treat Content-Type application/problem+json as application/json for now
		Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
		String responseContentTypeValue = responseContentType.getValue();
		if (responseContentTypeValue.equals("application/problem+json")) {
			responseContentTypeValue = "application/json";
		}
		if (validator.getOperation().getOperationId().equals("execute")
				&& httpResponse.getStatusLine().getStatusCode() == 200) {
			responseContentTypeValue = "/*";
		}
		Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body)
				.header(CONTENT_TYPE, responseContentTypeValue).build();
		validator.validateResponse(response, data);
		Assert.assertTrue(data.isValid(), printResults(data.results()));
	}

	private JsonNode parseResponse(HttpResponse httpResponse) throws IOException {
		StringWriter writer = new StringWriter();
		String encoding = StandardCharsets.UTF_8.name();
		IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
		this.rspEntity = writer.toString();
		return new ObjectMapper().readTree(writer.toString());
	}

	private String parseRawResponse(HttpResponse httpResponse) throws IOException {
		StringWriter writer = new StringWriter();
		String encoding = StandardCharsets.UTF_8.name();
		IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
		return writer.toString();
	}

	private HttpResponse sendGetRequest(String url, String acceptType) throws IOException {
		HttpGet request = new HttpGet(url);
		this.reqEntity = request;
		request.setHeader("Accept", acceptType);
		return clientBuilder.build().execute(request);
	}

	/**
	 * <pre>
	* Abstract Test 33: /conf/core/job-creation-sync-document
	* Test Purpose: Validate that the server responds as expected when synchronous execution is sc_execution_code,negotiated and the response type is `document`.
	* Requirement: /req/core/job-creation-sync-document
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request ensuring that synchronous execution has been sc_execution_mode,negotiated according to tests ats_core_job-creation-default-execution-mode,/conf/core/job-creation-default-execution-mode and the requested response type is `document` (ie `"response": "document"`) according to requirement req_core_job-creation-sync-document,/req /core/job-creation-sync-document
	* 2.  Verify that each process executes successfully according to requirement req_core_job-creation-sync-document,/req/core/job-creation-sync-document
	* |===
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-sync-document ")
	public void testJobCreationSyncDocument() {
		// create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId, RESPONSE_VALUE_DOCUMENT);
		try {
			sendPostRequestSync(executeNode, true);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	private JsonNode createExecuteJsonNode(String echoProcessId, String responseMode) {
		ObjectNode executeJsonNode = createExecuteJsonNode(echoProcessId);
		executeJsonNode.set(RESPONSE_KEY, new TextNode(responseMode));
		return executeJsonNode;
	}

	/**
	 * <pre>
	* Abstract Test 32: /conf/core/job-creation-sync-raw-mixed-multi
	* Test Purpose: Validate that the server responds as expected when synchronous execution is sc_execution_mode,negotiated, the response type is `raw` and the output transmission is a mix of `value` and `reference`.
	* Requirement: /req/core/job-creation-sync-raw-mixed-multi
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request ensuring that synchronous execution is sc_execution_mode,negotiated according to test ats_core_job-creation-default-execution-mode,/conf/core/job-creation-default-execution-mode, that more than one output is requested, that the requested response type is `raw` (ie `"response": "raw"`) and the the transmission mode is a mix of `value` (ie `"transmissionMode": "value"`) and reference (ie `"transmissionMode": "reference"`) according to requirement req_core_job-creation-sync-raw-mixed-multi,/req/core/job-creation-sync-raw-mixed-multi
	* 2.  Verify that each process executes successfully according to requirement req_core_job-creation-sync-raw-mixed-multi,/req/core/job-creation-sync-raw-mixed-multi
	* 3.  For each output requested with `"transmissionMode": "value"` verify that the body of the corresponding part contains the output value
	* 4.  For each output requested with `"transmissionMode": "reference"` verify that the body of the corresponding part is empty and the `Content-Location` header is included that points to the output value
	* |===
	*
	*
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-sync-raw-mixed-multi ")
	public void testJobCreationSyncRawMixedMulti() {
		// create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId, RESPONSE_VALUE_RAW);
		try {
			sendPostRequestSync(executeNode, true);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 31: /conf/core/job-creation-sync-raw-ref
	* Test Purpose: Validate that the server responds as expected when synchronous execution is sc_execution_mode,negotiated, the response type is `raw` and the transmission mode is `ref`.
	* Requirement: /req/core/job-creation-sync-raw-ref
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request ensuring that synchronous execution is sc_execution_mode,negotiated according to test ats_core_job-creation-default-execution-mode,/conf/core/job-creation-default-execution-mode, that the requested response type is `raw` (ie `"response": "raw"`) and the transmission mode is set to `ref` (ie `"transmissionMode": "ref"`) according to requirement req_core_job-creation-sync-raw-ref,/req/core/job-creation-sync-raw-ref
	* 2.  Verify that each process executes successfully according to requirement req_core_job-creation-sync-raw-ref,/req/core/job-creation-sync-raw-ref
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-sync-raw-ref ")
	public void testJobCreationSyncRawRef() {
		// create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId, RESPONSE_VALUE_RAW);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode, true);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	private void validateMultipartResponse(String responsePayload, JsonNode executeNode) {

		boolean foundTestString = false;
		String errorMessage = "";
		this.rspEntity = responsePayload;

		try {

			MimeMultipart mimeMultipart = new MimeMultipart(
					new ByteArrayDataSource(responsePayload.getBytes(), "multipart/related"));
			if (mimeMultipart.getCount() < 1) {
				Assert.assertTrue(mimeMultipart.getCount() > 0, "Error with multipart response");
			}
			for (int i = 0; i < mimeMultipart.getCount(); i++) {
				if (mimeMultipart.getBodyPart(i).getContent().toString().contains(TEST_STRING_INPUT)) {
					foundTestString = true;
					break;
				}
			}

		}
		catch (Exception e) {
			errorMessage = e.getLocalizedMessage();
		}
		if (!foundTestString) {
			errorMessage = "The input test string was not detected in one of the parts of the multipart response.";
		}

		Assert.assertTrue(foundTestString, "The multipart response failed the validity check because " + errorMessage);
	}

	/**
	 * <pre>
	* Abstract Test 30: /conf/core/job-creation-sync-raw-value-multi
	* Test Purpose: Validate that the server responds as expected when synchronous execution is sc_execution_mode,negotiated, the response type is `raw` and the output transmission is `value`.
	* Requirement: /req/core/job-creation-sync-raw-value-multi
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request ensuring that synchronous execution is sc_execution_mode,negotiated according to test ats_core_job-creation-default-execution-mode,/conf/core/job-creation-default-execution-mode, that more than one output is requested, that the requested response type is `raw` (ie `"response": "raw"`) and the the transmission mode is set to `value` (ie `"transmissionMode": "value"`) according to requirement req_core_job-creation-sync-raw-value-multi,/req/core/job-creation-sync-raw-value-multi
	* 2.  Verify that each process executes successfully according to requirement req_core_job-creation-sync-raw-value-multi,/req/core/job-creation-sync-raw-value-multi
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-sync-raw-value-multi ")
	public void testJobCreationSyncRawValueMulti() {
		// create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId, RESPONSE_VALUE_RAW);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode, true);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 28: /conf/core/job-creation-sync-raw-value-one
	* Test Purpose: Validate that the server responds as expected when synchronous execution is sc_execution_mode,negotiated, a single output value is requested, the response type is `raw` and the output transmission is `value`.
	* Requirement: /req/core/job-creation-sync-raw-value-one
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request ensuring that synchronous execution is sc_execution_mode,negotiated according to test ats_core_job-creation-default-execution-mode,/conf/core/job-creation-default-execution-mode, that only one output is requested, that the requested response type is `raw` (ie `"response": "raw"`) and that the output transmission is set to `value` (ie `"transmissionMode": "value"`) according to requirement req_core_job-creation-sync-raw-value-one,/req/core/job-creation-sync-raw-value-one
	* 2.  Verify that each process executes successfully according to requirement req_core_job-creation-sync-raw-value-one,/req/core/job-creation-sync-raw-value-one
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-creation-sync-raw-value-one ")
	public void testJobCreationSyncRawValueOne() {
		// create job

		JsonNode executeNode = createExecuteJsonNodeOneOutput(echoProcessId, RESPONSE_VALUE_RAW);
		try {

			HttpResponse httpResponse = sendPostRequestSync(executeNode, true);

			Assert.assertTrue(parseRawResponse(httpResponse).contains(TEST_STRING_INPUT));

		}
		catch (Exception e) {

			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 37: /conf/core/job-exception-no-such-job
	* Test Purpose: Validate that an invalid job identifier is handled correctly.
	* Requirement: /req/core/job-exception-no-such-job
	* Test Method:
	* 1.  Validate that the document contains the exception type "http://wwwopengisnet/def/exceptions/ogcapi-processes-1/10/no-such-job"
	* 2.  Validate the document for all supported media types using the resources and tests identified in job-exception-no-such-job
	* |===
	*
	* An exception response caused by the use of an invalid job identifier may be retrieved in a number of different formats. The following table identifies the applicable schema document for each format and the test to be used to validate the response. All supported formats should be exercised.
	*
	* [[job-exception-no-such-job]]
	* 3. Schema and Tests for the Job Result for Non-existent Job
	* [width="90%",cols="3",options="header"]
	* |===
	* |Format |Schema Document |Test ID
	* |HTML |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/exception.yaml[exception.yaml] |ats_html_content,/conf/html/content
	* |JSON |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/exception.yaml[exception.yaml] |ats_json_content,/conf/json/content
	* |===
	*
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-exception-no-such-job ")
	public void testJobExceptionNoSuchJob() {
		final ValidationData<Void> data = new ValidationData<>();
		try {
			HttpResponse httpResponse = sendGetRequest(getInvalidJobURL.toString(), "application/json");
			validateResponse(httpResponse, getJobsValidator, data);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 35: /conf/core/job-op
	* Test Purpose: Validate that the status info of a job can be retrieved.
	* Requirement: /req/core/job
	* Test Method:
	* 1.  Validate the contents of the returned document using the test ats_core_job-success,/conf/core/job-success
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job ")
	public void testJobOp() {
		// create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		try {
			HttpClient client = HttpClientBuilder.create().build();
			String executeEndpoint = rootUri + echoProcessPath + "/execution";
			HttpPost request = new HttpPost(executeEndpoint);
			this.reqEntity = request;
			request.setHeader("Accept", "application/json");
			ContentType contentType = ContentType.APPLICATION_JSON;
			request.setEntity(new StringEntity(executeNode.toString(), contentType));
			HttpResponse httpResponse = client.execute(request);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test null: /conf/core/job-results-no-such-job
	* Test Purpose: Validate that the job results retrieved using an invalid job identifier complies with the require structure and contents.
	* Requirement: /req/core/job-results-exception-no-such-job
	* Test Method:
	* 1.  Validate that the document contains the exception type "http://wwwopengisnet/def/exceptions/ogcapi-processes-1/10/no-such-job"
	* 2.  Validate the document for all supported media types using the resources and tests identified in job-results-exception-no-such-job
	* |===
	*
	* The job results page for a job may be retrieved in a number of different formats. The following table identifies the applicable schema document for each format and the test to be used to validate the job results for a non-existent job against that schema.  All supported formats should be exercised.
	*
	* [[job-results-exception-no-such-job]]
	* 3. Schema and Tests for the Job Result for Non-existent Job
	* [width="90%",cols="3",options="header"]
	* |===
	* |Format |Schema Document |Test ID
	* |HTML |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/exception.yaml[exception.yaml] |ats_html_content,/conf/html/content
	* |JSON |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/exception.yaml[exception.yaml] |ats_json_content,/conf/json/content
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-results-exception-no-such-job ")
	public void testJobResultsNoSuchJob() {
		final ValidationData<Void> data = new ValidationData<>();
		try {
			HttpResponse httpResponse = sendGetRequest(getInvalidJobResultURL.toString(), "application/json");
			validateResponse(httpResponse, getResultValidator, data);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 46: /conf/core/job-results-exception-results-not-ready
	* Test Purpose: Validate that the job results retrieved for an incomplete job complies with the require structure and contents.
	* Requirement: /req/core/job-results-exception-results-not-ready
	* Test Method:
	* 1.  Validate that the document was returned with a 404
	* 2.  Validate that the document contains the exception `type` "http://wwwopengisnet/def/exceptions/ogcapi-processes-1/10/result-not-ready"
	* 3.  Validate the document for all supported media types using the resources and tests identified in job-results-exception-results-not-ready
	* |===
	*
	* The job results page for a job may be retrieved in a number of different formats. The following table identifies the applicable schema document for each format and the test to be used to validate the job results for an incomplete job against that schema.  All supported formats should be exercised.
	*
	* [[job-results-exception-results-not-ready]]
	* 4. Schema and Tests for the Job Result for an Incomplete Job
	* [width="90%",cols="3",options="header"]
	* |===
	* |Format |Schema Document |Test ID
	* |HTML |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/exception.yaml[exception.yaml] |ats_html_content,/conf/html/content
	* |JSON |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/exception.yaml[exception.yaml] |ats_json_content,/conf/json/content
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-results-exception-results-not-ready ")
	public void testJobResultsExceptionResultsNotReady() {
		if (echoProcessSupportsAsync()) {
			JsonNode statusNode = null;
			HttpResponse httpResponse = null;
			final ValidationData<Void> data = new ValidationData<>();
			// create invalid execute request
			JsonNode executeNode;
			try {
				executeNode = createExecuteJsonNodeWithPauseInput(echoProcessId, RESPONSE_VALUE_RAW);
			}
			catch (SkipException e) {
				throw e;
			}

			try {
				System.out.println("Checking error: asynchronous POST request..." + executeNode);
				httpResponse = sendPostRequestASync(executeNode);

				ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
				httpResponse.getEntity().writeTo(baos1);

				String responseContentString1 = baos1.toString();
				baos1.close();
				System.out.println("HTTP Response Content: " + responseContentString1);
				httpResponse.getEntity().getContent().close();

				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Header locationHeader = httpResponse.getFirstHeader("location"); // location
																					// of
																					// job
				String jobLocationString = locationHeader.getValue();
				System.out.println("Job location: " + jobLocationString);

				System.out.println("Checking error: Sending GET request to job location...");
				httpResponse = sendGetRequest(jobLocationString, "application/json"); // Issue
																						// an
																						// HTTP
																						// GET
																						// request
																						// to
																						// the
																						// URL
																						// ‘/jobs/{jobID}/results’
																						// before
																						// the
																						// job
																						// completes
																						// execution.

				String jobResultsLocationString = jobLocationString + "/results";
				System.out.println("Checking error: Job results location: " + jobResultsLocationString);
				httpResponse = sendGetRequest(jobResultsLocationString, "application/json");
				Assert.assertEquals(httpResponse.getStatusLine().getStatusCode(), 404,
						"Failed Abstract test A.46 (Step 3). The response to the /jobs/{jobID}/results request did not return a 404 status code.");

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				httpResponse.getEntity().writeTo(baos);

				String responseContentString = baos.toString();
				baos.close();
				System.out.println("HTTP Response Content: " + responseContentString);
				httpResponse.getEntity().getContent().close();
				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode resultsNode = objectMapper.readTree(responseContentString);
				JsonNode typeNode = resultsNode.get("type");
				if (typeNode != null) {
					String typeValue = typeNode.textValue();
					System.out.println("Actual Type Value: " + typeValue);
				}

				Assert.assertTrue(
						resultsNode.get("type").textValue().equals(
								"http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/result-not-ready"),
						"Failed Abstract test A.46 (Step 4). The document did not contain an exception of type http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/result-not-ready");

				Assert.assertTrue(validateResponseAgainstSchema(EXCEPTION_SCHEMA_URL, responseContentString),
						"Failed Abstract test A.46 (Step 5). Unable to validate the response document against: "
								+ EXCEPTION_SCHEMA_URL);

				boolean isSchemaValid = validateResponseAgainstSchema(EXCEPTION_SCHEMA_URL, responseContentString);
				System.out.println("Schema Validation Result: " + isSchemaValid);

			}
			catch (Exception e) {
				System.out.println("Checking error: An exception occurred: " + e.getMessage());
				e.printStackTrace();
				Assert.fail(e.getLocalizedMessage());
			}

		}
		else {
			throw new SkipException(
					"This test is skipped because the server has not declared support for asynchronous execution mode.");
		}

	}

	/**
	 * <pre>
	* Abstract Test 47: /conf/core/job-results-failed
	* Test Purpose: Validate that the job results for a failed job complies with the require structure and contents.
	* Requirement: /req/core/job-results-failed
	* Test Method:
	* 1.  Issue an HTTP GET request to the URL '/jobs/{jobID}/results'
	* 2.  Validate that the document was returned with a HTTP error code (4XX or 5XX)
	* 3.  Validate that the document contains an exception `type` that corresponds to the reason the job failed (eg InvalidParameterValue for invalid input data)
	* 4.  Validate the document for all supported media types using the resources and tests identified in job-results-failed-schema
	* |===
	*
	* The job results page for a job may be retrieved in a number of different formats. The following table identifies the applicable schema document for each format and the test to be used to validate the job results for a failed job against that schema.  All supported formats should be exercised.
	*
	* [[job-results-failed-schema]]
	* 5. Schema and Tests for the Job Result for a Failed Job
	* [width="90%",cols="3",options="header"]
	* |===
	* |Format |Schema Document |Test ID
	* |HTML |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/exception.yaml[exception.yaml] |ats_html_content,/conf/html/content
	* |JSON |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/exception.yaml[exception.yaml] |ats_json_content,/conf/json/content
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-results-failed ")
	public void testJobResultsFailed() {
		final ValidationData<Void> data = new ValidationData<>();
		// create invalid execute request
		JsonNode executeNode = createInvalidExecuteJsonNode(echoProcessId);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode > 200, "Got unexpected status code: " + statusCode);
			validateResponse(httpResponse, executeValidator, data);
		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	private JsonNode createInvalidExecuteJsonNode(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		executeNode.set("invalid-execute-request", new TextNode("true)"));
		// executeNode.set("invalid-execute-request", objectMapper.);
		return executeNode;
	}

	/**
	 * <pre>
	* Abstract Test 38: /conf/core/job-results
	* Test Purpose: Validate that the results of a job can be retrieved.
	* Requirement: /req/core/job-results
	* Test Method:
	* 1.  Validate that the document was returned with a status code 200
	* 2.  Validate the contents of the returned document using the test ats_job-results-success,/conf/core/job-results-success
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-results ")
	public void testJobResults() {
		System.out.println("Entering testJobResults()");
		HttpResponse httpResponse = null;

		if (echoProcessSupportsAsync()) {
			// create async job
			JsonNode executeNode = createExecuteJsonNode(echoProcessId);
			final ValidationData<Void> data = new ValidationData<>();
			try {
				httpResponse = this.sendPostRequestASync(executeNode);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);
				Header locationHeader = httpResponse.getFirstHeader("location");
				String locationString = locationHeader.getValue();
				httpResponse = sendGetRequest(locationString, "application/json");
				assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,
						"Expected status code 200 but found " + httpResponse.getStatusLine().getStatusCode());

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				httpResponse.getEntity().writeTo(baos);
				String responseContentString = baos.toString();
				baos.close();
				httpResponse.getEntity().getContent().close();
				assertTrue(validateResponseAgainstSchema(STATUS_SCHEMA_URL, responseContentString),
						"Unable to validate the response document against: " + STATUS_SCHEMA_URL);

			}
			catch (Exception e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}
		else {
			// create sync job
			JsonNode executeNode = createExecuteJsonNode(echoProcessId);
			final ValidationData<Void> data = new ValidationData<>();
			try {
				System.out.println("Sending POST request...");
				httpResponse = sendPostRequestSync(executeNode);
				System.out.println("Received POST response: " + httpResponse);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Assert.assertTrue(statusCode == 200, "Got unexpected status code: " + statusCode);
			}
			catch (Exception ee) {
				Assert.fail(ee.getLocalizedMessage());
			}
		}
	}

	private HttpResponse getResultResponse(HttpResponse httpResponse) throws IOException {
		System.out.println("No 'results' link found in status document.");
		JsonNode statusNode = parseResponse(httpResponse);
		JsonNode linksNode = statusNode.get("links");
		Assert.assertNotNull(linksNode);
		Assert.assertTrue(!linksNode.isMissingNode(), "No links in status document.");
		if (linksNode instanceof ArrayNode) {
			ArrayNode linksArrayNode = (ArrayNode) linksNode;
			for (int i = 0; i < linksArrayNode.size(); i++) {
				JsonNode linksChildNode = linksArrayNode.get(i);
				if (linksChildNode.get("rel").asText().equals("http://www.opengis.net/def/rel/ogc/1.0/results")) {
					String resultsUrl = linksChildNode.get("href").asText();
					String resultsMimeType = linksChildNode.get("type").asText();
					return sendGetRequest(resultsUrl, resultsMimeType);
				}
			}
		}
		return null;
	}

	/**
	 * <pre>
	* Abstract Test 44: /conf/core/job-results-async-document
	* Test Purpose: Validate that the server responds as expected when the asynchronous execution is sc_execution_mode,negotiated and the response type is `document`.
	* Requirement: /req/core/job-results-async-document
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request ensuring that asynchronous execution is sc_execution_mode,negotiated according to test ats_core_job-creation-auto-execution-mode,/conf/core/job-creation-auto-execution-mode and that the requested response type is `document` (ie `"response": "document"`) according to requirement req_core_job-creation-async-document,/req/core/job-creation-async-document
	* 2.  If the server responds asynchronously periodically retrieve the status of the asynchronously execute job as per test ats_core_job-op,/conf/core/job-op
	* 3.  When the job status is `successful`, get the results as per test ats_core_job-results-op,/conf/core/job-results and verify that they conform to requirement req_core_job-results-async-document,/req/core/job-results-async-document
	* |====
	*
	* NOTE: In the case where a process supports both `async-execute` and `sync-execute` job control options there is a possibility that the server responds synchronously even though the `Prefer` headers asserts a `respond-async` preference.  In this case, the following additional test should be performed:
	*
	* [width="90%",cols="2,6a"]
	* |====
	* ^|Test Method |. Inspect the headers of the synchronous response and see if a `Link` header is included with `rel=monitor`.
	* 4.  If the link exists, get the job status as per test ats_core_job-op,/conf/cor e/job-op and ensure that the job status is set to `successful`
	* 5.  Get the results as per test ats_core_job-results-op,/conf/core/job-results and verify that they conform to the test ats_core_job-results-async-document,/conf/core/job-results-async-document
	* 6.  If the link does not exist then verify that the synchronous response conforms to the requirement req_core_job-creation-sync-document,/req/core/job-creation-sync-documen
	* |====
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-results-async-document ")
	public void testJobResultsAsyncDocument() {
		// create job
		if (echoProcessSupportsAsync()) {
			JsonNode executeNode = createExecuteJsonNode(echoProcessId, RESPONSE_VALUE_DOCUMENT);
			try {
				HttpResponse httpResponse = sendPostRequestASync(executeNode);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
			}
			catch (Exception e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}
		else {
			throw new SkipException(ASYNC_MODE_NOT_SUPPORTED_MESSAGE);
		}
	}

	/**
	 * <pre>
	* Abstract Test 43: /conf/core/job-results-async-raw-mixed-multi
	* Test Purpose: Validate that the server responds as expected when asynchronous execution is sc_execution_mode,negotiated, more than one output is requested, the response type is `raw` and the output transmission is a mix of `value` and `reference`.
	* Requirement: /req/core/job-results-async-raw-mixed-multi
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request ensuring that asynchronous execution is sc_execution_mode,negotiated according to test ats_core_job-creation-auto-execution-mode,/conf/core/job-creation-auto-execution-mode, that the requested response type is `raw` (ie `"response": "raw"`) and that the output transmission is set to a mix of `value` (ie `"outputTransmission": "value"`) and `reference` (ie `"outputTransmission": "reference"`) according to requirement req_core_job-creation-async-raw-mixed-multi,/req/core/job-creation-async-raw-mixed-multi
	* 2.  Periodically retrieve the status of the asynchronously execute job as per test ats_core_job-op,/conf/core/job-op
	* 3.  When the job status is `successful`, get the results as per test ats_core_job-results-op,/conf/core/job-results and verify that they conform to requirement req_core_job-results-async-raw-mixed-multi,/conf/core/job-results-async-raw-mixed-multi
	* 4.  For each output requested with `"transmissionMode": "value"` verify that the body of the corresponding part contains the output value
	* 5.  For each output requested with `"transmissionMode": "reference"` verify that the body of the corresponding part is empty and the `Content-Location` header is included that points to the output value
	* |====
	*
	* NOTE: In the case where a process supports both `async-execute` and `sync-execute` job control options there is a possibility that the server responds synchronously even though the `Prefer` headers asserts a `respond-async` preference.  In this case, the following additional test should be performed.
	*
	* [width="90%",cols="2,6a"]
	* |====
	* ^|Test Method |. Inspect the headers of the synchronous response and see if a `Link` header is included with `rel=monitor`.
	* 6.  If the link exists, get the job status as per test ats_core_job-op,/conf/cor e/job-op and ensure that the job status is set to `successful`
	* 7.  Get the results as per test ats_core_job-results-op,/conf/core/job-results and verify that they conform to the test ats_core_job-results-async-raw-mixed-multi,/conf/core/job-results-async-raw-mixed-multi
	* 8.  If the link does not exist then verify that the synchronous response conforms to requirement req_core_job-creation-sync-raw-mixed-multi,/req/core/job-creation-sync-raw-mixed-multi
	* |====
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-results-async-raw-mixed-multi ")
	public void testJobResultsAsyncRawMixedMulti() {
		if (echoProcessSupportsAsync()) {
			// create job
			JsonNode executeNode = createExecuteJsonNodeRawMixedMulti(echoProcessId);
			final ValidationData<Void> data = new ValidationData<>();
			try {
				HttpResponse httpResponse = sendPostRequestASync(executeNode);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
				Header locationHeader = httpResponse.getFirstHeader("location");
				String locationString = locationHeader.getValue();
				httpResponse = sendGetRequest(locationString, ContentType.APPLICATION_JSON.getMimeType());
				JsonNode responseNode = parseResponse(httpResponse);
				Body body = Body.from(responseNode);
				Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
				Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body)
						.header(CONTENT_TYPE, responseContentType.getValue()).build();
				getStatusValidator.validateResponse(response, data);
				Assert.assertTrue(data.isValid(), printResults(data.results()));
			}
			catch (Exception e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}
		else {
			throw new SkipException(ASYNC_MODE_NOT_SUPPORTED_MESSAGE);
		}
	}

	/**
	 * <pre>
	* Abstract Test 42: /conf/core/job-results-async-raw-ref
	* Test Purpose: Validate that the server responds as expected when asynchronous execution is ,sc_execution_mode,negotiated, the response type is `raw` and the output transmission is `reference`.
	* Requirement: /req/core/job-results-async-raw-ref
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request ensuring that synchronous execution is sc_execution_mode,negotiated according to test ats_core_job-creation-auto-execution-mode,/conf/core/job-creation-auto-execution-mode, that the requested response type is `raw` (ie `"response": "raw"`) and that the output transmission is set to `reference` (ie `"outputTransmission": "reference"`) according to requirement req_core_job-creation-async-raw-ref,/req/core/job-creation-async-raw-ref
	* 2.  If the server responds asynchronously, periodically retrieve the status of the asynchronously executed job as per test ats_core_job-op,/conf/core/job-op
	* 3.  When the job status is `successful`, get the results as per test ats_core_job-results-op,/conf/core/job-results and verify that they conform to requirement req_core_job-results-async-raw-ref,/req/core/job-results-async-ref
	* |====
	*
	* NOTE: In the case where a process supports both `async-execute` and `sync-execute` job control options there is a possibility that the server responds synchronously even though the `Prefer` headers asserts a `respond-async` preference.  In this case, the following additional test should be performed.
	*
	* [width="90%",cols="2,6a"]
	* |====
	* ^|Test Method |. Inspect the headers of the synchronous response and see if a `Link` header is included with `rel=monitor`.
	* 4.  If the link exists, get the job status as per test ats_core_job-op,/conf/core/job-op and ensure that the job status is set to `successful`
	* 5.  Get the results as per test ats_core_job-results-op,/conf/core/job-results and verify that they conform to the test ats_core_job-results-async-document,/conf/core/job-results-async-document
	* 6.  If the link does not exist then verify that the synchronous response conforms to requirement req_core_job-creation-sync-raw-ref,/req/core/job-creation-sync-raw-ref
	* |====
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-results-async-raw-ref ")
	public void testJobResultsAsyncRawRef() {
		if (echoProcessSupportsAsync()) {
			// create job
			JsonNode executeNode = createExecuteJsonNodeRawRef(echoProcessId);
			final ValidationData<Void> data = new ValidationData<>();
			try {
				HttpResponse httpResponse = sendPostRequestASync(executeNode);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
				Header locationHeader = httpResponse.getFirstHeader("location");
				String locationString = locationHeader.getValue();
				httpResponse = sendGetRequest(locationString, ContentType.APPLICATION_JSON.getMimeType());
				JsonNode responseNode = parseResponse(httpResponse);
				Body body = Body.from(responseNode);
				Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
				Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body)
						.header(CONTENT_TYPE, responseContentType.getValue()).build();
				getStatusValidator.validateResponse(response, data);
				Assert.assertTrue(data.isValid(), printResults(data.results()));
			}
			catch (Exception e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}
		else {
			throw new SkipException(ASYNC_MODE_NOT_SUPPORTED_MESSAGE);
		}
	}

	/**
	 * <pre>
	* Abstract Test 41: /conf/core/job-results-async-raw-value-multi
	* Test Purpose: Validate that the server responds as expected when asynchronous execution is sc_execution_mode,negotiated, more than one output is requested, the response type is `raw` and the output transmission is `value`.
	* Requirement: /req/core/job-results-async-raw-value-multi
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request ensuring that asynchronous execution is sc_execution_mode,negotiated according to test ats_core_job-creation-auto-execution-mode,/conf/core/job-creation-auto-execution-mode, that the requested response type is `raw` (ie `"response": "raw"`) and that the output transmission is set to `value` (ie `"outputTransmission": "value"`) according to requirement req_core_job-creation-async-raw-value-multi,/req/core/job-creation-async-raw-value-multi
	* 2.  Periodically retrieve the status of the asynchronously execute job as per test ats_core_job-op,/conf/core/job-op
	* 3.  When the job status is `successful`, get the results as per test ats_core_job-results-op,/conf/core/job-results and verify that they conform to requirement req_core_job-results-async-raw-value-multi,/conf/core/job-results-async-raw-value-multi
	* |====
	*
	* NOTE: In the case where a process supports both `async-execute` and `sync-execute` job control options there is a possibility that the server responds synchronously even though the `Prefer` headers asserts a `respond-async` preference.  In this case, the following additional test should be performed.
	*
	* [width="90%",cols="2,6a"]
	* |====
	* ^|Test Method |. Inspect the headers of the synchronous response and see if a `Link` header is included with `rel=monitor`.
	* 4.  If the link exists, get the job status as per test ats_core_job-op,/conf/cor e/job-op and ensure that the job status is set to `successful`
	* 5.  Get the results as per test ats_core_job-results-op,/conf/core/job-results and verify that they conform to the test ats_core_job-results-async-raw-value-multi,/conf/core/job-results-async-raw-value-multi
	* 6.  If the link does not exist then verify that the synchronous response conforms to requirement req_core_job-creation-sync-raw-value-multi,/req/core/job-creation-sync-raw-value-multi
	* |====
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-results-async-raw-value-multi ")
	public void testJobResultsAsyncRawValueMulti() {
		if (echoProcessSupportsAsync()) {
			// create job
			JsonNode executeNode = createExecuteJsonNodeRawValueMulti(echoProcessId);
			final ValidationData<Void> data = new ValidationData<>();
			try {
				HttpResponse httpResponse = sendPostRequestASync(executeNode);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
				Header locationHeader = httpResponse.getFirstHeader("location");
				String locationString = locationHeader.getValue();
				httpResponse = sendGetRequest(locationString, ContentType.APPLICATION_JSON.getMimeType());
				JsonNode responseNode = parseResponse(httpResponse);
				Body body = Body.from(responseNode);
				Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
				Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body)
						.header(CONTENT_TYPE, responseContentType.getValue()).build();
				getStatusValidator.validateResponse(response, data);
				Assert.assertTrue(data.isValid(), printResults(data.results()));
			}
			catch (Exception e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}
		else {
			throw new SkipException(ASYNC_MODE_NOT_SUPPORTED_MESSAGE);
		}
	}

	private boolean loopOverStatusReturnsFailed(JsonNode responseNode) {
		try {

			HttpClient client = HttpClientBuilder.create().build();
			ArrayNode linksArrayNode = (ArrayNode) responseNode.get("links");

			boolean hasMonitorOrResultLink = false;

			JsonNode statusNode = responseNode.get("status");

			if (statusNode != null) {
				String statusNodeText = statusNode.asText();
				if (statusNodeText.equals("failed")) {
					return true;
				}
				else if (statusNodeText.equals("successful")) {
					return false;
				}
			}

			if (!hasMonitorOrResultLink)
				for (JsonNode currentJsonNode : linksArrayNode) {

					// Fetch status document
					if (currentJsonNode.get("rel").asText() == "monitor") {
						HttpUriRequest request = new HttpGet(currentJsonNode.get("href").asText());
						request.setHeader("Accept", "application/json");
						HttpResponse httpResponse = client.execute(request);
						JsonNode resultNode = parseResponse(httpResponse);
						loopOverStatus(resultNode);
						hasMonitorOrResultLink = true;
					}
				}

		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
		return false;
	}

	private void loopOverStatus(JsonNode responseNode) {
		try {

			if (attempts >= MAX_ATTEMPTS) {
				throw new Exception(String.format("Server did not return result in %d seconds.",
						MAX_ATTEMPTS * ASYNC_LOOP_WAITING_PERIOD / 1000));
			}

			HttpClient client = HttpClientBuilder.create().build();
			ArrayNode linksArrayNode = (ArrayNode) responseNode.get("links");

			boolean hasMonitorOrResultLink = false;
			for (JsonNode currentJsonNode : linksArrayNode) {
				// Fetch result document
				if (currentJsonNode.get("rel").asText().equals("http://www.opengis.net/def/rel/ogc/1.0/results")) {

					HttpUriRequest request = new HttpGet(currentJsonNode.get("href").asText());

					HttpResponse httpResponse = client.execute(request);

					String resultString = parseRawResponse(httpResponse);
					this.rspEntity = responseNode.asText();

					// May be more generic here
					Assert.assertTrue(resultString.contains(TEST_STRING_INPUT),
							"Response does not contain " + TEST_STRING_INPUT + "\n" + resultString);
					hasMonitorOrResultLink = true;
				}

			}

			if (!hasMonitorOrResultLink)
				for (JsonNode currentJsonNode : linksArrayNode) {
					String relString = currentJsonNode.get("rel").asText();

					// Fetch status document
					if (relString.equals("monitor") || relString.equals("status")) {
						HttpUriRequest request = new HttpGet(currentJsonNode.get("href").asText());
						request.setHeader("Accept", "application/json");
						HttpResponse httpResponse = client.execute(request);
						JsonNode resultNode = parseResponse(httpResponse);
						try {
							Thread.sleep(ASYNC_LOOP_WAITING_PERIOD);
						}
						catch (Exception e) {
							TestSuiteLogger.log(Level.WARNING, e.getMessage());
						}
						attempts++;
						loopOverStatus(resultNode);
						hasMonitorOrResultLink = true;
					}
				}
			// if the code gets to this position, no result or monitor link were found
			// imho we need to throw an exception
			if (!hasMonitorOrResultLink)
				throw new AssertionError(
					"No result (rel='http://www.opengis.net/def/rel/ogc/1.0/results') or monitor (rel='monitor') links were found in response.");

		}
		catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	* Abstract Test 40: /conf/core/job-results-async-raw-value-one
	* Test Purpose: Validate that the server responds as expected when asynchronous execution is sc_execution_mode,negotiated, one output is requested, the response type is `raw` and the output transmission is `value`.
	* Requirement: /req/core/job-results-async-raw-value-one
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request ensuring that asynchronous execution is sc_execution_mode,negotiated according to test ats_core_job-creation-auto-execution-mode,/conf/core/job-creation-auto-execution-mode, that the requested response type is `raw` (ie `"response": "raw"`) and that the output transmission is set to `value` (ie `"outputTransmission": "value"`) according to requirement req_core_job-creation-async-raw-value-one,/req/core/job-creation-async-raw-value-one
	* 2.  If the server responds asynchronously, periodically retrieve the status of the asynchronously executed job as per test ats_core_job-op,/conf/core/job-op
	* 3.  When the job status is `successful`, get the results as per test ats_core_job-results-op,/conf/core/job-results and verify that they conform to requirement req_core_job-results-async-raw-value-one,/req/core/job-results-async-raw-value-one
	* |====
	*
	* NOTE: In the case where a process supports both `async-execute` and `sync-execute` job control options there is a possibility that the server responds synchronously even though the `Prefer` headers asserts a `respond-async` preference.  In this case, the following additional test should be performed.
	*
	* [width="90%",cols="2,6a"]
	* |====
	* ^|Test Method |. Inspect the headers of the synchronous response and see if a `Link` header is included with `rel=monitor`.
	* 4.  If the link exists, get the job status as per test ats_core_job-op,/conf/core/job-op and ensure that the job status is set to `successful`
	* 5.  Get the results as per test ats_core_job-results-op,/conf/core/job-results and verify that they conform to the test ats_core_job-results-async-raw-value-multi,/conf/core/job-results-async-raw-value-multi
	* 6.  If the link does not exist then verify that the synchronous response conforms to requirement req_core_job-creation-sync-raw-value-one/req/core/job-creation-sync-raw-value-one
	* |====
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-results-async-raw-value-one ")
	public void testJobResultsAsyncRawValueOne() {
		if (echoProcessSupportsAsync()) {
			// create job
			JsonNode executeNode = createExecuteJsonNodeOneOutput(echoProcessId, RESPONSE_VALUE_RAW);
			try {

				HttpResponse httpResponse = sendPostRequestASync(executeNode);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				System.out.println("Status Code: " + statusCode);
				Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
				JsonNode responseNode = parseResponse(httpResponse);
				if (statusCode == 200) {
					Assert.assertEquals(responseNode.asText(), TEST_STRING_INPUT);
				}
				else {

					Header locationHeader = httpResponse.getFirstHeader("location");
					String locationString = locationHeader.getValue();
					System.out.println("Location String: " + locationString);
					httpResponse = sendGetRequest(locationString, "application/json");
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					httpResponse.getEntity().writeTo(baos);
					String responseContentString = baos.toString();
					baos.close();
					httpResponse.getEntity().getContent().close();

					ObjectMapper objectMapper = new ObjectMapper();
					JsonNode statusNode = objectMapper.readTree(responseContentString);
					System.out.println("Status Node: " + statusNode);

					loopOverStatus(statusNode);
				}
			}
			catch (Exception e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}
		else {
			throw new SkipException(ASYNC_MODE_NOT_SUPPORTED_MESSAGE);
		}
	}

	private JsonNode createExecuteJsonNodeOneOutput(String echoProcessId, String responseValue) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		// executeNode.set("id", new TextNode(echoProcessId));
		Input inputOne = inputs.get(0);
		String inputId = inputOne.getId();
		for (Input input : inputs) {
			addInput(input, inputsNode);
		}
		for (Output output : outputs) {
			if (output.getId().equals(inputId)) {
				addOutput(output, outputsNode);
				break;
			}
		}
		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		executeNode.set(RESPONSE_KEY, new TextNode(responseValue));
		return executeNode;
	}

	private JsonNode createExecuteJsonNodeWithPauseInput(String echoProcessId, String responseValue) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		// executeNode.set("id", new TextNode(echoProcessId));
		Input inputOne = inputs.get(0);
		String inputId = inputOne.getId();
		addInput(inputOne, inputsNode);
		for (Output output : outputs) {
			if (output.getId().equals(inputId)) {
				addOutput(output, outputsNode);
				break;
			}
		}

		if (inputs.stream().anyMatch(p -> p.getId().equals("pause"))) {
			inputsNode.set("pause", new IntNode(5));
		}
		else {
			throw new SkipException("No input with id pause found.");
		}

		executeNode.set("inputs", inputsNode);
		executeNode.set("outputs", outputsNode);
		executeNode.set(RESPONSE_KEY, new TextNode(responseValue));
		return executeNode;
	}

	/**
	 * <pre>
	* Abstract Test 39: /conf/core/job-results-sync
	* Test Purpose: Validate that the server responds as expected when getting results from a job for a process that has been executed synchronously.
	* Requirement: /req/core/job-results-sync
	* Test Method:
	* 1.  For each identified process construct an execute request according to test ats_core_job-creation-request,/conf/core/job-creation-request ensuring that synchronous execution is sc_execution_mode,negotiated according to test ats_core_job-creation-default-execution-mode,/conf/core/job-creation-default-execution-mode
	* 2.  Inspect the headers of the response and see if a `Link` header is included with `rel=monitor`
	* 3.  If the link exists, get the job status as per test ats_core_job-op,/conf/core/job-op and ensure that the job status is set to `successful`
	* |====
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-results-sync ")
	public void testJobResultsSync() {
		// create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		final ValidationData<Void> data = new ValidationData<>();
		try {

			HttpResponse httpResponse = sendPostRequestSync(executeNode, true);
			Header[] headers = httpResponse.getHeaders("Link");

			if (headers.length > 0) {
				boolean foundRelMonitorHeader = false;
				String statusUrl = "";
				for (Header header : headers) {
					String headerValue = header.getValue();

					if (headerValue.contains("rel=monitor")) {
						foundRelMonitorHeader = true;
						statusUrl = headerValue.split(";")[0];
						break;
					}
				}

				if (!foundRelMonitorHeader)
					Assert.assertTrue(foundRelMonitorHeader,
							"Permission 7 and Requirement 33 of OGC 18-062r2 state that for servers that support the creation of a job for synchronously executed processes, a successful execution of the operation SHALL include an HTTP Link header with rel=monitor pointing to the created job.");

				httpResponse = sendGetRequest(statusUrl, ContentType.APPLICATION_JSON.getMimeType());
				validateResponse(httpResponse, getStatusValidator, data);

			}

		}
		catch (IOException e) {

			Assert.fail(e.getLocalizedMessage());
		}

	}

	/**
	 * <pre>
	* Abstract Test 36: /conf/core/job-success
	* Test Purpose: Validate that the job status info complies with the require structure and contents.
	* Requirement: /req/core/job-success
	* Test Method:
	* |===
	*
	* The status info page for a job may be retrieved in a number of different formats. The following table identifies the applicable schema document for each format and the test to be used to validate the status info against that schema. All supported formats should be exercised.
	*
	* [[job-status-info-schema]]
	* 1. Schema and Tests for the Job Status Info
	* [width="90%",cols="3",options="header"]
	* |===
	* |Format |Schema Document |Test ID
	* |HTML |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/statusInfo.yaml[statusInfo.yaml] |ats_html,/conf/html/content
	* |JSON |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/statusInfo.yaml[statusInfo.yaml] |ats_json_content,/conf/json/content
	* |===
	* TODO: Check additional content
	* </pre>
	 */
	@Test(description = "Implements Requirement /req/core/job-success ")
	public void testJobSuccess() {

		if (echoProcessSupportsAsync()) {
			// create async job
			JsonNode executeNode = createExecuteJsonNode(echoProcessId);
			final ValidationData<Void> data = new ValidationData<>();
			try {
				HttpClient client = HttpClientBuilder.create().build();
				String executeEndpoint = rootUri + echoProcessPath + "/execution";
				HttpPost request = new HttpPost(executeEndpoint);
				request.setHeader("Accept", "application/json");
				request.setHeader("Prefer", "respond-async ");
				ContentType contentType = ContentType.APPLICATION_JSON;
				request.setEntity(new StringEntity(executeNode.toString(), contentType));
				HttpResponse httpResponse = client.execute(request);
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
				Header locationHeader = httpResponse.getFirstHeader("location");
				String locationString = locationHeader.getValue();
				client = HttpClientBuilder.create().build();
				HttpGet statusRequest = new HttpGet(locationString);
				request.setHeader("Accept", "application/json");
				httpResponse = client.execute(statusRequest);
				StringWriter writer = new StringWriter();
				String encoding = StandardCharsets.UTF_8.name();
				IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
				JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
				Body body = Body.from(responseNode);
				Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
				Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body)
						.header(CONTENT_TYPE, responseContentType.getValue()).build();
				getStatusValidator.validateResponse(response, data);
				Assert.assertTrue(data.isValid(), printResults(data.results()));
			}
			catch (Exception e) {
				Assert.fail(e.getLocalizedMessage());
			}
		}
		else {
			throw new SkipException(Jobs.ASYNC_MODE_NOT_SUPPORTED_MESSAGE
					+ " Also note that the specification does not mandate that servers create a job as a result of executing a process synchronously (See Clause 7.11.4 of OGC 18-062r2)");
		}
	}

}