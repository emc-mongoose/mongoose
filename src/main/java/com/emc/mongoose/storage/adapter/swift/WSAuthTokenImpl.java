package com.emc.mongoose.storage.adapter.swift;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.HTTPMethod;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.data.WSObject;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.ByteArrayOutputStream;
import java.io.IOException;
/**
 Created by kurila on 03.03.15.
 */
public class WSAuthTokenImpl<T extends WSObject>
implements AuthToken<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final WSRequestConfigImpl<T> reqConf;
	private String value = null;
	//
	public WSAuthTokenImpl(final WSRequestConfigImpl<T> reqConf, final String value) {
		this.reqConf = reqConf;
		this.value = value;
	}
	//
	@Override
	public final String getValue() {
		return value;
	}
	//
	@Override
	public final String toString() {
		return getValue();
	}
	//
	@Override
	public final void create(final String addr)
	throws IllegalStateException {
		try {
			final HttpResponse httpResp = execute(addr, HTTPMethod.GET);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						if(httpResp.containsHeader(WSRequestConfigImpl.KEY_X_AUTH_TOKEN)) {
							value = httpResp
								.getFirstHeader(WSRequestConfigImpl.KEY_X_AUTH_TOKEN)
								.getValue();
							LOG.info(Markers.MSG, "Created auth token \"{}\"", value);
						} else {
							LOG.warn(Markers.ERR, "Server hasn't returned auth token header");
						}
					} else {
						final StringBuilder msg = new StringBuilder("Create auth token failure: ")
							.append(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.append('\n').append(buff.toString());
							}
						}
						LOG.warn(
							Markers.ERR, "Create auth token response ({}): {}",
							value, statusCode, msg.toString()
						);
					}
				}
				EntityUtils.consumeQuietly(httpEntity);
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "HTTP request execution failure");
		}
	}
	//
	private final static String MSG_INVALID_METHOD = "<NULL> is invalid HTTP method";
	//
	private HttpResponse execute(final String addr, final HTTPMethod method)
	throws IOException {
		//
		if(method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		//
		final MutableWSRequest httpReq = reqConf
			.createRequest().setMethod(method).setUriPath("/auth/v1.0");
		//
		httpReq.setHeader(WSRequestConfigImpl.KEY_X_AUTH_USER, reqConf.getUserName());
		httpReq.setHeader(WSRequestConfigImpl.KEY_X_AUTH_KEY, reqConf.getSecret());
		httpReq.setHeader(HttpHeaders.ACCEPT, "*/*");
		//
		return reqConf.execute(addr, httpReq);
	}
}
