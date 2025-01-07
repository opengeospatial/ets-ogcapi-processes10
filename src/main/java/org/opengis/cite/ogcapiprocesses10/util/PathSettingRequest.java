package org.opengis.cite.ogcapiprocesses10.util;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openapi4j.core.util.MultiStringMap;
import org.openapi4j.operation.validator.model.Request;
import org.openapi4j.operation.validator.model.impl.Body;

/**
 * <p>
 * PathSettingRequest class.
 * </p>
 *
 * @author bpr
 */
public class PathSettingRequest implements Request {

	private final String url;

	private final Method method;

	private final String path;

	private final Map<String, String> cookies;

	// private final Map<String, Collection<String>> headers;
	private final MultiStringMap<String> headers;

	private final String query;

	private final Body body;

	/**
	 * <p>
	 * Constructor for PathSettingRequest.
	 * </p>
	 * @param url a {@link java.lang.String} object
	 * @param path a {@link java.lang.String} object
	 * @param method a Method object
	 */
	public PathSettingRequest(String url, String path, Method method) {
		this.url = requireNonNull(url, "A URL is required");
		this.method = requireNonNull(method, "A method is required");
		this.path = path;
		this.query = "";
		this.body = null;
		this.cookies = new HashMap<>();
		this.headers = new MultiStringMap<>(false);
	}

	/** {@inheritDoc} */
	@Override
	public String getURL() {
		return url;
	}

	/** {@inheritDoc} */
	@Override
	public String getPath() {
		return path;
	}

	/** {@inheritDoc} */
	@Override
	public Method getMethod() {
		return method;
	}

	/** {@inheritDoc} */
	@Override
	public Body getBody() {
		return body;
	}

	/** {@inheritDoc} */
	@Override
	public String getQuery() {
		return query;
	}

	/** {@inheritDoc} */
	@Override
	public Map<String, String> getCookies() {
		return cookies;
	}

	/** {@inheritDoc} */
	@Override
	public Map<String, Collection<String>> getHeaders() {
		return headers.asUnmodifiableMap();
	}

	/** {@inheritDoc} */
	@Override
	public Collection<String> getHeaderValues(String name) {
		return headers.get(name);
	}

}
