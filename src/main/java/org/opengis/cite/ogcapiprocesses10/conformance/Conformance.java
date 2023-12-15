package org.opengis.cite.ogcapiprocesses10.conformance;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;
import static org.opengis.cite.ogcapiprocesses10.SuiteAttribute.API_MODEL;
import static org.opengis.cite.ogcapiprocesses10.SuiteAttribute.IUT;
import static org.opengis.cite.ogcapiprocesses10.SuiteAttribute.REQUIREMENTCLASSES;
import static org.opengis.cite.ogcapiprocesses10.conformance.RequirementClass.CORE;
import static org.opengis.cite.ogcapiprocesses10.openapi3.OpenApiUtils.retrieveTestPointsForConformance;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opengis.cite.ogcapiprocesses10.CommonFixture;
import org.opengis.cite.ogcapiprocesses10.openapi3.TestPoint;
import org.opengis.cite.ogcapiprocesses10.openapi3.UriBuilder;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.reprezen.kaizen.oasparser.model3.MediaType;
import com.reprezen.kaizen.oasparser.model3.OpenApi3;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 *
 * A.2.3. Conformance Path {root}/conformance
 *
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class Conformance extends CommonFixture {

	private List<RequirementClass> requirementClasses;

	private static String urlSchema = "http://schemas.opengis.net/ogcapi/processes/part1/1.0/openapi/schemas/confClasses.yaml";

	@DataProvider(name = "conformance")
	public Object[][] conformanceUris(ITestContext testContext) {
		OpenApi3 apiModel = (OpenApi3) testContext.getSuite().getAttribute(API_MODEL.getName());
		URI iut = (URI) testContext.getSuite().getAttribute(IUT.getName());

		TestPoint tp = null;
		if (rootUri.toString().endsWith("/")) {
			tp = new TestPoint(rootUri.toString(), "conformance", null);
		}
		else {
			tp = new TestPoint(rootUri.toString(), "/conformance", null);
		}

		List<TestPoint> testPoints = new ArrayList<TestPoint>();
		testPoints.add(tp);
		Object[][] testPointsData = new Object[1][];
		int i = 0;
		for (TestPoint testPoint : testPoints) {
			testPointsData[i++] = new Object[] { testPoint };
		}
		return testPointsData;
	}

	@AfterClass
	public void storeRequirementClassesInTestContext(ITestContext testContext) {
		testContext.getSuite().setAttribute(REQUIREMENTCLASSES.getName(), this.requirementClasses);
	}

	/**
	 * Partly addresses Requirement 1 : /req/processes/core/conformance-success
	 * @param testPoint the test point to test, never <code>null</code>
	 */
	@Test(description = "Implements /conf/core/conformance-success (partial)",
			groups = "A.2.3. Conformance Path /conformance", dataProvider = "conformance")
	public void testValidateConformanceOperationAndResponse(TestPoint testPoint) {
		String testPointUri = testPoint.getServerUrl() + testPoint.getPath();
		Response response = init().baseUri(testPointUri).accept(JSON).when().request(GET);
		this.rspEntity = response.getBody().asInputStream();
		validateConformanceOperationResponse(testPointUri, response);
	}

	/**
	 * Requirement 1 : /req/processes/core/conformance-success
	 *
	 * Abstract Test A.2.3.6.1: /conf/core/conformance-success Abstract Test A.2.3.6.2:
	 * /conf/core/conformance-success Abstract Test A.2.3.6.3:
	 * /conf/core/conformance-success Abstract Test A.2.3.6.4:
	 * /conf/core/conformance-success TODO / DOABLE?
	 */
	private void validateConformanceOperationResponse(String testPointUri, Response response) {
		response.then().statusCode(200);

		assertTrue(validateResponseAgainstSchema(Conformance.urlSchema, response.getBody().asString()),
				"Unable to validate the response document against: " + Conformance.urlSchema);

		JsonPath jsonPath = response.jsonPath();
		this.requirementClasses = parseAndValidateRequirementClasses(jsonPath);
		assertTrue(this.requirementClasses.contains(CORE),
				"Conformance class \"http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/core\" is not available from path "
						+ testPointUri);
	}

	/**
	 * @param jsonPath never <code>null</code>
	 * @return the parsed requirement classes, never <code>null</code>
	 * @throws AssertionError if the json does not follow the expected structure
	 */
	List<RequirementClass> parseAndValidateRequirementClasses(JsonPath jsonPath) {
		List<Object> conformsTo = jsonPath.getList("conformsTo");
		assertNotNull(conformsTo, "Missing member 'conformsTo'.");

		List<RequirementClass> requirementClasses = new ArrayList<>();
		for (Object conformTo : conformsTo) {
			if (conformTo instanceof String) {
				String conformanceClass = (String) conformTo;
				RequirementClass requirementClass = RequirementClass.byConformanceClass(conformanceClass);
				if (requirementClass != null)
					requirementClasses.add(requirementClass);
			}
			else
				throw new AssertionError(
						"At least one element array 'conformsTo' is not a string value (" + conformTo + ")");
		}
		return requirementClasses;
	}

}
