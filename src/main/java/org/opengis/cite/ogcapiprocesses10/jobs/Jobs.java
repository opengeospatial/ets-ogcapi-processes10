package org.opengis.cite.ogcapiprocesses10.jobs;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
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
import org.opengis.cite.ogcapiprocesses10.util.ExecutionMode;
import org.opengis.cite.ogcapiprocesses10.util.TestSuiteLogger;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 *
 * A.2.6. Jobs  {root}/jobs
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

	private OpenApi3 openApi3;
	
	private String getJobsListPath = "/jobs";
	
//	private String getJobPath = "/jobs";
	
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
	
//	private CloseableHttpClient client;
	
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
			echoProcessId = (String) testContext.getSuite().getAttribute( SuiteAttribute.ECHO_PROCESS_ID.getName() );
		    echoProcessPath = getProcessListPath + "/" + echoProcessId;
			executeEndpoint = rootUri + echoProcessPath + "/execution";
		    parseEchoProcess();
			openApi3 = new OpenApi3Parser().parse(specURI.toURL(), false);
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
		    getInvalidJobURL = new URL(processListEndpointString + "/invalid-job-" + UUID.randomUUID());
		    getInvalidJobResultURL = new URL(rootUri +  getProcessListPath + "/invalid-job-" + UUID.randomUUID() + "/results");
			clientBuilder = HttpClientBuilder.create();
		} catch (MalformedURLException | ResolutionException | ValidationException e) {
			Assert.fail("Could set up endpoint: " + processListEndpointString + ". Exception: " + e.getLocalizedMessage());
		}
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
			if(inputsNode instanceof ArrayNode) {
				ArrayNode inputsArrayNode = (ArrayNode)inputsNode;				
				for (int i = 0; i < inputsArrayNode.size(); i++) {
					System.out.println(inputsArrayNode.get(i));
				}
			} else {
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
			if(outputsNode instanceof ArrayNode) {
				ArrayNode outputsArrayNode = (ArrayNode)outputsNode;
				
				for (int i = 0; i < outputsArrayNode.size(); i++) {
					System.out.println(outputsArrayNode.get(i));
				}
			} else {
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
			if(jobControlOptionsNode != null && !jobControlOptionsNode.isMissingNode()) {
				if(jobControlOptionsNode instanceof ArrayNode) {
					ArrayNode jobControlOptionsArrayNode = (ArrayNode) jobControlOptionsNode;
					boolean syncSupported = false;
					boolean aSyncSupported = false;
					for (int i = 0; i < jobControlOptionsArrayNode.size(); i++) {
						JsonNode jobControlOptionsArrayChildNode = jobControlOptionsArrayNode.get(i);
						if(jobControlOptionsArrayChildNode.asText().equals(JOB_CONTROL_OPTIONS_SYNC)) {
							syncSupported = true;
						} else if(jobControlOptionsArrayChildNode.asText().equals(JOB_CONTROL_OPTIONS_ASYNC)) {
							aSyncSupported = true;
						} 
					}
					if(syncSupported && !aSyncSupported) {
						supportedExecutionModes = SupportedExecutionModes.ONLY_SYNC;
					}
					if(aSyncSupported && !syncSupported) {
						supportedExecutionModes = SupportedExecutionModes.ONLY_ASYNC;
					}
					if(syncSupported && aSyncSupported) {
						supportedExecutionModes = SupportedExecutionModes.EITHER;
					}
				}
			}
		} catch (IOException e) {
			Assert.fail("Could not parse echo process.");
		}
		
	}

	private ObjectNode createExecuteJsonNode(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		executeNode.set("id", new TextNode(echoProcessId));
		for (Input input : inputs) {
			addInput(input, inputsNode);
		}
		for (Output output : outputs) {
			addOutput(output, outputsNode);
		}
		executeNode.set("inputs", inputsNode);
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
			if(type.getTypeDefinition().equals("string")) {
//		        inputNode.set("value", new TextNode("teststring"));
				inputsNode.set(input.getId(), new TextNode(TEST_STRING_INPUT));
			} else if(input.isBbox()) {
				inputNode.set("crs", new TextNode("urn:ogc:def:crs:EPSG:6.6:4326"));
				ArrayNode arrayNode = objectMapper.createArrayNode();
				arrayNode.add(345345345);
				arrayNode.add(345345345);
				arrayNode.add(345345345);
				arrayNode.add(345345345);
				inputNode.set("bbox", arrayNode);
				inputsNode.set(input.getId(), inputNode);
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
	@Test(description = "Implements Requirement /req/core/job-creation-op ", groups = "job")
	public void testJobCreationAutoExecutionMode() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		try {
			//send execute request with prefer header
			HttpResponse httpResponse = sendPostRequestASync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if(supportedExecutionModes.equals(SupportedExecutionModes.ONLY_SYNC)) {
				Assert.assertTrue(statusCode == 200, "Got unexpected status code: " + statusCode);		
			}
			if(supportedExecutionModes.equals(SupportedExecutionModes.ONLY_ASYNC)) {
				Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);		
			}
			if(supportedExecutionModes.equals(SupportedExecutionModes.EITHER)) {
				Assert.assertTrue(statusCode == 201 || statusCode == 200, "Got unexpected status code: " + statusCode);		
			}
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
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
	@Test(description = "Implements Requirement /req/core/job-creation-op ", groups = "job")
	public void testJobCreationDefaultExecutionMode() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		try {
			//send execute request without prefer header
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if(supportedExecutionModes.equals(SupportedExecutionModes.ONLY_SYNC) || 
					supportedExecutionModes.equals(SupportedExecutionModes.EITHER)) {
				Assert.assertTrue(statusCode == 200, "Got unexpected status code: " + statusCode);		
			}
			if(supportedExecutionModes.equals(SupportedExecutionModes.ONLY_ASYNC)) {
				Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);		
			}
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-creation-op ", groups = "job")
	public void testJobCreationDefaultOutputs() {
		//create job
		JsonNode executeNode = createExecuteJsonNodeNoOutputs(echoProcessId);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	private JsonNode createExecuteJsonNodeNoOutputs(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		executeNode.set("id", new TextNode(echoProcessId));
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
	@Test(description = "Implements Requirement /req/core/job-creation-input-array ", groups = "job")
	public void testJobCreationInputArray() {
		//create job
		JsonNode executeNode = createExecuteJsonNodeWithInputArray(echoProcessId);
		TestSuiteLogger.log(Level.INFO, executeNode.toString());
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-creation-input-inline-bbox ", groups = "job")
	public void testJobCreationInputInlineBbox() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		TestSuiteLogger.log(Level.INFO, executeNode.toString());
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-creation-input-binary ", groups = "job")
	public void testJobCreationInputInlineBinary() {
		//create job
		JsonNode executeNode = createExecuteJsonNodeWithBinaryInput(echoProcessId);
		TestSuiteLogger.log(Level.INFO, executeNode.toString());
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-creation-input-inline-mixed ", groups = "job")
	public void testJobCreationInputInlineMixed() {
		//create job
		JsonNode executeNode = createExecuteJsonNodeWithMixedInput(echoProcessId);
		TestSuiteLogger.log(Level.INFO, executeNode.toString());
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-creation-input-inline-object ", groups = "job")
	public void testJobCreationInputInlineObject() {
		//create job
		JsonNode executeNode = createExecuteJsonNodeWithObject(echoProcessId);
		try {
			HttpResponse httpResponse =sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	private JsonNode createExecuteJsonNodeWithObject(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		executeNode.set("id", new TextNode(echoProcessId));
		boolean foundObjectInput = false;
		for (Input input : inputs) {
			boolean inputIsObject = false;
			List<Type> types = input.getTypes();
			if(foundObjectInput) {
				addInput(input, inputsNode);
				continue;
			}
			for (Type type : types) {
				if(type.getTypeDefinition().equals(TYPE_DEFINITION_OBJECT)) {
					addObjectInput(input, inputsNode);
					foundObjectInput = true;
					inputIsObject = true;
					continue;
				}
			}
			if(!inputIsObject) {
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
		executeNode.set("id", new TextNode(echoProcessId));
		boolean foundObjectInput = false;
		for (Input input : inputs) {
			boolean inputIsObject = false;
			List<Type> types = input.getTypes();
			if(foundObjectInput) {
				addInput(input, inputsNode);
				continue;
			}
			for (Type type : types) {
				if(type.getTypeDefinition().equals(TYPE_DEFINITION_OBJECT)) {
					addObjectInput(input, inputsNode);
					foundObjectInput = true;
					inputIsObject = true;
					continue;
				}
			}
			if(!inputIsObject) {
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

	private JsonNode createExecuteJsonNodeWithBinaryInput(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		executeNode.set("id", new TextNode(echoProcessId));
		boolean foundObjectInput = false;
		for (Input input : inputs) {
			boolean inputIsObject = false;
			List<Type> types = input.getTypes();
			if(foundObjectInput) {
				addInput(input, inputsNode);
				continue;
			}
			for (Type type : types) {
				if(type.getTypeDefinition().equals(TYPE_DEFINITION_OBJECT)) {
					addObjectInput(input, inputsNode);
					foundObjectInput = true;
					inputIsObject = true;
					continue;
				}
			}
			if(!inputIsObject) {
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

	private JsonNode createExecuteJsonNodeWithMixedInput(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		executeNode.set("id", new TextNode(echoProcessId));
		boolean foundObjectInput = false;
		for (Input input : inputs) {
			boolean inputIsObject = false;
			List<Type> types = input.getTypes();
			if(foundObjectInput) {
				addInput(input, inputsNode);
				continue;
			}
			for (Type type : types) {
				if(type.getTypeDefinition().equals(TYPE_DEFINITION_OBJECT)) {
					addObjectInput(input, inputsNode);
					foundObjectInput = true;
					inputIsObject = true;
					continue;
				}
			}
			if(!inputIsObject) {
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
	* Abstract Test null: /conf/core/job-creation-input-inline
	* Test Purpose: Validate in-line process input values are validated against the corresponding schema from the process description.
	* Requirement: /req/core/job-creation-input-inline
	* Test Method: 
	* 1.  Verify that each process executes successfully according to the ats-job-creation-success-sync,relevant requirement based on the combination of execute parameters
	* |===
	* TODO: Check additional content
	* </pre>
	*/
	@Test(description = "Implements Requirement /req/core/job-creation-input-inline ", groups = "job")
	public void testJobCreationInputInline() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-creation-input-ref ", groups = "job")
	public void testJobCreationInputRef() {
		//create job
		JsonNode executeNode = createExecuteJsonNodeWithHref(echoProcessId);
		ValidationData<Void> data = new ValidationData<>();
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			Body body = Body.from(responseNode);
			Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
			Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, "/*")
					.build();
			executeValidator.validateResponse(response, data);
			Assert.assertTrue(data.isValid(), printResults(data.results()));			
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	private JsonNode createExecuteJsonNodeWithHref(String echoProcessId2) throws SkipException {
		// TODO Auto-generated method stub
		throw new SkipException("No input with href detected.");
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
	@Test(description = "Implements Requirement /req/core/job-creation-input-validation ", groups = "job")
	public void testJobCreationInputValidation() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		ValidationData<Void> data = new ValidationData<>();
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			Body body = Body.from(responseNode);
			Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
			Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, "/*")
					.build();
			executeValidator.validateResponse(response, data);
			Assert.assertTrue(data.isValid(), printResults(data.results()));			
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
		
		executeNode = createExecuteJsonNodeWithWrongInput(echoProcessId);
		data = new ValidationData<>();
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode, true);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			Body body = Body.from(responseNode);
			Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertEquals(statusCode, HttpStatus.SC_BAD_REQUEST);
			Response response = new DefaultResponse.Builder(statusCode).body(body).header(CONTENT_TYPE, responseContentType.getValue())
					.build();
			executeValidator.validateResponse(response, data);
			Assert.assertTrue(data.isValid(), printResults(data.results()));
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
		
//		Assert.fail("Not implemented yet.");
	}
	
	private HttpResponse sendPostRequestSync(JsonNode executeNode, boolean checkForStatusCode) throws IOException {
		HttpResponse httpResponse = clientBuilder.build().execute(createPostRequest(executeNode));
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(checkForStatusCode) {
		    Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);
		}
		return httpResponse;
	}

	private HttpResponse sendPostRequestSync(JsonNode executeNode) throws IOException {
		return sendPostRequestSync(executeNode, false);
	}
	
	private HttpPost createPostRequest(JsonNode executeNode) {
		HttpPost request = new HttpPost(executeEndpoint);
		request.setHeader("Accept", "application/json");
		ContentType contentType = ContentType.APPLICATION_JSON;
		request.setEntity(new StringEntity(executeNode.toString(), contentType));
		return request;
	}
	
	private HttpResponse sendPostRequestASync(JsonNode executeNode) throws IOException {
		HttpPost request = new HttpPost(executeEndpoint);
		request.setHeader("Accept", "application/json");
		request.setHeader("Prefer", "respond-async ");
		ContentType contentType = ContentType.APPLICATION_JSON;
		request.setEntity(new StringEntity(executeNode.toString(), contentType));
		HttpResponse httpResponse = clientBuilder.build().execute(request);
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);
		return httpResponse;		 
	}

	private JsonNode createExecuteJsonNodeWithWrongInput(String echoProcessId) throws SkipException {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		executeNode.set("id", new TextNode(echoProcessId));
		boolean foundTestableInput = false;
		for (Input input : inputs) {
			if(checkForFormat(input.getTypes()) != null) {
				foundTestableInput = true;
				addInputWithWrongFormat(input, inputsNode);
			}
			addInput(input, inputsNode);
		}
		if(!foundTestableInput) {
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
			if(type.getContentMediaType() != null) {
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
	@Test(description = "Implements Requirement /req/core/job-creation-inputs ", groups = "job")
	public void testJobCreationInputs() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		try {
			HttpResponse httpResponse =sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-creation-op ", groups = "job")
	public void testJobCreationOp() {
		//create job
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
			Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, responseContentType.getValue())
					.build();
			getStatusValidator.validateResponse(response, data);
			Assert.assertTrue(data.isValid(), printResults(data.results()));			
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
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
	@Test(description = "Implements Requirement /req/core/job-creation-request ", groups = "job")
	public void testJobCreationRequest() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		final ValidationData<Void> data = new ValidationData<>();		
		try {
			HttpResponse httpResponse = sendPostRequestASync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);
			Header locationHeader = httpResponse.getFirstHeader("location");
			String locationString = locationHeader.getValue();
			httpResponse = sendGetRequest(locationString, "application/json");
			validateResponse(httpResponse, getStatusValidator, data);
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
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
	@Test(description = "Implements Requirement /req/core/job-creation-success-async ", groups = "job")
	public void testJobCreationSuccessAsync() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		final ValidationData<Void> data = new ValidationData<>();
		try {
			HttpResponse httpResponse = sendPostRequestASync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);
			Header locationHeader = httpResponse.getFirstHeader("location");
			String locationString = locationHeader.getValue();
			httpResponse = sendGetRequest(locationString, "application/json");
			validateResponse(httpResponse, getStatusValidator, data);
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}
	
	private void validateResponse(HttpResponse httpResponse, OperationValidator validator, ValidationData<Void> data) throws IOException {
		JsonNode responseNode = parseResponse(httpResponse);
		Body body = Body.from(responseNode);
		Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
		Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, responseContentType.getValue())
				.build();
		validator.validateResponse(response, data);
		Assert.assertTrue(data.isValid(), printResults(data.results()));		
	}
	
	private JsonNode parseResponse(HttpResponse httpResponse) throws IOException {
		StringWriter writer = new StringWriter();
		String encoding = StandardCharsets.UTF_8.name();
		IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
		return new ObjectMapper().readTree(writer.toString());
	}
	
	private HttpResponse sendGetRequest(String url, String acceptType) throws IOException {		
		HttpGet statusRequest = new HttpGet(url);
		statusRequest.setHeader("Accept", acceptType);
		return clientBuilder.build().execute(statusRequest);
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
	@Test(description = "Implements Requirement /req/core/job-creation-sync-document ", groups = "job")
	public void testJobCreationSyncDocument() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId, RESPONSE_VALUE_DOCUMENT);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-creation-sync-raw-mixed-multi ", groups = "job")
	public void testJobCreationSyncRawMixedMulti() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId, RESPONSE_VALUE_RAW);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-creation-sync-raw-ref ", groups = "job")
	public void testJobCreationSyncRawRef() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId, RESPONSE_VALUE_RAW);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
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
	@Test(description = "Implements Requirement /req/core/job-creation-sync-raw-value-multi ", groups = "job")
	public void testJobCreationSyncRawValueMulti() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId, RESPONSE_VALUE_RAW);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-creation-sync-raw-value-one ", groups = "job")
	public void testJobCreationSyncRawValueOne() {
		// create job
		JsonNode executeNode = createExecuteJsonNodeOneInput(echoProcessId, RESPONSE_VALUE_RAW);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200, "Got unexpected status code: " + statusCode);
			JsonNode responseNode = parseResponse(httpResponse);
			Assert.assertEquals(responseNode.asText(), TEST_STRING_INPUT);
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-exception-no-such-job ", groups = "job")
	public void testJobExceptionNoSuchJob() {
		final ValidationData<Void> data = new ValidationData<>();
		try {
			HttpResponse httpResponse = sendGetRequest(getInvalidJobURL.toString(), "application/json");
			validateResponse(httpResponse, getJobsValidator, data);
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job ", groups = "job")
	public void testJobOp() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		try {			
			HttpClient client = HttpClientBuilder.create().build();
			String executeEndpoint = rootUri + echoProcessPath + "/execution";
			HttpPost request = new HttpPost(executeEndpoint);
			request.setHeader("Accept", "application/json");
			ContentType contentType = ContentType.APPLICATION_JSON;
			request.setEntity(new StringEntity(executeNode.toString(), contentType));
			HttpResponse httpResponse = client.execute(request);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200 || statusCode == 201, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-results-exception-no-such-job ", groups = "job")
	public void testJobResultsNoSuchJob() {
		final ValidationData<Void> data = new ValidationData<>();
		try {
			HttpResponse httpResponse = sendGetRequest(getInvalidJobResultURL.toString(), "application/json");
			validateResponse(httpResponse, getResultValidator, data);
		} catch (Exception e) {
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
	@Test(description = "Implements Requirement /req/core/job-results-exception-results-not-ready ", groups = "job")
	public void testJobResultsExceptionResultsNotReady() {
		Assert.fail("Not implemented yet.");
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
	@Test(description = "Implements Requirement /req/core/job-results-failed ", groups = "job")
	public void testJobResultsFailed() {
		final ValidationData<Void> data = new ValidationData<>();
		//create invalid execute request
		JsonNode executeNode = createInvalidExecuteJsonNode(echoProcessId);
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode > 200, "Got unexpected status code: " + statusCode);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			Body body = Body.from(responseNode);
			Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
			Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, responseContentType.getValue())
					.build();
			executeValidator.validateResponse(response, data);
			Assert.assertTrue(data.isValid(), printResults(data.results()));			
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	private JsonNode createInvalidExecuteJsonNode(String echoProcessId) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		executeNode.set("invalid-execute-request", new TextNode("true)"));
//		executeNode.set("invalid-execute-request", objectMapper.);
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
	@Test(description = "Implements Requirement /req/core/job-results ", groups = "job")
	public void testJobResults() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		final ValidationData<Void> data = new ValidationData<>();		
		try {
			HttpResponse httpResponse = sendPostRequestASync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);
			Header locationHeader = httpResponse.getFirstHeader("location");
			String locationString = locationHeader.getValue();
			httpResponse = sendGetRequest(locationString, "application/json");
			httpResponse = getResultResponse(httpResponse);
			Assert.assertNotNull(httpResponse);
			validateResponse(httpResponse, getResultValidator, data);
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	private HttpResponse getResultResponse(HttpResponse httpResponse) throws IOException {
		JsonNode statusNode = parseResponse(httpResponse);
		JsonNode linksNode = statusNode.get("links");
		Assert.assertNotNull(linksNode);
		Assert.assertTrue(!linksNode.isMissingNode(), "No links in status document.");
		if(linksNode instanceof ArrayNode) {
			ArrayNode linksArrayNode = (ArrayNode)linksNode;
			for (int i = 0; i < linksArrayNode.size(); i++) {
				JsonNode linksChildNode = linksArrayNode.get(i);
				if(linksChildNode.get("rel").asText().equals("results")) {
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
	@Test(description = "Implements Requirement /req/core/job-results-async-document ", groups = "job")
	public void testJobResultsAsyncDocument() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId, RESPONSE_VALUE_DOCUMENT);
		try {
			HttpResponse httpResponse = sendPostRequestASync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);		
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
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
	@Test(description = "Implements Requirement /req/core/job-results-async-raw-mixed-multi ", groups = "job")
	public void testJobResultsAsyncRawMixedMulti() {
		Assert.fail("Not implemented yet.");
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
	@Test(description = "Implements Requirement /req/core/job-results-async-raw-ref ", groups = "job")
	public void testJobResultsAsyncRawRef() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		final ValidationData<Void> data = new ValidationData<>();		
		try {
			HttpResponse httpResponse = sendPostRequestASync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);
			Header locationHeader = httpResponse.getFirstHeader("location");
			String locationString = locationHeader.getValue();
			httpResponse = sendGetRequest(locationString, ContentType.APPLICATION_JSON.getMimeType());
			JsonNode responseNode = parseResponse(httpResponse);
			Body body = Body.from(responseNode);
			Header responseContentType = httpResponse.getFirstHeader(CONTENT_TYPE);
			Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, responseContentType.getValue())
					.build();
			getStatusValidator.validateResponse(response, data);
			Assert.assertTrue(data.isValid(), printResults(data.results()));			
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
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
	@Test(description = "Implements Requirement /req/core/job-results-async-raw-value-multi ", groups = "job")
	public void testJobResultsAsyncRawValueMulti() {
		Assert.fail("Not implemented yet.");
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
	@Test(description = "Implements Requirement /req/core/job-results-async-raw-value-one ", groups = "job")
	public void testJobResultsAsyncRawValueOne() {
		// create job
		JsonNode executeNode = createExecuteJsonNodeOneInput(echoProcessId, RESPONSE_VALUE_RAW);
		try {
			HttpResponse httpResponse = sendPostRequestASync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 201, "Got unexpected status code: " + statusCode);
			JsonNode responseNode = parseResponse(httpResponse);
			Assert.assertEquals(responseNode.asText(), TEST_STRING_INPUT);
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

	private JsonNode createExecuteJsonNodeOneInput(String echoProcessId, String responseValue) {
		ObjectNode executeNode = objectMapper.createObjectNode();
		ObjectNode inputsNode = objectMapper.createObjectNode();
		ObjectNode outputsNode = objectMapper.createObjectNode();
		executeNode.set("id", new TextNode(echoProcessId));
		Input inputOne = inputs.get(0);
		String inputId = inputOne.getId();
		addInput(inputOne, inputsNode);
		for (Output output : outputs) {
			if(output.getId().equals(inputId)) {
			    addOutput(output, outputsNode);
			    break;
			}
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
	@Test(description = "Implements Requirement /req/core/job-results-sync ", groups = "job")
	public void testJobResultsSync() {
		//create job
		JsonNode executeNode = createExecuteJsonNode(echoProcessId);
		final ValidationData<Void> data = new ValidationData<>();
		try {
			HttpResponse httpResponse = sendPostRequestSync(executeNode);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			Assert.assertTrue(statusCode == 200, "Got unexpected status code: " + statusCode);
			Header[] headers = httpResponse.getHeaders("Link");
			boolean foundRelMonitorHeader = false;
			String statusUrl = "";
			for (Header header : headers) {
//				HeaderElement[] headerElements = header.getElements();
//				for (HeaderElement headerElement : headerElements) {
				String heaerValue = header.getValue();
					if(heaerValue.contains("rel=monitor")) {
						foundRelMonitorHeader = true;
						statusUrl = heaerValue.split(";")[0];
						break;
					}
//				}
			}
			if(!foundRelMonitorHeader) {
				throw new SkipException("Did not find Link with value rel=monitor, skipping test.");
			}
			httpResponse = sendGetRequest(statusUrl, ContentType.APPLICATION_JSON.getMimeType());
			validateResponse(httpResponse, getResultValidator, data);
		} catch (IOException e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

//	/**
//	* <pre>
//	* </pre>
//	*/
//	@Test(description = "Implements Requirement  ", groups = "")
//	public void () {
//
//	}

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
	@Test(description = "Implements Requirement /req/core/job-success ", groups = "job")
	public void testJobSuccess() {
		//create job
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
			Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, responseContentType.getValue())
					.build();
			getStatusValidator.validateResponse(response, data);
			Assert.assertTrue(data.isValid(), printResults(data.results()));			
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}
}
