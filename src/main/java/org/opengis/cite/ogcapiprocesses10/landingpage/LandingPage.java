package org.opengis.cite.ogcapiprocesses10.landingpage;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.HTML;
import static io.restassured.http.Method.GET;
import static org.opengis.cite.ogcapiprocesses10.EtsAssert.assertTrue;
//import com.atlassian.oai.validator.OpenApiInteractionValidator;
//import com.atlassian.oai.validator.report.ValidationReport;
import com.networknt.schema.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.core.JsonFactory;
import java.util.HashMap;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;

//import org.apache.commons.io.IOUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.opengis.cite.ogcapiprocesses10.CommonFixture;
import org.opengis.cite.ogcapiprocesses10.util.TestSuiteLogger;
import org.testng.annotations.Test;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 *
 * A.2.1. Landing Page {root}/
 *
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class LandingPage extends CommonFixture {

	private JsonPath response;

	private String body;

	// private static String
	// utrlSchema="http://schemas.opengis.net/ogcapi/common/part1/0.1/core/openapi/schemas/landingPage.json";
	// private static String
	// urlSchema="https://raw.githubusercontent.com/opengeospatial/ogcapi-processes/master/core/openapi/schemas/landingPage.yaml";
	private static String urlSchema = "http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/landingPage.yaml";

	/**
	 * <pre>
	 * cf. <a href=
	"https://github.com/opengeospatial/ogcapi-processes/blob/master/core/abstract_tests/core/ATS_landingpage-op.adoc">core/abstract_tests/core/ATS_landingpage-op.adoc</a>
	 * Abstract Test 1: /conf/core/landingpage-op
	 * Test Purpose: Validate that a landing page can be retrieved from the expected location.
	 * Requirement: /req/core/root-op
	 *
	 * Test Method:
	 *  1. Issue an HTTP GET request to the URL {root}/
	 *  2. Validate that a document was returned with a status code 200
	 *  3. Validate the contents of the returned document using test /conf/core/landingpage-success.
	 * </pre>
	 */
	@Test(description = "Implements Abstract Test 1: /conf/core/landingpage-op", groups = "A.2.1. Landing Page /")
	public void testLandingPageRetrieval() {
		Response request = init().baseUri(rootUri.toString()).accept(JSON).when().request(GET, "/");
		TestSuiteLogger.log(Level.INFO, rootUri.toString());
		request.then().statusCode(200);
		response = request.jsonPath();
		body = request.getBody().asString();
	}

	/**
	 * <pre>
	 * cf. <a href=
	"https://github.com/opengeospatial/ogcapi-processes/blob/master/core/abstract_tests/core/ATS_landingpage-success.adoc">core/abstract_tests/core/ATS_landingpage-success.adoc</a>
	 * </pre>
	 */
	@Test(description = "Implements Abstract Test 2: /conf/core/landingpage-success", groups = "A.2.1. Landing Page /")
	public void testLandingPageValidation() {

		Response request = init().baseUri(rootUri.toString()).accept(JSON).when().request(GET, "/");
		TestSuiteLogger.log(Level.INFO, rootUri.toString());
		request.then().statusCode(200);
		response = request.jsonPath();

		List<Object> links = response.getList("links");

		Set<String> linkTypes = collectLinkTypes(links);

		String required[] = { "service-desc", "service-doc", "http://www.opengis.net/def/rel/ogc/1.0/conformance",
				"http://www.opengis.net/def/rel/ogc/1.0/processes" };

		boolean isValid[] = { linkTypes.contains(required[0]), linkTypes.contains(required[1]), };
		// A.2.1.2.3.a Validate that the landing page includes a "service-desc" and/or
		// "service-doc" link to an API Definition.
		assertTrue(isValid[0] || isValid[1],
				"The landing page must include includes a 'service-desc' and/or 'service-doc' link to an API Definition, but contains "
						+ String.join(", ", linkTypes));
		// A.2.1.2.3.b Validate that the landing page includes a
		// "http://www.opengis.net/def/rel/ogc/1.0/conformance" link to the conformance
		// class declaration.
		// A.2.1.2.3.c Validate that the landing page includes a
		// "http://www.opengis.net/def/rel/ogc/1.0/processes" link to the list of
		// processes.

		for (int i = 2; i < required.length; i++) {

			boolean expectedLinkTypesExists = linkTypes.contains(required[i]);

			if (required[i].equals("http://www.opengis.net/def/rel/ogc/1.0/conformance")
					&& expectedLinkTypesExists == false) {
				expectedLinkTypesExists = linkTypes.contains("conformance"); // This is
																				// added
																				// because
																				// some
																				// servers
																				// will
																				// implement
																				// both
																				// OGC API
																				// -
																				// Features
																				// and OGC
																				// API -
																				// Processes
			}

			if (required[i].equals("http://www.opengis.net/def/rel/ogc/1.0/processes")
					&& expectedLinkTypesExists == false) {
				expectedLinkTypesExists = linkTypes.contains("processes"); // This is
																			// added
																			// because
																			// Abstract
																			// Test 2 says
																			// it should
																			// be
																			// 'processes'
																			// even though
																			// Requirement
																			// 2 says it
																			// should be
																			// http://www.opengis.net/def/rel/ogc/1.0/processes
				// The issue was reported at
				// https://github.com/opengeospatial/ogcapi-processes/issues/307
			}

			assertTrue(expectedLinkTypesExists, "The landing page must include at least links with relation type '"
					+ required[i] + "', but contains " + String.join(", ", linkTypes));
		}

		// A.2.1.2.2 Validate the landing page for *all supported media types* (only JSON)
		// using the resources and tests identified in Schema and Tests for Landing Pages
		// /conf/(geojson?)json/content
		assertTrue(validateResponseAgainstSchema(LandingPage.urlSchema, body),
				"The landing page does nto conform to the schema: " + LandingPage.urlSchema);

		// checkHtmlConfClass(rootUri.toString()); // DEACTIVATED because we do not parse
		// HTML content.

	}

	private Set<String> collectLinkTypes(List<Object> links) {
		Set<String> linkTypes = new HashSet<>();
		for (Object link : links) {
			Map<String, Object> linkMap = (Map<String, Object>) link;
			Object linkType = linkMap.get("rel");
			linkTypes.add((String) linkType);
		}
		return linkTypes;
	}

}