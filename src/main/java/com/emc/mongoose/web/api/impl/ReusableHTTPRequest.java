package com.emc.mongoose.web.api.impl;
//
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.web.api.MutableHTTPRequest;
//
import com.emc.mongoose.web.api.WSIOTask;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.message.BasicRequestLine;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
/**
 Created by kurila on 04.12.14.
 */
@NotThreadSafe
public final class ReusableHTTPRequest
extends AbstractHttpMessage
implements MutableHTTPRequest {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile WSIOTask.HTTPMethod method;
	private volatile String uri;
	private volatile RequestLine requestline;
	//
	public ReusableHTTPRequest(final WSIOTask.HTTPMethod method, final String uri) {
		this(new BasicRequestLine(method.name(), uri, HttpVersion.HTTP_1_1));
	}
	//
	public ReusableHTTPRequest(
		final WSIOTask.HTTPMethod method, final String uri, final ProtocolVersion ver
	) {
		this(new BasicRequestLine(method.name(), uri, ver));
	}
	//
	public ReusableHTTPRequest(final RequestLine requestline) {
		super();
		this.requestline = requestline;
		this.method = WSIOTask.HTTPMethod.valueOf(requestline.getMethod());
		this.uri = requestline.getUri();
	}
	//
	@Override
	public final ProtocolVersion getProtocolVersion() {
		return getRequestLine().getProtocolVersion();
	}
	//
	@Override
	public final RequestLine getRequestLine() {
		return requestline;
	}
	//
	@Override
	public final String toString() {
		return method.name() + ' ' + uri + ' ' + headergroup;
	}
	//
	@Override
	public final WSIOTask.HTTPMethod getMethod() {
		return method;
	}
	//
	@Override
	public final synchronized void setMethod(final WSIOTask.HTTPMethod method) {
		this.method = method;
		requestline = new BasicRequestLine(
			method.name(), uri, requestline.getProtocolVersion()
		);
	}
	//
	@Override
	public final URI getURI() {
		URI r;
		try {
			r = new URI(uri);
		} catch(final URISyntaxException e) {
			ExceptionHandler.trace(LOG, Level.DEBUG, e, "Failed to build URI instance");
		}
		return r;
	}
	//
	@Override
	public final synchronized void setUri(final String uri) {
		this.uri = uri;
		requestline = new BasicRequestLine(
			requestline.getMethod(), uri, requestline.getProtocolVersion()
		);
	}
}
