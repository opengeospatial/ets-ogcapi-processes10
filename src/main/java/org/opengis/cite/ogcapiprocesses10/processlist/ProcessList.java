package org.opengis.cite.ogcapiprocesses10.processlist;

import static org.testng.Assert.assertTrue;

import java.io.FileWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResults.ValidationItem;
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
import org.opengis.cite.ogcapiprocesses10.conformance.Conformance;
import org.opengis.cite.ogcapiprocesses10.util.PathSettingRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 *
 * A.2.5. List of processes {root}/processes
 *
 * @author <a href="mailto:b.pross@52north.org">Benjamin Pross </a>
 */
public class ProcessList extends CommonFixture {

	private static final String OPERATION_ID = "getProcesses";

	private OpenApi3 openApi3;

	private String getProcessListPath = "/processes";

	private OperationValidator validator;

	private URL getProcessListURL;

	private static String urlSchema = "https://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/processList.yaml";

	@BeforeClass
	public void setup() {
		String processListEndpointString = rootUri.toString() + getProcessListPath;
		try {
			openApi3 = new OpenApi3Parser().parse(specURL, false);
			addServerUnderTest(openApi3);
			final Path path = openApi3.getPathItemByOperationId(OPERATION_ID);
			final Operation operation = openApi3.getOperationById(OPERATION_ID);
			validator = new OperationValidator(openApi3, path, operation);
			getProcessListURL = new URL(processListEndpointString);
		}
		catch (MalformedURLException | ResolutionException | ValidationException e) {

			Assert.fail("Could not set up endpoint: " + processListEndpointString + ". Exception: "
					+ e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	 * Abstract Test 9: /conf/core/pl-limit-definition
	 * Test Purpose: Validate that the `limit` query parameter is constructed correctly.
	 * Requirement: /req/core/pl-limit-definition
	 * Test Method:
	 *
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/core/pl-limit-definition ", groups = "pl")
	public void testPlLimitDefinition() {
		final ValidationData<Void> data = new ValidationData<>();
		try {
			Request request = new PathSettingRequest(rootUri.toString(), getProcessListPath + "?limit=10",
					Request.Method.GET);
			this.reqEntity = new HttpGet(rootUri.toString() + getProcessListPath + "?limit=10");
			validator.validateQuery(request, data);
		}
		catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Could not validate path: " + getProcessListPath + "\n" + printResults(data.results()));
		}
		Assert.assertTrue(data.isValid(), printResults(data.results()));
	}

	/**
	 * <pre>
	 * Abstract Test 12: /conf/core/pl-limit-response
	 * Test Purpose: Validate that the `limit` query parameter is processed correctly.
	 * Requirement: /req/core/pl-limit-response
	 * Test Method:
	 * 1.  Verify that this count is not greater than the value specified by the `limit` parameter
	 * 2.  If the API definition specifies a maximum value for `limit` parameter, verify that the count does not exceed this maximum value
	 * |===
	 * TODO: Check additional content
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/core/pl-limit-response ", groups = "pl")
	public void testPlLimitResponse() {
		String testEndpoint = getProcessListURL.toString() + "?limit=1";
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpUriRequest request = new HttpGet(testEndpoint);
			request.setHeader("Accept", "application/json");
			this.reqEntity = request;
			HttpResponse httpResponse = client.execute(request);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			this.rspEntity = writer.toString();
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			JsonNode processesNode = responseNode.get("processes");
			if (!(processesNode instanceof ArrayNode)) {
				Assert.fail("No processes available.");
			}
			else {
				ArrayNode processesArrayNode = (ArrayNode) processesNode;
				Assert.assertTrue(processesArrayNode.size() == 1,
						"Wrong number of processes, expected 1, got " + processesArrayNode.size());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Could not validate path: " + testEndpoint);
		}
	}

	/**
	 * <pre>
	 * Abstract Test 11: /conf/core/pl-links
	 * Test Purpose: Validate that the proper links are included in a repsonse.
	 * Requirement: /req/core/pl-links
	 * Test Method:
	 * 1.  Verify that the `links` section of the response contains a link with `rel=alternate` for each response representation the service claims to support in its sc_conformance,conformance document
	 * |===
	 * TODO: Check additional content
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/core/pl-links ", groups = "pl")
	public void testPlLinks() {
		String testEndpoint = getProcessListURL.toString() + "?limit=1";
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpUriRequest request = new HttpGet(testEndpoint);
			this.reqEntity = request;
			request.setHeader("Accept", "application/json");
			HttpResponse httpResponse = client.execute(request);
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(httpResponse.getEntity().getContent(), writer, encoding);
			this.rspEntity = writer.toString();
			JsonNode responseNode = new ObjectMapper().readTree(writer.toString());
			JsonNode linksNode = responseNode.get("links");
			if (!(linksNode instanceof ArrayNode)) {
				Assert.fail("No links available.");
			}
			else {
				ArrayNode linksArrayNode = (ArrayNode) linksNode;
				// Assert.assertTrue(linksArrayNode.size() == 1, "Wrong number of
				// processes, expected 1, got " + linksArrayNode.size());
				Assert.assertNotNull(linksArrayNode);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Could not validate path: " + testEndpoint);
		}
	}

	/**
	 * <pre>
	 * Abstract Test 8: /conf/core/process-list
	 * Test Purpose: Validate that information about the processes can be retrieved from the expected location.
	 * Requirement: /req/core/process-list
	 * Test Method:
	 * |===
	 * TODO: Check additional content
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/core/process-list ", groups = "processlist")
	public void testProcessList() {
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
			this.rspEntity = writer.toString();
			String responsePayload = writer.toString();
			JsonNode responseNode = new ObjectMapper().readTree(responsePayload);
			ArrayNode arrayNode = (ArrayNode) responseNode.get("processes");
			Assert.assertTrue(arrayNode.size() > 0, "No processes listed at " + getProcessListURL.toString());

		}
		catch (Exception e) {
			Assert.fail(
					"ProcessList.testProcessList(): An exception occured when trying to retrieve the processes list from "
							+ getProcessListURL.toString());
		}
	}

	/**
	 * <pre>
	 * Abstract Test 10: /conf/core/process-list-success
	 * Test Purpose: Validate that the process list content complies with the required structure and contents.
	 * Requirement: /req/core/process-list-success
	 * Test Method:
	 * |===
	 *
	 * The process list may be retrieved in a number of different formats. The following table identifies the applicable schema document for each format and the test to be used to validate the against that schema. All supported formats should be exercised.
	 *
	 * [[process-list-schema]]
	 * 1. Schema and Tests for Lists content
	 * [width="90%",cols="3",options="header"]
	 * |===
	 * |Format |Schema Document |Test ID
	 * |HTML |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/processList.yaml[processList.yaml] |ats_html_content,/conf/html/content
	 * |JSON |link:http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/processList.yaml[processList.yaml] |ats_json_content,/conf/json/content
	 * |===
	 * TODO: Check additional content
	 * </pre>
	 */
	@Test(description = "Implements Requirement /req/core/process-list-success ", groups = "processlist")
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
			this.rspEntity = writer.toString();
			String responsePayload = writer.toString();
			JsonNode responseNode = new ObjectMapper().readTree(responsePayload);
			Body body = Body.from(responseNode);
			Header contentType = httpResponse.getFirstHeader(CONTENT_TYPE);
			Response response = new DefaultResponse.Builder(httpResponse.getStatusLine().getStatusCode()).body(body)
					.header(CONTENT_TYPE, contentType.getValue()).build();
			validator.validateResponse(response, data);

			assertTrue(validateResponseAgainstSchema(ProcessList.urlSchema, responsePayload),
					"The response document failed validation against: " + ProcessList.urlSchema + " ");

		}
		catch (Exception e) {
			Assert.fail(
					"ProcessList.testProcessListSuccess(): An exception occured when trying to retrieve the processes list from "
							+ getProcessListURL.toString());
		}
	}

}
