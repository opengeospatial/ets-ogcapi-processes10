/**
 * 
 */
package org.opengis.cite.ogcapiprocesses10.process;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.operation.validator.model.Request;
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
import org.opengis.cite.ogcapiprocesses10.util.PathSettingRequest;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * A.2.5. List of processes {root}/processes
 *
 * @author <a href="mailto:b.pross@52north.org">Benjamin Pross </a>
 */
public class Process extends CommonFixture {

    private static final String OPERATION_ID_GET_PROCESS_DESCRIPTION = "getProcessDescription";

    private OpenApi3 openApi3;
	
    private String getProcessListPath = "/processes";
	
    private OperationValidator getProcessDescriptionValidator;
    
    private URL getInvalidProcessURL;
    
    private String echoProcessId;

    private String echoProcessPath;
    
    @BeforeClass
    public void setup(ITestContext testContext) {		
	String processListEndpointString = rootUri.toString() + getProcessListPath;		
	try {
	    openApi3 = new OpenApi3Parser().parse(specURI.toURL(), false);
	    addServerUnderTest(openApi3);
	    final Path path = openApi3.getPathItemByOperationId(OPERATION_ID_GET_PROCESS_DESCRIPTION);
	    final Operation operation = openApi3.getOperationById(OPERATION_ID_GET_PROCESS_DESCRIPTION);
	    getProcessDescriptionValidator = new OperationValidator(openApi3, path, operation);
	    getInvalidProcessURL = new URL(processListEndpointString + "/invalid-process-" + UUID.randomUUID());
	} catch (MalformedURLException | ResolutionException | ValidationException e) {
	    Assert.fail("Could set up endpoint: " + processListEndpointString + ". Exception: " + e.getLocalizedMessage());
	}
	echoProcessId = (String) testContext.getSuite().getAttribute( SuiteAttribute.ECHO_PROCESS_ID.getName() );
	echoProcessPath = getProcessListPath + "/" + echoProcessId;
    }
	
    /**
     * <pre>
     * Abstract Test 15: /conf/core/process-exception-no-such-process
     * Test Purpose: Validate that an invalid process identifier is handled correctly.
     * Requirement: /req/core/process-exception-no-such-process
     * Test Method: 
     * 1.  Validate that the document contains the exception `type` "http://wwwopengisnet/def/exceptions/ogcapi-processes-1/10/no-such-process"
     * 2.  Validate the document for all supported media types using the resources and tests identified in no-such-process
     * |===
     * 
     * An exception response caused by the use of an invalid process identifier may be retrieved in a number of different formats. The following table identifies the applicable schema document for each format and the test to be used to validate the response. All supported formats should be exercised.
     * 
     * [[no-such-process]]
     * 3. Schema and Tests for Non-existent Process
     * [width="90%",cols="3",options="header"]
     * |===
     * |Format |Schema Document |Test ID
     * |HTML |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/exception.yaml[exception.yaml] |ats_html_content,/conf/html/content
     * |JSON |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/exception.yaml[exception.yaml] |ats_json_content,/conf/json/content
     * |===
     * TODO: Check additional content
     * </pre>
     */
    @Test(description = "Implements Requirement /req/core/process-exception-no-such-process ", groups = "process")
    public void testProcessExceptionNoSuchProcess() {
	final ValidationData<Void> data = new ValidationData<>();
	try {
	    HttpClient client = HttpClientBuilder.create().build();
	    HttpUriRequest request = new HttpGet(getInvalidProcessURL.toString());
	    this.reqEntity = request;
	    request.setHeader("Accept", "application/json");
	    HttpResponse httpResponse = client.execute(request);
	    StringWriter writer = new StringWriter();
	    String encoding = StandardCharsets.UTF_8.name();
	    IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
	    JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
	    Body body = Body.from(responseNode);
	    Header contentType = httpResponse.getFirstHeader(CONTENT_TYPE);
	    Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, contentType.getValue())
		.build();
	    getProcessDescriptionValidator.validateResponse(response, data);
	    Assert.assertTrue(data.isValid(), printResults(data.results()));
	} catch (Exception e) {
	    e.printStackTrace();
	    Assert.fail(e.getLocalizedMessage());
	}
    }

    /**
     * <pre>
     * Abstract Test 13: /conf/core/process
     * Test Purpose: Validate that a process description can be retrieved from the expected location.
     * Requirement: /req/core/process
     * Test Method: 
     * |===
     * TODO: Check additional content
     * </pre>
     */
    @Test(description = "Implements Requirement /req/core/process ", groups = "process")
    public void testProcess() {
	final ValidationData<Void> data = new ValidationData<>();
	try {
	    //Request request = new PathSettingRequest(rootUri.toString(), echoProcessPath, Request.Method.GET);
	    //getProcessDescriptionValidator.validatePath(request, data);

	    HttpClient client = HttpClientBuilder.create().build();
	    HttpUriRequest request = new HttpGet(rootUri.toString()+echoProcessPath);
	    this.reqEntity = request;
	    request.setHeader("Accept", "application/json");
	    HttpResponse httpResponse = client.execute(request);
	    StringWriter writer = new StringWriter();
	    String encoding = StandardCharsets.UTF_8.name();
	    IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
	    JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
	    Body body = Body.from(responseNode);
	    Header contentType = httpResponse.getFirstHeader(CONTENT_TYPE);
	    Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, contentType.getValue())
		.build();
	    getProcessDescriptionValidator.validateResponse(response, data);
	    Assert.assertTrue(data.isValid(), printResults(data.results()));
	} catch (Exception e) {
	    e.printStackTrace();
	    Assert.fail("Could not validate path: " + echoProcessPath + "\n" + printResults(data.results()));
	}
    }

    /**
     * <pre>
     * Abstract Test 14: /conf/core/process-success
     * Test Purpose: Validate that the content complies with the required structure and contents.
     * Requirement: /req/core/process-success
     * Test Method: 
     * |===
     * 
     * The interface of a process may be describing using a number of different models or process description languages. The following table identifies the applicable schema document for each process description model described in this standard.
     * 
     * [[process-description-model]]
     * 1. Schema and Tests for Process Description Models
     * [width="90%",cols="3",options="header"]
     * |===
     * |Model |Schema Document |Test ID
     * |OGC Process Description JSON|link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/process.yaml[process.yaml] |req_ogc-process-description_json-encoding,/conf/ogc-process-description/json-encoding
     * |===
     * TODO: Check additional content
     * </pre>
     */
    @Test(description = "Implements Requirement /req/core/process-success ", groups = "process")
    public void testProcessSuccess() {
	final ValidationData<Void> data = new ValidationData<>();
	try {
	    HttpClient client = HttpClientBuilder.create().build();
	    HttpUriRequest request = new HttpGet(rootUri + echoProcessPath);
	    this.reqEntity = request;
	    request.setHeader("Accept", "application/json");
	    HttpResponse httpResponse = client.execute(request);
	    StringWriter writer = new StringWriter();
	    String encoding = StandardCharsets.UTF_8.name();
	    IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
	    JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
	    Body body = Body.from(responseNode);
	    Header contentType = httpResponse.getFirstHeader(CONTENT_TYPE);
	    Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, contentType.getValue())
		.build();
	    getProcessDescriptionValidator.validateResponse(response, data);
	    Assert.assertTrue(data.isValid(), printResults(data.results()));
	} catch (Exception e) {
	    Assert.fail(e.getLocalizedMessage());
	}

    }


}
