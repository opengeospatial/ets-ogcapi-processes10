package org.opengis.cite.ogcapiprocesses10.landingpage;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;
import static org.opengis.cite.ogcapiprocesses10.EtsAssert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opengis.cite.ogcapiprocesses10.CommonFixture;
import org.testng.annotations.Test;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * Updated at the OGC API - Tiles Sprint 2020 by ghobona
 *
 * A.2.2. Landing Page {root}/
 *
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class LandingPage extends CommonFixture {

    private JsonPath response;


    /**
     * <pre>
     * Abstract Test 3: /ats/core/root-op
     * Test Purpose: Validate that a landing page can be retrieved from the expected location.
     * Requirement: /req/core/root-op
     *
     * Test Method:
     *  1. Issue an HTTP GET request to the URL {root}/
     *  2. Validate that a document was returned with a status code 200
     *  3. Validate the contents of the returned document using test /ats/core/root-success.
     * </pre>
     */
    @Test(description = "Implements Requirement 34 of OGC API - Tiles", groups = "landingpage")
    public void landingPageRetrieval() {
        Response request = init().baseUri( rootUri.toString() ).accept( JSON ).when().request( GET, "/" );
        request.then().statusCode( 200 );
        response = request.jsonPath();
    }

    /**
     * <pre>
     * Requirement 12
     * If the API has mechanism to expose root resources (e.g. a landing page), the API SHALL advertise a URI to retrieve tile definitions defined by this service as links
     * to the descriptions paths with rel: tiles.
     * </pre>
     */
    @Test(description = "Implements Requirement 12 of OGC API - Tiles: Landing Page {root}/, Requirement 12 (Requirement /req/tiles/root/root-success)", groups = "landingpage")
    public void tilesLandingPageValidation() {

        List<Object> links = response.getList( "links" );

        Set<String> linkTypes = collectLinkTypes( links );

        boolean expectedLinkTypesExists = linkTypes.contains( "tiles" );
        assertTrue( expectedLinkTypesExists,
                    "The landing page must include at least links with relation type 'tiles', but contains "
                                             + String.join( ", ", linkTypes ) );


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
