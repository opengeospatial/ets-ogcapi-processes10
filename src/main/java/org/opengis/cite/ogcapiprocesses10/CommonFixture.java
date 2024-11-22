package org.opengis.cite.ogcapiprocesses10;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.HTML;
import static io.restassured.http.Method.GET;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.core.validation.ValidationResults.ValidationItem;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Server;
import org.opengis.cite.ogcapiprocesses10.util.JsonUtils;
import org.opengis.cite.ogcapiprocesses10.util.TestSuiteLogger;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * A supporting base class that sets up a common test fixture. These configuration methods
 * are invoked before those defined in a subclass.
 *
 * @author bpr
 */
public class CommonFixture {

	private ByteArrayOutputStream requestOutputStream = new ByteArrayOutputStream();

	private ByteArrayOutputStream responseOutputStream = new ByteArrayOutputStream();

	protected RequestLoggingFilter requestLoggingFilter;

	protected ResponseLoggingFilter responseLoggingFilter;

	protected URL specURL;

	protected URI rootUri;

	protected int limit = -1;

	protected boolean testAllProcesses = false;

	/** A String representing the request. */
	protected HttpRequest reqEntity;

	/** An Object representing the content of the response message. */
	protected Object rspEntity;

	protected final String CONTENT_TYPE = "Content-Type";

	protected final String CONTENT_MEDIA_TYPE_PROPERTY_KEY = "contentMediaType";

	protected final String CONTENT_SCHEMA_PROPERTY_KEY = "contentSchema";

	protected final String CONTENT_ENCODING_PROPERTY_KEY = "contentEncoding";

	private static final int MAX_RSP_ATTR_LENGTH = 4096;

	private static final String REQ_ATTR = "request";

	private static final String REQ_POST_ATTR = "post-request";

	private static final String RSP_ATTR = "response";

	/**
	 * Initializes the common test fixture with a client component for interacting with
	 * HTTP endpoints.
	 * @param testContext The test context that contains all the information for a test
	 * run, including suite attributes.
	 */
	@BeforeClass
	public void initCommonFixture(ITestContext testContext) {
		initLogging();
		rootUri = (URI) testContext.getSuite().getAttribute(SuiteAttribute.IUT.getName());
		limit = (Integer) testContext.getSuite().getAttribute(SuiteAttribute.PROCESS_TEST_LIMIT.getName());
		boolean useLocalSchema = (boolean) testContext.getSuite()
			.getAttribute(SuiteAttribute.USE_LOCAL_SCHEMA.getName());
		if (useLocalSchema) {
			specURL = getClass().getResource("/org/opengis/cite/ogcapiprocesses10/openapi/api-processes10.yaml");
		}
		else {
			try {
				specURL = new URI("https://developer.ogc.org/api/processes/openapi.yaml").toURL();
			}
			catch (MalformedURLException | URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * <p>
	 * clearMessages.
	 * </p>
	 */
	@BeforeMethod
	public void clearMessages() {
		initLogging();
	}

	/**
	 * <p>
	 * getRequest.
	 * </p>
	 * @return a {@link java.lang.String} object
	 */
	public String getRequest() {
		return requestOutputStream.toString();
	}

	/**
	 * <p>
	 * getResponse.
	 * </p>
	 * @return a {@link java.lang.String} object
	 */
	public String getResponse() {
		return responseOutputStream.toString();
	}

	/**
	 * <p>
	 * init.
	 * </p>
	 * @return a {@link io.restassured.specification.RequestSpecification} object
	 */
	protected RequestSpecification init() {
		return given().filters(requestLoggingFilter, responseLoggingFilter).log().all();
	}

	/**
	 * Obtains the (XML) response entity as a DOM Document. This convenience method wraps
	 * a static method call to facilitate unit testing (Mockito workaround).
	 * @param response A representation of an HTTP response message.
	 * @param targetURI The target URI from which the entity was retrieved (may be null).
	 * @return A Document representing the entity.
	 *
	 * @see ClientUtils#getResponseEntityAsDocument public Document
	 * getResponseEntityAsDocument( ClientResponse response, String targetURI ) { return
	 * ClientUtils.getResponseEntityAsDocument( response, targetURI ); }
	 */

	/**
	 * Builds an HTTP request message that uses the GET method. This convenience method
	 * wraps a static method call to facilitate unit testing (Mockito workaround).
	 * @return A ClientRequest object.
	 *
	 * @see ClientUtils#buildGetRequest public ClientRequest buildGetRequest( URI
	 * endpoint, Map String, String qryParams, MediaType... mediaTypes ) { return
	 * ClientUtils.buildGetRequest( endpoint, qryParams, mediaTypes ); }
	 */

	/**
	 * <p>
	 * validateResponseAgainstSchema.
	 * </p>
	 * @param urlSchema a {@link java.lang.String} object
	 * @param body a {@link java.lang.String} object
	 * @return a boolean
	 */
	protected boolean validateResponseAgainstSchema(String urlSchema, String body) {
		ObjectMapper objMapper = new ObjectMapper(new YAMLFactory());
		JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
			.objectMapper(objMapper)
			.build();
		SchemaValidatorsConfig config = new SchemaValidatorsConfig();
		config.setTypeLoose(false);
		try {
			JsonSchema jsonSchema = factory.getSchema(new URI(urlSchema), config);
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode json = objectMapper.readTree(body);
			Set<ValidationMessage> validationResult = jsonSchema.validate(json);
			if (validationResult.isEmpty()) {
				System.out.println("no validation errors :-)");
				return true;
			}
			else {
				validationResult.forEach(vm -> System.out.println(vm.getMessage()));
				return false;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Try to implement testing for A.5. Conformance Class HTML TODO: Abtract Test 56:
	 * /conf/html/content Abtract Test 57: /conf/html/definition
	 * @param uri a {@link java.lang.String} object
	 */
	protected void checkHtmlConfClass(String uri) {
		Response request = init().baseUri(uri).accept(HTML).when().request(GET);
		// TestSuiteLogger.log(Level.INFO, rootUri.toString());
		request.then().statusCode(200);
		String headerValue = request.getHeader("Content-Type");
		System.out.println("Content-type header fecthed: " + headerValue);
		if (headerValue.indexOf("text/html") < 0)
			throw new AssertionError(
					"The Content-type header should be text/html but '" + headerValue + "' has been found");
		String bodyHTML = request.getBody().asString();
	}

	private void initLogging() {
		this.requestOutputStream = new ByteArrayOutputStream();
		this.responseOutputStream = new ByteArrayOutputStream();
		PrintStream requestPrintStream = new PrintStream(requestOutputStream, true);
		PrintStream responsePrintStream = new PrintStream(responseOutputStream, true);
		requestLoggingFilter = new RequestLoggingFilter(requestPrintStream);
		responseLoggingFilter = new ResponseLoggingFilter(responsePrintStream);
	}

	/**
	 * <p>
	 * printResults.
	 * </p>
	 * @param validationResults a {@link org.openapi4j.core.validation.ValidationResults}
	 * object
	 * @return a {@link java.lang.String} object
	 */
	protected String printResults(ValidationResults validationResults) {
		List<ValidationItem> validationItems = validationResults.items();
		StringBuilder printedResultsStringBuilder = new StringBuilder();
		for (ValidationItem validationItem : validationItems) {
			printedResultsStringBuilder.append(validationItem + "\n");
		}
		return printedResultsStringBuilder.toString();
	}

	/**
	 * <p>
	 * addServerUnderTest.
	 * </p>
	 * @param openApi3 a {@link org.openapi4j.parser.model.v3.OpenApi3} object
	 */
	protected void addServerUnderTest(OpenApi3 openApi3) {
		Server serverUnderTest = new Server();
		String authority = rootUri.getAuthority();
		String scheme = rootUri.getScheme();
		serverUnderTest.setUrl(scheme + "://" + authority);
		openApi3.addServer(serverUnderTest);
	}

	/**
	 * <p>
	 * createInput.
	 * </p>
	 * @param schemaNode a {@link com.fasterxml.jackson.databind.JsonNode} object
	 * @param id a {@link java.lang.String} object
	 * @return a {@link org.opengis.cite.ogcapiprocesses10.CommonFixture.Input} object
	 */
	protected Input createInput(JsonNode schemaNode, String id) {
		Input input = new Input(id);
		JsonNode typeNode = schemaNode.get("type");
		if (typeNode != null) {
			String typeDefinition = typeNode.asText();
			Type type = new Type(typeDefinition);
			if (typeDefinition.equals("object")) {
				JsonNode propertiesNode = schemaNode.get("properties");
				if (propertiesNode != null) {
					Iterator<String> propertyNames = propertiesNode.fieldNames();
					while (propertyNames.hasNext()) {
						String propertyName = (String) propertyNames.next();
						if (propertyName.equals("bbox")) {
							input.setBbox(true);
							break;
						}
					}
				}
			}
			else if (typeDefinition.equals("string")) {
				JsonNode formatNode = schemaNode.get("format");
				if (formatNode != null) {
					String format = formatNode.asText();
					input.setFormat(format);
					if (format.equals("byte")) {
						type.setBinary(true);
					}
				}
				JsonNode contentMediaTypeNode = schemaNode.get(CONTENT_MEDIA_TYPE_PROPERTY_KEY);
				if (contentMediaTypeNode != null && !contentMediaTypeNode.isMissingNode()) {
					type.setContentMediaType(contentMediaTypeNode.asText());
				}
				JsonNode contentEncodingNode = schemaNode.get(CONTENT_ENCODING_PROPERTY_KEY);
				if (contentEncodingNode != null && !contentEncodingNode.isMissingNode()) {
					String contentEncodingNodeText = contentEncodingNode.asText();
					if (contentEncodingNodeText.equals("binary")) {
						type.setBinary(true);
					}
					type.setContentEncoding(contentEncodingNodeText);
				}
				JsonNode contentSchemaNode = schemaNode.get(CONTENT_SCHEMA_PROPERTY_KEY);
				if (contentSchemaNode != null && !contentSchemaNode.isMissingNode()) {
					type.setContentSchema(contentSchemaNode.asText());
				}
			}
			else if (typeDefinition.equals("array")) {
				JsonNode itemsNode = schemaNode.get("items");
				if (itemsNode != null) {
					JsonNode itemsTypeNode = itemsNode.get("type");
					if (itemsTypeNode != null) {
						Type itemsType = new Type(itemsTypeNode.asText());
						input.addType(itemsType);
					}
				}
			}
			input.addType(type);
		}
		else {
			// oneOf, allOf, anyOf
			JsonNode oneOfNode = schemaNode.get("oneOf");
			if (oneOfNode != null) {
				if (oneOfNode instanceof ArrayNode) {
					ArrayNode oneOfArrayNode = (ArrayNode) oneOfNode;
					for (int i = 0; i < oneOfArrayNode.size(); i++) {
						JsonNode oneOfChildNode = oneOfArrayNode.get(i);
						JsonNode oneOfTypeNode = oneOfChildNode.get("type");
						if (oneOfTypeNode != null) {
							String typeDefinition = oneOfTypeNode.asText();
							Type type = new Type(typeDefinition);
							if (typeDefinition.equals("object")) {
								JsonNode propertiesNode = oneOfChildNode.get("properties");
								if (propertiesNode != null) {
									Iterator<String> propertyNames = propertiesNode.fieldNames();
									while (propertyNames.hasNext()) {
										String propertyName = (String) propertyNames.next();
										if (propertyName.equals("bbox")) {
											input.setBbox(true);
											break;
										}
									}
								}
							}
							else if (typeDefinition.equals("string")) {
								JsonNode formatNode = oneOfChildNode.get("format");
								if (formatNode != null) {
									String format = formatNode.asText();
									if (format.equals("byte")) {
										type.setBinary(true);
									}
								}
								JsonNode contentMediaTypeNode = oneOfChildNode.get(CONTENT_MEDIA_TYPE_PROPERTY_KEY);
								if (contentMediaTypeNode != null && !contentMediaTypeNode.isMissingNode()) {
									type.setContentMediaType(contentMediaTypeNode.asText());
								}
								JsonNode contentEncodingNode = oneOfChildNode.get(CONTENT_ENCODING_PROPERTY_KEY);
								if (contentEncodingNode != null && !contentEncodingNode.isMissingNode()) {
									String contentEncodingNodeText = contentEncodingNode.asText();
									if (contentEncodingNodeText.equals("binary")) {
										type.setBinary(true);
									}
									type.setContentEncoding(contentEncodingNodeText);
								}
								JsonNode contentSchemaNode = oneOfChildNode.get(CONTENT_SCHEMA_PROPERTY_KEY);
								if (contentSchemaNode != null && !contentSchemaNode.isMissingNode()) {
									type.setContentSchema(contentSchemaNode.asText());
								}
							}
							else if (typeDefinition.equals("array")) {
								JsonNode itemsNode = oneOfChildNode.get("items");
								if (itemsNode != null) {
									JsonNode itemsTypeNode = itemsNode.get("type");
									if (itemsTypeNode != null) {
										Type itemsType = new Type(itemsTypeNode.asText());
										input.addType(itemsType);
									}
								}
							}
							input.addType(type);
						}
					}
				}
			}
			else {
				if (checkAllOfForBbox(schemaNode)) {
					input.setBbox(true);
				}
			}
		}
		return input;
	}

	private boolean checkAllOfForBbox(JsonNode schemaNode) {
		JsonNode allOfNode = schemaNode.get("allOf");
		if (allOfNode != null) {
			if (allOfNode instanceof ArrayNode) {
				for (JsonNode jsonNode : allOfNode) {
					JsonNode formatNode = jsonNode.get("format");
					String format = formatNode.asText();
					if (format.equals("ogc-bbox")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * <p>
	 * createOutput.
	 * </p>
	 * @param schemaNode a {@link com.fasterxml.jackson.databind.JsonNode} object
	 * @param id a {@link java.lang.String} object
	 * @return a {@link org.opengis.cite.ogcapiprocesses10.CommonFixture.Output} object
	 */
	protected Output createOutput(JsonNode schemaNode, String id) {
		Output output = new Output(id);
		JsonNode typeNode = schemaNode.get("type");
		if (typeNode != null) {
			String typeDefinition = typeNode.asText();
			if (typeDefinition.equals("array")) {
				Type itemsType = new Type(typeDefinition);
				output.addType(itemsType);
			}
		}
		else {
			if (checkAllOfForBbox(schemaNode)) {
				output.setBbox(true);
			}
		}
		return output;
	}

	public class Type {

		private String typeDefinition;

		private String contentEncoding;

		private String contentMediaType;

		private String contentSchema;

		private boolean isBinary;

		private String format;

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

		public void setContentSchema(String schema) {
			this.contentSchema = schema;

		}

		public String getContentSchema() {
			return contentSchema;
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

		public String getFormat() {
			return format;
		}

	}

	public class Input {

		protected String id;

		protected List<Type> types;

		protected boolean isBbox;

		protected String format;

		public Input(String id, List<Type> types, boolean isBbox) {
			this.id = id;
			this.types = types;
			this.isBbox = isBbox;
		}

		public Input(String id, Type type) {
			this(id, Arrays.asList(new Type[] { type }), false);
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

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
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

	public class Output {

		private String id;

		private List<Type> types;

		private boolean bbox;

		public Output(String id) {
			this.id = id;
			this.types = new ArrayList<CommonFixture.Type>();
		}

		public void addType(Type itemsType) {
			types.add(itemsType);
		}

		public void setBbox(boolean b) {
			bbox = true;
		}

		public boolean isBbox() {
			return bbox;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public List<Type> getTypes() {
			return types;
		}

		public void setTypes(List<Type> types) {
			this.types = types;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Id: " + id + "\n");
			builder.append("\tType: " + types + "\n");
			return builder.toString();
		}

	}

	/**
	 * Augments the test result with supplementary attributes in the event that a test
	 * method failed. The "request" attribute contains a String representing the request
	 * entity (POST method) or query component (GET method). The "response" attribute
	 * contains the content of the response entity.
	 * @param result A description of the test result.
	 */
	@AfterMethod
	public void addAttributesOnTestFailure(ITestResult result) {
		if (result.getStatus() != ITestResult.FAILURE) {
			return;
		}
		if (null != this.reqEntity) {
			result.setAttribute(REQ_ATTR, this.reqEntity.toString());
		}
		if (null != this.reqEntity) {
			String request = "";
			if (this.reqEntity instanceof HttpGet) {
				request = this.reqEntity.toString();
			}
			else {
				// https://github.com/opengeospatial/ets-ogcapi-processes10/issues/51
				try {
					HttpPost postRequest = (HttpPost) this.reqEntity;
					result.setAttribute(REQ_POST_ATTR, postRequest.toString());
					request = JsonUtils.inputStreamToString(postRequest.getEntity().getContent());
					// we have to add a "<" at the start, because this is used by TEAM
					// Engine EarlReporter
					// to differentiate between GET and POST.
					request = "<!-- non XML body -->\n" + request;
				}
				catch (Exception e) {
					TestSuiteLogger.log(Level.WARNING, "Could not get POST endpoint URI.", e);
				}
			}
			result.setAttribute(REQ_ATTR, request);
		}
		if (null != this.rspEntity) {
			StringBuilder response = new StringBuilder();
			if (this.rspEntity instanceof InputStream) {
				try {
					response = response.append(JsonUtils.inputStreamToString((InputStream) rspEntity));
				}
				catch (IOException e) {
					TestSuiteLogger.log(Level.WARNING, "Could not write response to String.", e);
				}
			}
			else if (rspEntity instanceof String) {
				response = response.append((String) rspEntity);
			}
			String responseString = JsonUtils.prettifyString(response.toString());
			if (response.length() > MAX_RSP_ATTR_LENGTH) {
				response = new StringBuilder();
				response.append(responseString);
				response.delete(MAX_RSP_ATTR_LENGTH, response.length());
				responseString = response.toString();
			}
			result.setAttribute(RSP_ATTR, responseString);
		}
	}

}
