package org.opengis.cite.ogcapiprocesses10.ogcprocessdescription;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
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
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
*
* A.3 OGC Process Description
*
* @author <a href="mailto:b.pross@52north.org">Benjamin Pross </a>
*/
public class OGCProcessDescription extends CommonFixture {

	private static final String OPERATION_ID = "getProcessDescription";

	private OpenApi3 openApi3;
	
	private String getProcessListPath = "/processes";
	
	private OperationValidator validator;
    
    private URL getProcessListURL;
    
    private String echoProcessId;
    private int processTestLimit = 1; //this is a minimum
    private final int maximumNumberOfProcessesTested = 5;  
    
	@BeforeClass
	public void setup(ITestContext testContext) {		
		String processListEndpointString = rootUri.toString() + getProcessListPath;		
		try {
			echoProcessId = (String) testContext.getSuite().getAttribute( SuiteAttribute.ECHO_PROCESS_ID.getName() );
			
			processTestLimit = (Integer) testContext.getSuite().getAttribute( SuiteAttribute.PROCESS_TEST_LIMIT.getName() );
		
			openApi3 = new OpenApi3Parser().parse(specURI.toURL(), false);
			addServerUnderTest(openApi3);
		    final Path path = openApi3.getPathItemByOperationId(OPERATION_ID);
		    final Operation operation = openApi3.getOperationById(OPERATION_ID);
		    validator = new OperationValidator(openApi3, path, operation);
		    getProcessListURL = new URL(processListEndpointString);
		} catch (MalformedURLException | ResolutionException | ValidationException e) {
			Assert.fail("Could set up endpoint: " + processListEndpointString + ". Exception: " + e.getLocalizedMessage());
		}
	}


	/**
	 * <pre>
	 * Abstract Test 48: /conf/ogc-process-description/json-encoding
	 * Test Purpose: Verify that a JSON-encoded OGC Process Description complies with the required structure and contents.
	 * Requirement: /req/ogc-process-description/json-encoding
	 * Test Method: 
	 * |===
	 * 1. Retrieve a description of each process according to test /conf/core/process.
     *
     * 2. For each process, verify the contents of the response body validate against the JSON Schema: process.yaml.
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/ogc-process-description/json-encoding", groups = "ogcprocessdescription")
	public void testOGCProcessDescriptionJSON() {
		final ValidationData<Void> data = new ValidationData<>();
		try {
		
			
			{
			HttpClient client = HttpClientBuilder.create().build();
			HttpUriRequest request = new HttpGet(getProcessListURL.toString());
			request.setHeader("Accept", "application/json");
		    this.reqEntity = request;
			HttpResponse httpResponse = client.execute(request);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			JsonNode processesNode = responseNode.get("processes");
			if(processesNode.isArray()) {
				ArrayNode processesArrayNode = (ArrayNode)processesNode;
				if(testAllProcesses) {
			
					for (int i=0; i < Math.min(processTestLimit,maximumNumberOfProcessesTested); i++) {  //we intentionally limit this
						
						JsonNode jsonNode = processesArrayNode.get(i);
					
							HttpClient client2 = HttpClientBuilder.create().build();
							HttpUriRequest request2 = new HttpGet(getProcessListURL.toString()+"/"+jsonNode.get("id").textValue());
							request2.setHeader("Accept", "application/json");
							  this.reqEntity = request2;
							HttpResponse httpResponse2 = client2.execute(request2);
							StringWriter writer2 = new StringWriter();
							String encoding2 = StandardCharsets.UTF_8.name();
							IOUtils.copy(httpResponse2.getEntity().getContent(), writer2, encoding2);
							JsonNode responseNode2 = new ObjectMapper().readTree(writer2.toString());
							Body body2 = Body.from(responseNode2);
							Header contentType2 = httpResponse2.getFirstHeader(CONTENT_TYPE);
							Response response2 = new DefaultResponse.Builder(httpResponse2.getStatusLine().getStatusCode()).body(body2).header(CONTENT_TYPE, contentType2.getValue())
									.build();
							validator.validateResponse(response2, data);
							Assert.assertTrue(data.isValid(), printResults(data.results()));	
		
					}
				}
				else { //test echo process only
				
					HttpClient client2 = HttpClientBuilder.create().build();
					HttpUriRequest request2 = new HttpGet(getProcessListURL.toString()+"/"+echoProcessId);
					request2.setHeader("Accept", "application/json");
					  this.reqEntity = request2;
					HttpResponse httpResponse2 = client2.execute(request2);
					StringWriter writer2 = new StringWriter();
					String encoding2 = StandardCharsets.UTF_8.name();
					IOUtils.copy(httpResponse2.getEntity().getContent(), writer2, encoding2);
					JsonNode responseNode2 = new ObjectMapper().readTree(writer2.toString());
					Body body2 = Body.from(responseNode2);
					Header contentType2 = httpResponse2.getFirstHeader(CONTENT_TYPE);
					Response response2 = new DefaultResponse.Builder(httpResponse2.getStatusLine().getStatusCode()).body(body2).header(CONTENT_TYPE, contentType2.getValue())
							.build();
					validator.validateResponse(response2, data);
					Assert.assertTrue(data.isValid(), printResults(data.results()));
				}
			}
			}
			
			
			
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}
	
	/**
	 * <pre>
	 * Abstract Test 49: /conf/ogc-process-description/inputs-def
	 * Test Purpose: Verify that the definition of inputs for each process complies with the required structure and contents.
	 * Requirement: /req/ogc-process-description/inputs-def
	 * Test Method: 
	 * |===
     * 1. Retrieve a description of each process according to test /conf/core/process.
     *
     * 2. For each process, verify that the definition of the inputs conforms to the JSON Schema: inputDescription.yaml.
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/ogc-process-description/inputs-def", groups = "ogcprocessdescription", dependsOnMethods = { "testOGCProcessDescriptionJSON" })
	public void testOGCProcessDescriptionInputsDef() {
		//This test depends on the testOGCProcessDescriptionJSON method.
		//Whereas testOGCProcessDescriptionJSON validates process descriptions, the testOGCProcessDescriptionInputsDef test confirms that the validated processes had Inputs defined in them.
		final ValidationData<Void> data = new ValidationData<>();
		try {
		
			
			{
			HttpClient client = HttpClientBuilder.create().build();
			HttpUriRequest request = new HttpGet(getProcessListURL.toString());
			request.setHeader("Accept", "application/json");
		    this.reqEntity = request;
			HttpResponse httpResponse = client.execute(request);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			JsonNode processesNode = responseNode.get("processes");
			if(processesNode.isArray()) {
				ArrayNode processesArrayNode = (ArrayNode)processesNode;
				if(testAllProcesses) {
			
					for (int i=0; i < Math.min(processTestLimit,maximumNumberOfProcessesTested); i++) {  //we intentionally limit this
						
						JsonNode jsonNode = processesArrayNode.get(i);
					
						checkProcessDescriptionHasInputsField(getProcessListURL.toString(),jsonNode.get("id").textValue());
					}
				}
				else { //test echo process only
	
					checkProcessDescriptionHasInputsField(getProcessListURL.toString(),echoProcessId);
				}
			}
			}
			
			
			
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}		
	}

	/**
	 * <pre>
	 * Abstract Test 50: /conf/ogc-process-description/input-def
	 * Test Purpose: Verify that the definition of each input for each process complies with the required structure and contents.
	 * Requirement: /req/ogc-process-description/input-def
	 * Test Method: 
	 * |===
	 * For each input identified according to the test /conf/ogc-process-description/inputs-def verify that the value of the schema key, that defines the input, validates according to the JSON Schema: schema.yaml.
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/ogc-process-description/input-def", groups = "ogcprocessdescription", dependsOnMethods = { "testOGCProcessDescriptionJSON" })
	public void testOGCProcessDescriptionInputDef() {
		//This test depends on the testOGCProcessDescriptionJSON method.
		//Whereas testOGCProcessDescriptionJSON validates process descriptions, the testOGCProcessDescriptionInputDef test confirms that the validated processes have valid schemas defined in them.
		final ValidationData<Void> data = new ValidationData<>();
		try {
		
			
			{
			HttpClient client = HttpClientBuilder.create().build();
			HttpUriRequest request = new HttpGet(getProcessListURL.toString());
			request.setHeader("Accept", "application/json");
		    this.reqEntity = request;
			HttpResponse httpResponse = client.execute(request);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			JsonNode processesNode = responseNode.get("processes");
			if(processesNode.isArray()) {
				ArrayNode processesArrayNode = (ArrayNode)processesNode;
				if(testAllProcesses) {
			
					for (int i=0; i < Math.min(processTestLimit,maximumNumberOfProcessesTested); i++) {  //we intentionally limit this to 5
						
						JsonNode jsonNode = processesArrayNode.get(i);						
						checkEachInputDescriptionOfProcessHasSchema(getProcessListURL.toString(),jsonNode.get("id").textValue(),false);
		
					}
				}
				else { //test echo process only
				
					checkEachInputDescriptionOfProcessHasSchema(getProcessListURL.toString(),echoProcessId,false);
									
				}
			}
			}
			
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}		
	}
	

	/**
	 * <pre>
	 * Abstract Test 51: /conf/ogc-process-description/input-mixed-type
	 * Test Purpose: Validate that each input of mixed type complies with the required structure and contents.
	 * Requirement: /req/ogc-process-description/input-mixed-type
	 * Test Method: 
	 * |===
     * 1. Retrieve a description of each process according to test /conf/core/process.
	 * 
     * 2. For each process identify if the process has one or more inputs of mixed type.
	 * 
     * 3. For each sub-schema of each identified input, verify that the definition validates according to the JSON Schema: schema.yaml.
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/ogc-process-description/input-mixed-type", groups = "ogcprocessdescription", dependsOnMethods = { "testOGCProcessDescriptionJSON" })
	public void testOGCProcessDescriptionInputDefinitionOfMixedType() {
		//This test depends on the testOGCProcessDescriptionJSON method.
		//Whereas testOGCProcessDescriptionJSON validates process descriptions, the testOGCProcessDescriptionInputDefinitionOfMixedType test confirms that the validated processes have valid schemas defined in them.
		final ValidationData<Void> data = new ValidationData<>();
		try {
			
			{
			HttpClient client = HttpClientBuilder.create().build();
			HttpUriRequest request = new HttpGet(getProcessListURL.toString());
			request.setHeader("Accept", "application/json");
		    this.reqEntity = request;
			HttpResponse httpResponse = client.execute(request);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			JsonNode processesNode = responseNode.get("processes");
			if(processesNode.isArray()) {
				ArrayNode processesArrayNode = (ArrayNode)processesNode;
				if(testAllProcesses) {
			
					for (int i=0; i < Math.min(processTestLimit,maximumNumberOfProcessesTested); i++) {  //we intentionally limit this to 5
						
						JsonNode jsonNode = processesArrayNode.get(i);						
						checkEachInputDescriptionOfProcessHasSchema(getProcessListURL.toString(),jsonNode.get("id").textValue(),true);
		
					}
				}
				else { //test echo process only
				
					checkEachInputDescriptionOfProcessHasSchema(getProcessListURL.toString(),echoProcessId,true);
									
				}
			}
			}
			
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}		
	}
	

	/**
	 * <pre>
	 * Abstract Test 52: /conf/ogc-process-description/outputs-def
	 * Test Purpose: Verify that the definition of outputs for each process complies with the required structure and contents.
	 * Requirement: /req/ogc-process-description/outputs-def
	 * Test Method: 
	 * |===
     * 1. Retrieve a description of each process according to test /conf/core/process.
     *
     * 2. For each process, verify that the definition of the outputs conforms to the JSON Schema: outputDescription.yaml.
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/ogc-process-description/outputs-def", groups = "ogcprocessdescription")
	public void testProcessListSuccess() {
		final ValidationData<Void> data = new ValidationData<>();
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpUriRequest request = new HttpGet(getProcessListURL.toString());
			request.setHeader("Accept", "application/json");
		    this.reqEntity = request;
			HttpResponse httpResponse = client.execute(request);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			Body body = Body.from(responseNode);
			Header contentType = httpResponse.getFirstHeader(CONTENT_TYPE);
			Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, contentType.getValue())
					.build();
			validator.validateResponse(response, data);
			Assert.assertTrue(data.isValid(), printResults(data.results()));
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}
	
	/**
	 * <pre>
	 * Abstract Test 52: /conf/ogc-process-description/outputs-def
	 * Test Purpose: Verify that the definition of outputs for each process complies with the required structure and contents.
	 * Requirement: /req/ogc-process-description/outputs-def
	 * Test Method: 
	 * |===
	   * 1. Retrieve a description of each process according to test /conf/core/process.
	   *
	   * 2. For each process, verify that the definition of the outputs conforms to the JSON Schema: outputDescription.yaml.
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/ogc-process-description/outputs-def", groups = "ogcprocessdescription", dependsOnMethods = { "testOGCProcessDescriptionJSON" })
	public void testOGCProcessDescriptionOutputsDef() {
	  //This test depends on the testOGCProcessDescriptionJSON method.
	  //Whereas testOGCProcessDescriptionJSON validates process descriptions, the testOGCProcessDescriptionOutputsDef test confirms that the validated processes have Outputs defined in them.
	  final ValidationData<Void> data = new ValidationData<>();
	  try {
	  
	    
	    {
	    HttpClient client = HttpClientBuilder.create().build();
	    HttpUriRequest request = new HttpGet(getProcessListURL.toString());
	    request.setHeader("Accept", "application/json");
	      this.reqEntity = request;
	    HttpResponse httpResponse = client.execute(request);
	    StringWriter writer = new StringWriter();
	    String encoding = StandardCharsets.UTF_8.name();
	    IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
	    JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
	    JsonNode processesNode = responseNode.get("processes");
	    if(processesNode.isArray()) {
	      ArrayNode processesArrayNode = (ArrayNode)processesNode;
	      if(testAllProcesses) {
	    
	        for (int i=0; i < Math.min(processTestLimit,maximumNumberOfProcessesTested); i++) {  //we intentionally limit this
	          
	          JsonNode jsonNode = processesArrayNode.get(i);
	        
	          checkProcessDescriptionHasOutputsField(getProcessListURL.toString(),jsonNode.get("id").textValue());
	        }
	      }
	      else { //test echo process only

	        checkProcessDescriptionHasOutputsField(getProcessListURL.toString(),echoProcessId);
	      }
	    }
	    }
	    
	    
	    
	  } catch (Exception e) {
	    Assert.fail(e.getLocalizedMessage());
	  }		
	}
	

	/**
	 * <pre>
	 * Abstract Test 53: /conf/ogc-process-description/output-def
	 * Test Purpose: Verify that the definition of each output for each process complies with the required structure and contents.
	 * Requirement: /req/ogc-process-description/output-def
	 * Test Method: 
	 * |===
	 * For each output identified according to the test /conf/ogc-process-description/outputs-def verify that the value of the schema key, that defines the output, validates according to the JSON Schema: schema.yaml.
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/ogc-process-description/output-def ", groups = "ogcprocessdescription", dependsOnMethods = { "testOGCProcessDescriptionJSON" })
	public void testOGCProcessDescriptionOutputDef() {
		//This test depends on the testOGCProcessDescriptionJSON method.
		//Whereas testOGCProcessDescriptionJSON validates process descriptions, the testOGCProcessDescriptionOutputDef test confirms that the validated processes have valid schemas defined in them.
		final ValidationData<Void> data = new ValidationData<>();
		try {
		  
		  {
		  HttpClient client = HttpClientBuilder.create().build();
		  HttpUriRequest request = new HttpGet(getProcessListURL.toString());
		  request.setHeader("Accept", "application/json");
		    this.reqEntity = request;
		  HttpResponse httpResponse = client.execute(request);
		  StringWriter writer = new StringWriter();
		  String encoding = StandardCharsets.UTF_8.name();
		  IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
		  JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
		  JsonNode processesNode = responseNode.get("processes");
		  if(processesNode.isArray()) {
		    ArrayNode processesArrayNode = (ArrayNode)processesNode;
		    if(testAllProcesses) {
		  
		      for (int i=0; i < Math.min(processTestLimit,maximumNumberOfProcessesTested); i++) {  //we intentionally limit this to 5
		        
		        JsonNode jsonNode = processesArrayNode.get(i);						
		        checkEachOutputDescriptionOfProcessHasSchema(getProcessListURL.toString(),jsonNode.get("id").textValue(),false);

		      }
		    }
		    else { //test echo process only
		    
		      checkEachOutputDescriptionOfProcessHasSchema(getProcessListURL.toString(),echoProcessId,false);
		              
		    }
		  }
		  }
		  
		} catch (Exception e) {
		  Assert.fail(e.getLocalizedMessage());
		}		

	}
	
	/**
	 * <pre>
	 * Abstract Test 54: /conf/ogc-process-description/output-mixed-type
	 * Test Purpose: Validate that each output of mixed type complies with the required structure and contents.
	 * Requirement: /req/ogc-process-description/output-mixed-type
	 * Test Method: 
	 * |===
	   * 1. Retrieve a description of each process according to test /conf/core/process.
	 * 
	   * 2. For each process identify if the process has one or more output of mixed type denoted by the use of the oneOf JSON Schema keyword.
	 * 
	   * 3. For each sub-schema or each identified output, verify that the definition validates according to the JSON Schema: schema.yaml.
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/ogc-process-description/output-mixed-type", groups = "ogcprocessdescription", dependsOnMethods = { "testOGCProcessDescriptionJSON" })
	public void testOGCProcessDescriptionOutputDefinitionOfMixedType() {
	  //This test depends on the testOGCProcessDescriptionJSON method.
	  //Whereas testOGCProcessDescriptionJSON validates process descriptions, the testOGCProcessDescriptionOutputDefinitionOfMixedType test confirms that the validated processes have valid schemas defined in them.
	  final ValidationData<Void> data = new ValidationData<>();
	  try {
	    
	    {
	    HttpClient client = HttpClientBuilder.create().build();
	    HttpUriRequest request = new HttpGet(getProcessListURL.toString());
	    request.setHeader("Accept", "application/json");
	      this.reqEntity = request;
	    HttpResponse httpResponse = client.execute(request);
	    StringWriter writer = new StringWriter();
	    String encoding = StandardCharsets.UTF_8.name();
	    IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
	    JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
	    JsonNode processesNode = responseNode.get("processes");
	    if(processesNode.isArray()) {
	      ArrayNode processesArrayNode = (ArrayNode)processesNode;
	      if(testAllProcesses) {
	    
	        for (int i=0; i < Math.min(processTestLimit,maximumNumberOfProcessesTested); i++) {  //we intentionally limit this to 5
	          
	          JsonNode jsonNode = processesArrayNode.get(i);						
	          checkEachOutputDescriptionOfProcessHasSchema(getProcessListURL.toString(),jsonNode.get("id").textValue(),true);
	  
	        }
	      }
	      else { //test echo process only
	      
	        checkEachOutputDescriptionOfProcessHasSchema(getProcessListURL.toString(),echoProcessId,true);
	                
	      }
	    }
	    }
	    
	  } catch (Exception e) {
	    Assert.fail(e.getLocalizedMessage());
	  }		
	}
	

	/*
	 * This method is used by the testOGCProcessDescriptionOutputsDef method.
	 */	
	private void checkProcessDescriptionHasOutputsField(String processListURL,String processIdentifier) throws ClientProtocolException, IOException
	{
	  HttpClient client2 = HttpClientBuilder.create().build();
	  HttpUriRequest request2 = new HttpGet(processListURL+"/"+processIdentifier);
	  request2.setHeader("Accept", "application/json");
	    this.reqEntity = request2;
	  HttpResponse httpResponse2 = client2.execute(request2);
	  StringWriter writer2 = new StringWriter();
	  String encoding2 = StandardCharsets.UTF_8.name();
	  IOUtils.copy(httpResponse2.getEntity().getContent(), writer2, encoding2);
	  JsonNode responseNode2 = new ObjectMapper().readTree(writer2.toString());
	  Body body2 = Body.from(responseNode2);
	  Header contentType2 = httpResponse2.getFirstHeader(CONTENT_TYPE);
	  Response response2 = new DefaultResponse.Builder(httpResponse2.getStatusLine().getStatusCode()).body(body2).header(CONTENT_TYPE, contentType2.getValue())
	      .build();
	  Assert.assertTrue(responseNode2.has("outputs"), "No 'outputs' field was found in the process description of '"+processIdentifier+"'. ");	

	}
	
	/*
	 * This method is used by the testOGCProcessDescriptionInputsDef method.
	 */	
	private void checkProcessDescriptionHasInputsField(String processListURL,String processIdentifier) throws ClientProtocolException, IOException
	{
		HttpClient client2 = HttpClientBuilder.create().build();
		HttpUriRequest request2 = new HttpGet(processListURL+"/"+processIdentifier);
		request2.setHeader("Accept", "application/json");
		  this.reqEntity = request2;
		HttpResponse httpResponse2 = client2.execute(request2);
		StringWriter writer2 = new StringWriter();
		String encoding2 = StandardCharsets.UTF_8.name();
		IOUtils.copy(httpResponse2.getEntity().getContent(), writer2, encoding2);
		JsonNode responseNode2 = new ObjectMapper().readTree(writer2.toString());
		Body body2 = Body.from(responseNode2);
		Header contentType2 = httpResponse2.getFirstHeader(CONTENT_TYPE);
		Response response2 = new DefaultResponse.Builder(httpResponse2.getStatusLine().getStatusCode()).body(body2).header(CONTENT_TYPE, contentType2.getValue())
				.build();
		Assert.assertTrue(responseNode2.has("inputs"), "No 'inputs' field was found in the process description of '"+processIdentifier+"'. ");	
	
	}
	
	/*
	 * This method is used by the testOGCProcessDescriptionInputDef and testOGCProcessDescriptionInputDefinitionOfMixedType methods.
	 */
	private void checkEachInputDescriptionOfProcessHasSchema(String processListURL,String processIdentifier, boolean checkHasMixedTypeInput) throws ClientProtocolException, IOException
	{
		HttpClient client3 = HttpClientBuilder.create().build();
		HttpUriRequest request3 = new HttpGet(processListURL+"/"+processIdentifier);
		request3.setHeader("Accept", "application/json");
		  this.reqEntity = request3;
		HttpResponse httpResponse3 = client3.execute(request3);
		StringWriter writer3 = new StringWriter();
		String encoding3 = StandardCharsets.UTF_8.name();
		IOUtils.copy(httpResponse3.getEntity().getContent(), writer3, encoding3);
		JsonNode responseNode3 = new ObjectMapper().readTree(writer3.toString());
		Body body3 = Body.from(responseNode3);
		Header contentType3 = httpResponse3.getFirstHeader(CONTENT_TYPE);
		Response response3 = new DefaultResponse.Builder(httpResponse3.getStatusLine().getStatusCode()).body(body3).header(CONTENT_TYPE, contentType3.getValue())
		    .build();
		JsonNode inputsNode3 = responseNode3.get("inputs");							
		Iterator inputsIter3 = inputsNode3.elements();	
		boolean atLeastOneMixedTypeInputFound = false;
		while(inputsIter3.hasNext()) {
		  ObjectNode input = (ObjectNode) inputsIter3.next();	
		  if(input.has("schema"))
		  {
			  if(checkHasMixedTypeInput) {
				  JsonNode schemaNode3 = input.get("schema");	
				  if(schemaNode3.has("oneOf")) {
					  atLeastOneMixedTypeInputFound = true;
				  }
			  }
		  }
		  else {
		    // fail if there is no schema element
		    Assert.assertTrue(input.has("schema"), "At least one Input element in the process description of '"+processIdentifier+"' is missing a 'schema' element. ");								
		  }
		}
		
		if(checkHasMixedTypeInput) {
			Assert.assertTrue(atLeastOneMixedTypeInputFound,"No Input definition of '"+processIdentifier+"' process identifies a Mixed Type input");
		}
	}
	
	
	/*
	 * This method is used by the testOGCProcessDescriptionOutputDef and testOGCProcessDescriptionOutputDefinitionOfMixedType methods.
	 */
	private void checkEachOutputDescriptionOfProcessHasSchema(String processListURL,String processIdentifier, boolean checkHasMixedTypeOutput) throws ClientProtocolException, IOException
	{
		HttpClient client3 = HttpClientBuilder.create().build();
		HttpUriRequest request3 = new HttpGet(processListURL+"/"+processIdentifier);
		request3.setHeader("Accept", "application/json");
		  this.reqEntity = request3;
		HttpResponse httpResponse3 = client3.execute(request3);
		StringWriter writer3 = new StringWriter();
		String encoding3 = StandardCharsets.UTF_8.name();
		IOUtils.copy(httpResponse3.getEntity().getContent(), writer3, encoding3);
		JsonNode responseNode3 = new ObjectMapper().readTree(writer3.toString());
		Body body3 = Body.from(responseNode3);
		Header contentType3 = httpResponse3.getFirstHeader(CONTENT_TYPE);
		Response response3 = new DefaultResponse.Builder(httpResponse3.getStatusLine().getStatusCode()).body(body3).header(CONTENT_TYPE, contentType3.getValue())
		    .build();
		JsonNode outputsNode3 = responseNode3.get("outputs");							
		Iterator outputsIter3 = outputsNode3.elements();	
		boolean atLeastOneMixedTypeOutputFound = false;
		while(outputsIter3.hasNext()) {
		  ObjectNode output = (ObjectNode) outputsIter3.next();	
		  if(output.has("schema"))
		  {
			  if(checkHasMixedTypeOutput) {
				  JsonNode schemaNode3 = output.get("schema");	
				  if(schemaNode3.has("oneOf")) {
					  atLeastOneMixedTypeOutputFound = true;
				  }
			  }
		  }
		  else {
		    // fail if there is no schema element
		    Assert.assertTrue(output.has("schema"), "At least one Output element in the process description of '"+processIdentifier+"' is missing a 'schema' element. ");								
		  }
		}
		
		if(checkHasMixedTypeOutput) {
			Assert.assertTrue(atLeastOneMixedTypeOutputFound,"No Output definition of '"+processIdentifier+"' process identifies a Mixed Type Output");
		}
	}	
}
