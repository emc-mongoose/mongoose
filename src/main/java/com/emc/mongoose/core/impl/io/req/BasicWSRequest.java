package com.emc.mongoose.core.impl.io.req;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.HTTPMethod;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
//
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.protocol.HTTP;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 04.12.14.
 */
@NotThreadSafe
public final class BasicWSRequest
extends AbstractHttpMessage
implements MutableWSRequest {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static class MutableRequestLine
	implements RequestLine {
		//
		private String method, uri;
		private ProtocolVersion protocolVersion;
		//
		public MutableRequestLine(final String m, final String u, final ProtocolVersion v) {
			this.method = m;
			this.protocolVersion = v;
			this.uri = u;
		}
		//
		@Override
		public final String getMethod() {
			return method;
		}
		//
		public final void setMethod(final String method) {
			this.method = method;
		}
		//
		@Override
		public final ProtocolVersion getProtocolVersion() {
			return protocolVersion;
		}
		//
		public final void setProtocolVersion(final ProtocolVersion protocolVersion) {
			this.protocolVersion = protocolVersion;
		}
		//
		@Override
		public final String getUri() {
			return uri;
		}
		//
		public final void setUri(final String uri) {
			this.uri = uri;
		}
	}
	private final MutableRequestLine requestline;
	//
	private volatile HTTPMethod method;
	private volatile String uriAddr, uriPath;
	private volatile HttpEntity contentEntity = null;
	//
	public BasicWSRequest(
		final HTTPMethod method, final String uriAddr, final String uriPath
	) {
		this(method, uriAddr, uriPath, HttpVersion.HTTP_1_1);
	}
	//
	public BasicWSRequest(
		final HTTPMethod method, final String uriAddr, final String uriPath,
		final ProtocolVersion ver
	) {
		this(
			uriAddr,
			new MutableRequestLine(
				method.name(),
				uriAddr == null ?
					(uriPath == null ? "" : uriPath) :
					(uriPath == null ? uriAddr : uriAddr + uriPath),
				ver
			)
		);
	}
	//
	private BasicWSRequest(final String uriAddr, final MutableRequestLine requestline) {
		super();
		this.requestline = requestline;
		method = HTTPMethod.valueOf(requestline.getMethod());
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
	public final HTTPMethod getMethod() {
		return method;
	}
	//
	@Override
	public final BasicWSRequest setMethod(final HTTPMethod method) {
		this.method = method;
		requestline.setMethod(method.name());
		return this;
	}
	//
	@Override
	public final String getUriAddr() {
		return uriAddr;
	}
	//
	@Override
	public final BasicWSRequest setUriAddr(final String uriAddr) {
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
	public final BasicWSRequest setUriPath(final String uriPath) {
		this.uriPath = uriPath;
		requestline.setUri(uriPath);
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
