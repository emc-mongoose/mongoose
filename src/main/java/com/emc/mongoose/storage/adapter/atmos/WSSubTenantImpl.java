package com.emc.mongoose.storage.adapter.atmos;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.util.log.Markers;
import com.emc.mongoose.core.impl.util.log.TraceLogger;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.io.task.WSIOTask;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
//
import org.apache.commons.lang.text.StrBuilder;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.ByteArrayOutputStream;
import java.io.IOException;
/**
 Created by kurila on 02.10.14.
 */
public class WSSubTenantImpl<T extends WSObject>
implements SubTenant<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	@SuppressWarnings("FieldCanBeLocal")
	private final WSRequestConfigImpl<T> reqConf;
	private String value = null;
	//
	public WSSubTenantImpl(final WSRequestConfigImpl<T> reqConf, final String value) {
		this.reqConf = reqConf;
		this.value = value;
	}
	//
	@Override
	public final String getValue() {
		return toString();
	}
	//
	@Override
	public final String toString() {
		return value;
	}
	//
	private final static String
		MSG_INVALID_METHOD = "<NULL> is invalid HTTP method",
		SUBTENANT = "subtenant";
	//
	final HttpResponse execute(final WSLoadExecutor<T> wsClient, final WSIOTask.HTTPMethod method)
	throws IOException {
		//
		if(method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		if(wsClient == null) {
			throw new IllegalStateException("No HTTP client specified");
		}
		//
		final MutableWSRequest httpReq = method.createRequest();
		//
		if(WSIOTask.HTTPMethod.PUT.equals(method)) {
			httpReq.setUriPath(String.format(WSRequestConfigImpl.FMT_URI, SUBTENANT));
			httpReq.setHeader(
				new BasicHeader(
					WSRequestConfig.KEY_EMC_FS_ACCESS,
					Boolean.toString(reqConf.getFileAccessEnabled())
				)
			);
		} else {
			httpReq.setUriPath(
				String.format(
					WSRequestConfigImpl.FMT_SLASH,
					String.format(WSRequestConfigImpl.FMT_URI, SUBTENANT), value
				)
			);
		}
		//
		reqConf.applyHeadersFinally(httpReq);
		return wsClient.execute(httpReq);
	}
	//
	@Override
	public final boolean exists(final LoadExecutor<T> client)
	throws IllegalStateException {
		boolean flagExists = false;
		//
		if(value!= null && value.length() > 0) {
			try {
				final HttpResponse httpResp = execute(
					(WSLoadExecutor<T>) client, WSIOTask.HTTPMethod.HEAD
				);
				if(httpResp != null) {
					final HttpEntity httpEntity = httpResp.getEntity();
					final StatusLine statusLine = httpResp.getStatusLine();
					if(statusLine == null) {
						LOG.warn(Markers.MSG, "No response status");
					} else {
						final int statusCode = statusLine.getStatusCode();
						if(statusCode == HttpStatus.SC_OK) {
							LOG.debug(Markers.MSG, "Subtenant \"{}\" exists", value);
							flagExists = true;
						} else if(statusCode == HttpStatus.SC_NOT_FOUND) {
							LOG.debug(Markers.MSG, "Subtenant \"{}\" doesn't exist", value);
						} else {
							final StrBuilder msg = new StrBuilder(statusLine.getReasonPhrase());
							if(httpEntity != null) {
								try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
									httpEntity.writeTo(buff);
									msg.appendNewLine().append(buff.toString());
								}
							}
							throw new IllegalStateException(msg.toString());
						}
					}
					EntityUtils.consumeQuietly(httpEntity);
				}
			} catch(final IOException e) {
				TraceLogger.failure(LOG, Level.WARN, e, "HTTP request execution failure");
			}
		}
		//
		return flagExists;
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
						LOG.info(Markers.MSG, "Subtenant \"{}\" created", value);
					} else {
						final StrBuilder msg = new StrBuilder(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.appendNewLine().append(buff.toString());
							}
						}
						LOG.warn(
							Markers.ERR, "Create subtenant \"{}\" response ({}): {}", value, statusCode, msg.toString()
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
	@Override
	public final void delete(final LoadExecutor<T> client)
	throws IllegalStateException {
		try {
			final HttpResponse httpResp = execute(
				(WSLoadExecutor<T>) client, WSIOTask.HTTPMethod.DELETE
			);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode == HttpStatus.SC_OK) {
						LOG.info(Markers.MSG, "Subtenant \"{}\" deleted", value);
					} else {
						final StrBuilder msg = new StrBuilder(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.appendNewLine().append(buff.toString());
							}
						}
						LOG.warn(
							Markers.ERR, "Delete subtenant \"{}\" response ({}): {}", value, statusCode, msg.toString()
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
}
