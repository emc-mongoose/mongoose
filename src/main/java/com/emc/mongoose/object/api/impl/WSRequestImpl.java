package com.emc.mongoose.object.api.impl;
//
import com.emc.mongoose.object.api.MutableWSRequest;
//
import com.emc.mongoose.object.api.WSIOTask;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.protocol.HTTP;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 04.12.14.
 */
@NotThreadSafe
public final class WSRequestImpl
extends AbstractHttpMessage
implements MutableWSRequest {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile WSIOTask.HTTPMethod method;
	private volatile String uriAddr, uriPath;
	private volatile RequestLine requestline;
	private volatile HttpEntity contentEntity = EMPTY_CONTENT_ENTITY;
	//
	public WSRequestImpl(
		final WSIOTask.HTTPMethod method, final String uriAddr, final String uriPath
	) {
		this(method, uriAddr, uriPath, HttpVersion.HTTP_1_1);
	}
	//
	public WSRequestImpl(
		final WSIOTask.HTTPMethod method, final String uriAddr, final String uriPath,
		final ProtocolVersion ver
	) {
		this(
			uriAddr,
			new BasicRequestLine(
				method.name(),
				uriAddr == null ?
					(uriPath == null ? "" : uriPath) :
					(uriPath == null ? uriAddr : uriAddr + uriPath),
				ver
			)
		);
	}
	//
	public WSRequestImpl(final String uriAddr, final RequestLine requestline) {
		super();
		this.requestline = requestline;
		method = WSIOTask.HTTPMethod.valueOf(requestline.getMethod());
		this.uriAddr = uriAddr;
		uriPath = requestline.getUri();
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
		return method.name() + ' ' + uriPath + ' ' + headergroup;
	}
	//
	@Override
	public final WSIOTask.HTTPMethod getMethod() {
		return method;
	}
	//
	@Override
	public final WSRequestImpl setMethod(final WSIOTask.HTTPMethod method) {
		this.method = method;
		requestline = new BasicRequestLine(
			method.name(), uriPath, requestline.getProtocolVersion()
		);
		return this;
	}
	//
	@Override
	public final String getUriAddr() {
		return uriAddr;
	}
	//
	@Override
	public final WSRequestImpl setUriAddr(final String uriAddr) {
		this.uriAddr = uriAddr;
		return this;
	}
	//
	@Override
	public final String getUriPath() {
		return uriPath;
	}
	//
	@Override
	public final WSRequestImpl setUriPath(final String uriPath) {
		this.uriPath = uriPath;
		requestline = new BasicRequestLine(
			method.name(), uriPath, requestline.getProtocolVersion()
		);
		return this;
	}
	//
	@Override
	public boolean expectContinue() {
		final Header expect = getFirstHeader(HTTP.EXPECT_DIRECTIVE);
		return expect != null && HTTP.EXPECT_CONTINUE.equalsIgnoreCase(expect.getValue());
	}
	//
	@Override
	public void setEntity(final HttpEntity contentEntity) {
		this.contentEntity = contentEntity;
	}
	//
	@Override
	public final HttpEntity getEntity() {
		return contentEntity;
	}
	//
	@Override
	public final void clearHeaders() {
		for(final HeaderIterator i = headergroup.iterator(); i.hasNext(); i.nextHeader()) {
			i.remove();
		}
	}
}
