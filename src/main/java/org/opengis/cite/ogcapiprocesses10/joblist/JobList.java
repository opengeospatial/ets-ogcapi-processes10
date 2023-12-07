package org.opengis.cite.ogcapiprocesses10.joblist;

import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

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
import org.opengis.cite.ogcapiprocesses10.conformance.Conformance;
import org.opengis.cite.ogcapiprocesses10.util.ExecutionMode;
import org.opengis.cite.ogcapiprocesses10.util.PathSettingRequest;
import org.opengis.cite.ogcapiprocesses10.util.TestSuiteLogger;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.IntNode;

/**
 *
 * A.2.6. JobList  {root}/jobs
 *
 * @author <a href="mailto:b.pross@52north.org">Benjamin Pross</a>
 */
public class JobList extends CommonFixture {
	private static final String OPERATION_ID = "getJobs";

	private OpenApi3 openApi3;
	
	private String getJobListPath = "/jobs";
	
	private OperationValidator validator;
    
    private URL getJobListURL;
    
    private static String urlSchema="https://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/jobList.yaml";    
    
	@BeforeClass
	public void setup() {		
		String jobListEndpointString = rootUri.toString() + getJobListPath;		
		try {		
			openApi3 = new OpenApi3Parser().parse(specURL, false);
			addServerUnderTest(openApi3);
		    final Path path = openApi3.getPathItemByOperationId(OPERATION_ID);
		    final Operation operation = openApi3.getOperationById(OPERATION_ID);
		    validator = new OperationValidator(openApi3, path, operation);
		    getJobListURL = new URL(jobListEndpointString);
		} catch (MalformedURLException | ResolutionException | ValidationException e) {	
			
			Assert.fail("Could not set up endpoint: " + jobListEndpointString + ". Exception: " + e.getLocalizedMessage());
		}
	}


	/**
	 * <pre>
	 * Abstract test A.64: /conf/job-list/job-list-op
	 * Test Purpose: Validate that information about jobs can be retrieved from the expected location.
	 * Requirement: /req/job-list/job-list-op
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/job-list/job-list-op ", groups = "jobList")
	public void testJobList() {		
		final ValidationData<Void> data = new ValidationData<>();
		try { 
		
			HttpClient client = HttpClientBuilder.create().build();				
			HttpUriRequest request = new HttpGet(getJobListURL.toString());			
			request.setHeader("Accept", "application/json");		
		    this.reqEntity = request;	    
			HttpResponse httpResponse = client.execute(request);			
			StringWriter writer = new StringWriter();			
			String encoding = StandardCharsets.UTF_8.name();		
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			String responsePayload = writer.toString();
			this.rspEntity = responsePayload;
			JsonNode responseNode = new ObjectMapper().readTree(responsePayload);		
			ArrayNode arrayNode = (ArrayNode) responseNode.get("jobs");	
			Assert.assertTrue(arrayNode.size()>0,"No processes listed at "+getJobListURL.toString());

		} catch (Exception e) {
			Assert.fail("jobList.testjobList(): An exception occured when trying to retrieve the job list from "+getJobListURL.toString());
		}
	}

	/**
	 * <pre>
	 * Abstract test A.71: /conf/job-list/job-list-success
	 * Test Purpose: Validate that the job list content complies with the required structure and contents.
	 * Requirement: /req/job-list/job-list-success
	 * Test Method: 
	 * |===
	 * 
	 * 1. Validate that a document was returned with an HTTP status code of 200.
	 * 
	 * 2. Validate the job list content for all supported media types using the resources and tests identified in Table A.10
	 * |===
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/job-list/job-list-success ", groups = "jobList")
	public void testJobListSuccess() {
		final ValidationData<Void> data = new ValidationData<>();
		try { 
			HttpClient client = HttpClientBuilder.create().build();
			HttpUriRequest request = new HttpGet(getJobListURL.toString());
			request.setHeader("Accept", "application/json");
		    this.reqEntity = request;
			HttpResponse httpResponse = client.execute(request);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			String responsePayload = writer.toString();
                        this.rspEntity = responsePayload;
			JsonNode responseNode = new ObjectMapper().readTree(responsePayload);
			Body body = Body.from(responseNode);
			Header contentType = httpResponse.getFirstHeader(CONTENT_TYPE);
			Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body).header(CONTENT_TYPE, contentType.getValue())
					.build();
			validator.validateResponse(response, data);			
			
			assertTrue( validateResponseAgainstSchema(JobList.urlSchema,responsePayload),
				    "The response document failed validation against: "+JobList.urlSchema+ " ");	
	
		} catch (Exception e) {
			Assert.fail("jobList.testjobListSuccess(): An exception occured when trying to retrieve the processes list from "+getJobListURL.toString());
		}
	}
}