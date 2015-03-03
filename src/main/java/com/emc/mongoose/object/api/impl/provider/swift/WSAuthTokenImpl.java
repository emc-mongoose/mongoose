package com.emc.mongoose.object.api.impl.provider.swift;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.object.api.MutableWSRequest;
import com.emc.mongoose.object.api.WSIOTask;
import com.emc.mongoose.object.api.impl.provider.swift.WSRequestConfigImpl;
import com.emc.mongoose.object.data.WSObject;
//
import com.emc.mongoose.object.load.WSLoadExecutor;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	public final void create(final LoadExecutor<T> client)
	throws IllegalStateException {
		try {
			final HttpResponse httpResp = execute(
				(WSLoadExecutor<T>) client, WSIOTask.HTTPMethod.PUT
			);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode == HttpStatus.SC_OK) {
						if(httpResp.containsHeader(WSRequestConfigImpl.KEY_X_AUTH_TOKEN)) {
							value = httpResp
								.getFirstHeader(WSRequestConfigImpl.KEY_X_AUTH_TOKEN)
								.getValue();
						} else {
							LOG.warn(Markers.ERR, "Server hasn't returned auth token header");
						}
					} else {
						final StrBuilder msg = new StrBuilder(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.appendNewLine().append(buff.toString());
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
			TraceLogger.failure(LOG, Level.WARN, e, "HTTP request execution failure");
		}
	}
	//
	private final static String MSG_INVALID_METHOD = "<NULL> is invalid HTTP method";
	//
	private HttpResponse execute(
		final WSLoadExecutor<T> wsClient, final WSIOTask.HTTPMethod method
	) throws IOException {
		//
		if(method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		if(wsClient == null) {
			throw new IllegalStateException("No HTTP client specified");
		}
		//
		final MutableWSRequest httpReq = method
			.createRequest()
			.setUriPath("/auth/v1.0");
		//
		httpReq.setHeader(WSRequestConfigImpl.KEY_X_AUTH_USER, reqConf.getUserName());
		httpReq.setHeader(WSRequestConfigImpl.KEY_X_AUTH_KEY, reqConf.getSecret());
		//
		return wsClient.execute(httpReq);
	}

}
