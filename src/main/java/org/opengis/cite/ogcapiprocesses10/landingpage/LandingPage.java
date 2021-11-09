package org.opengis.cite.ogcapiprocesses10.landingpage;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;
import static org.opengis.cite.ogcapiprocesses10.EtsAssert.assertTrue;

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


    /**
     * <pre>
     * cf. core/abstract_tests/core/ATS_landingpage-success.adoc
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
    @Test(description = "Implements Abstract Test 1: /conf/core/landingpage-op", groups = "landingpage")
    public void landingPageRetrieval() {
        Response request = init().baseUri( rootUri.toString() ).accept( JSON ).when().request( GET, "/" );
        TestSuiteLogger.log(Level.INFO, rootUri.toString());
        request.then().statusCode( 200 );
        response = request.jsonPath();
    }

    /**
     * <pre>
     * cf. core/abstract_tests/core/ATS_landingpage-success.adoc
     * </pre>
     */
    @Test(description = "Implements Abstract Test 2: /conf/core/landingpage-success", groups = "landingpage")
    public void landingPageValidation() {

        List<Object> links = response.getList( "links" );

        Set<String> linkTypes = collectLinkTypes( links );

	String required[] = {
	    "service-desc",
	    "service-doc",
	    "http://www.opengis.net/def/rel/ogc/1.0/conformance",
	    "http://www.opengis.net/def/rel/ogc/1.0/processes"
	};
	
	for(int i=0;i<required.length;i++){
	    boolean expectedLinkTypesExists = linkTypes.contains( required[i] );
	    assertTrue( expectedLinkTypesExists,
			"The landing page must include at least links with relation type '"+required[i]+"', but contains "
			+ String.join( ", ", linkTypes ) );
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
