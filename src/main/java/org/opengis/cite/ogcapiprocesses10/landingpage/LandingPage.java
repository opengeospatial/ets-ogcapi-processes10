package org.opengis.cite.ogcapiprocesses10.landingpage;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;
import static org.opengis.cite.ogcapiprocesses10.EtsAssert.assertTrue;
//import com.atlassian.oai.validator.OpenApiInteractionValidator;
//import com.atlassian.oai.validator.report.ValidationReport;
import com.networknt.schema.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.core.JsonFactory;

import io.swagger.v3.parser.*;
import io.swagger.parser.*;
import io.swagger.v3.parser.*;
import io.swagger.v3.parser.core.models.*;
import io.swagger.v3.oas.models.*;

import org.apache.commons.io.IOUtils;

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


    /**
     * <pre>
     * cf. <a href="https://github.com/opengeospatial/ogcapi-processes/blob/master/core/abstract_tests/core/ATS_landingpage-success.adoc">core/abstract_tests/core/ATS_landingpage-success.adoc</a>
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
    public void landingPageRetrieval() {
        Response request = init().baseUri( rootUri.toString() ).accept( JSON ).when().request( GET, "/" );
        TestSuiteLogger.log(Level.INFO, rootUri.toString());
        request.then().statusCode( 200 );
        response = request.jsonPath();
	body = request.getBody().asString();
    }

    /**
     * <pre>
     * cf. <a href="https://github.com/opengeospatial/ogcapi-processes/blob/master/core/abstract_tests/core/ATS_landingpage-success.adoc">core/abstract_tests/core/ATS_landingpage-success.adoc</a>
     * </pre>
     */
    @Test(description = "Implements Abstract Test 2: /conf/core/landingpage-success", groups = "A.2.1. Landing Page /")
    public void landingPageValidation() {

        List<Object> links = response.getList( "links" );

        Set<String> linkTypes = collectLinkTypes( links );

	String required[] = {
	    "service-desc",
	    "service-doc",
	    "http://www.opengis.net/def/rel/ogc/1.0/conformance",
	    "http://www.opengis.net/def/rel/ogc/1.0/processes"
	};
	boolean isValid[] = {
	    linkTypes.contains( required[0] ),
	    linkTypes.contains( required[1] ),
	};
	assertTrue( isValid[0] || isValid[1],
		    "The landing page must include includes a 'service-desc' and/or 'service-doc' link to an API Definition, but contains "
		    + String.join( ", ", linkTypes ) );
	for(int i=2;i<required.length;i++){
	    boolean expectedLinkTypesExists = linkTypes.contains( required[i] );
	    assertTrue( expectedLinkTypesExists,
			"The landing page must include at least links with relation type '"+required[i]+"', but contains "
			+ String.join( ", ", linkTypes ) );
	}
	
	/**
	 * Should be JSON-SCHEMA validation
	 * schema: https://raw.githubusercontent.com/opengeospatial/ogcapi-processes/master/core/openapi/schemas/landingPage.yaml
	 * schema instance (object): response
	 * TODO: IsValid? (make this resuable once higer level will works)
	 */
	JsonFactory json_factory = new JsonFactory();
	YAMLFactory yaml_factory = new YAMLFactory();
	ObjectMapper objectMapper =new ObjectMapper();
	ObjectMapper objMapper =new ObjectMapper(new YAMLFactory());
	Response lrequest = init().baseUri( "https://raw.githubusercontent.com/" ).when().request( GET, "/opengeospatial/ogcapi-processes/master/core/openapi/schemas/link.yaml" );
	lrequest.then().statusCode( 200 );
	String lresponse = lrequest.getBody().asString();
	JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).objectMapper(objMapper).build();
	SchemaValidatorsConfig config = new SchemaValidatorsConfig();
	config.setTypeLoose(false);
	try{
	    JsonNode node = new ObjectMapper().readTree(yaml_factory.createParser(lresponse));
	    System.out.println(node.toString());
	    JsonSchema jsonSchema = factory.getSchema(IOUtils.toInputStream(node.toString()), config);
	    JsonNode json = objectMapper.readTree(body);
	    System.out.println(response.toString());
	    JsonNode arrayNode = json.get("links");
	    if (arrayNode.isArray()) {
		for (JsonNode jsonNode : arrayNode) {
		    System.out.println(jsonNode.toString());
		    Set<ValidationMessage> validationResult = jsonSchema.validate(jsonNode);
		    if (validationResult.isEmpty()) {
			TestSuiteLogger.log(Level.INFO, "no validation errors :-)");
			System.out.println("no validation errors :-)");
		    } else {
			validationResult.forEach(vm -> System.out.println(vm.getMessage()));
		    }
		}
	    }
	    System.out.println(json.toString());
	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}

    }

    private Set<String> collectLinkTypes( List<Object> links ) {
        Set<String> linkTypes = new HashSet<>();
        for ( Object link : links ) {
            Map<String, Object> linkMap = (Map<String, Object>) link;
            Object linkType = linkMap.get( "rel" );
            linkTypes.add( (String) linkType );
        }
        return linkTypes;
    }

}
