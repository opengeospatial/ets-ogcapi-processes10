package org.opengis.cite.ogcapiprocesses10;

import static io.restassured.RestAssured.given;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.core.validation.ValidationResults.ValidationItem;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Server;
import org.opengis.cite.ogcapiprocesses10.util.ClientUtils;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;

/**
 * A supporting base class that sets up a common test fixture. These configuration methods are invoked before those
 * defined in a subclass.
 */
public class CommonFixture {

    private ByteArrayOutputStream requestOutputStream = new ByteArrayOutputStream();

    private ByteArrayOutputStream responseOutputStream = new ByteArrayOutputStream();

    protected RequestLoggingFilter requestLoggingFilter;

    protected ResponseLoggingFilter responseLoggingFilter;
    
    protected URI specURI;

    protected URI rootUri;
    
    protected final String CONTENT_TYPE = "Content-Type";

    /**
     * Initializes the common test fixture with a client component for interacting with HTTP endpoints.
     *
     * @param testContext
     *            The test context that contains all the information for a test run, including suite attributes.
     */
    @BeforeClass
    public void initCommonFixture( ITestContext testContext ) {
        initLogging();
        rootUri = (URI) testContext.getSuite().getAttribute( SuiteAttribute.IUT.getName() );
        try {
			specURI = new URI("https://raw.githubusercontent.com/opengeospatial/ogcapi-processes/master/openapi.yaml");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
    }

    @BeforeMethod
    public void clearMessages() {
        initLogging();
    }

    public String getRequest() {
        return requestOutputStream.toString();
    }

    public String getResponse() {
        return responseOutputStream.toString();
    }

    protected RequestSpecification init() {
        return given().filters( requestLoggingFilter, responseLoggingFilter ).log().all();
    }

    /**
     * Obtains the (XML) response entity as a DOM Document. This convenience method wraps a static method call to
     * facilitate unit testing (Mockito workaround).
     *
     * @param response
     *            A representation of an HTTP response message.
     * @param targetURI
     *            The target URI from which the entity was retrieved (may be null).
     * @return A Document representing the entity.
     *
     * @see ClientUtils#getResponseEntityAsDocument public Document getResponseEntityAsDocument( ClientResponse
     *      response, String targetURI ) { return ClientUtils.getResponseEntityAsDocument( response, targetURI ); }
     */

    /**
     * Builds an HTTP request message that uses the GET method. This convenience method wraps a static method call to
     * facilitate unit testing (Mockito workaround).
     *
     * @return A ClientRequest object.
     *
     * @see ClientUtils#buildGetRequest public ClientRequest buildGetRequest( URI endpoint, Map<String, String>
     *      qryParams, MediaType... mediaTypes ) { return ClientUtils.buildGetRequest( endpoint, qryParams, mediaTypes
     *      ); }
     */

    private void initLogging() {
        this.requestOutputStream = new ByteArrayOutputStream();
        this.responseOutputStream = new ByteArrayOutputStream();
        PrintStream requestPrintStream = new PrintStream( requestOutputStream, true );
        PrintStream responsePrintStream = new PrintStream( responseOutputStream, true );
        requestLoggingFilter = new RequestLoggingFilter( requestPrintStream );
        responseLoggingFilter = new ResponseLoggingFilter( responsePrintStream );
    }
    
    protected String printResults(ValidationResults validationResults) {
    	
    	List<ValidationItem> validationItems = validationResults.items();
    	
    	StringBuilder printedResultsStringBuilder = new StringBuilder();
    	
    	for (ValidationItem validationItem : validationItems) {
			printedResultsStringBuilder.append(validationItem + "\n");
		}
    	
    	return printedResultsStringBuilder.toString();
    }
    
    protected void addServerUnderTest(OpenApi3 openApi3) {
		Server serverUnderTest = new Server();
		String authority = rootUri.getAuthority();
		String scheme = rootUri.getScheme();
		serverUnderTest.setUrl(scheme + "://" + authority);
		openApi3.addServer(serverUnderTest);
	}
	
    protected Input createInput(JsonNode schemaNode, String id) {
		Input input = new Input(id);
		JsonNode typeNode = schemaNode.get("type");
		if(typeNode != null) {
			String typeDefinition = typeNode.asText();
			Type type = new Type(typeDefinition);
			if(typeDefinition.equals("object")) {
				JsonNode propertiesNode = schemaNode.get("properties");
				if(propertiesNode != null) {
					Iterator<String> propertyNames = propertiesNode.fieldNames();
					while (propertyNames.hasNext()) {
						String propertyName = (String) propertyNames.next();
						if(propertyName.equals("bbox")) {
							input.setBbox(true);
							break;
						}
					}
				}
			} else if(typeDefinition.equals("string")) {
				JsonNode formatNode = schemaNode.get("format");
				if(formatNode != null) {
					String format  = formatNode.asText();
					if(format.equals("byte")) {
						type.setBinary(true);
					}
				}
				
			}
			input.addType(type);
		}else {
			//oneOf, allOf, anyOf
			JsonNode oneOfNode = schemaNode.get("oneOf");
			if(oneOfNode != null) {
				if(oneOfNode instanceof ArrayNode) {
					ArrayNode oneOfArrayNode = (ArrayNode)oneOfNode;
					for (int i = 0; i < oneOfArrayNode.size(); i++) {
						JsonNode oneOfChildNode = oneOfArrayNode.get(i);		
						JsonNode oneOfTypeNode = oneOfChildNode.get("type");
						if(oneOfTypeNode != null) {							
							String typeDefinition = oneOfTypeNode.asText();
							Type type = new Type(typeDefinition);
							if(typeDefinition.equals("object")) {
								JsonNode propertiesNode = oneOfChildNode.get("properties");
								if(propertiesNode != null) {
									Iterator<String> propertyNames = propertiesNode.fieldNames();
									while (propertyNames.hasNext()) {
										String propertyName = (String) propertyNames.next();
										if(propertyName.equals("bbox")) {
											input.setBbox(true);
											break;
										}
									}
								}
							} else if(typeDefinition.equals("string")) {
								JsonNode formatNode = oneOfChildNode.get("format");
								if(formatNode != null) {
									String format  = formatNode.asText();
									if(format.equals("byte")) {
										type.setBinary(true);
									}
								}
								
							}
							input.addType(type);							
						}						
					}
				}
			}
		}		
		return input;
	}
	
	class Type {
		
		private String typeDefinition;
		private String contentEncoding;
		private String contentMediaType;
		private boolean isBinary;

		public Type(String typeDefinition) {
			this.typeDefinition = typeDefinition;
		}
		public boolean isBinary() {
			return isBinary;
		}

		public void setBinary(boolean isBinary) {
			this.isBinary = isBinary;
		}

		public String getContentEncoding() {
			return contentEncoding;
		}

		public void setContentEncoding(String contentEncoding) {
			this.contentEncoding = contentEncoding;
		}

		public String getContentMediaType() {
			return contentMediaType;
		}

		public void setContentMediaType(String contentMediaType) {
			this.contentMediaType = contentMediaType;
		}
		
		public String getTypeDefinition() {
			return typeDefinition;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("\tType definition: " + typeDefinition + "\n");
			builder.append("\tContent Encoding: " + contentEncoding + "\n");
			builder.append("\tType Content MediaType: " + contentMediaType + "\n");
			builder.append("\tIs binary: " + isBinary + "\n");
			return builder.toString();
		}
	}

	class Input {
		
		private String id;
		private List<Type> types;
		private boolean isBbox;
		
		public Input(String id, List<Type> types, boolean isBbox) {
			this.id = id;
			this.types = types;
			this.isBbox = isBbox;
		}
		
		public Input(String id, Type type) {
			this(id, Arrays.asList(new Type[] {type}), false);
		}
		
		public Input(String id) {
			this(id, new ArrayList<Type>(), false);
		}

		public boolean isBbox() {
			return isBbox;
		}

		public void setBbox(boolean isBbox) {
			this.isBbox = isBbox;
		}

		public String getId() {
			return id;
		}
		
		public List<Type> getTypes() {
			return types;
		}
		
		public boolean addType(Type type) {
			return types.add(type);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Id: " + id + "\n");
			builder.append("\tType: " + types + "\n");
			builder.append("\tIs bbox: " + isBbox);
			return builder.toString();
		}
		
	}


}
