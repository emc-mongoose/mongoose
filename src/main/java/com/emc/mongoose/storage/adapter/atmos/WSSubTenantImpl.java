package com.emc.mongoose.storage.adapter.atmos;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.HTTPMethod;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.data.WSObject;
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
	public final static String
		MSG_INVALID_METHOD = "<NULL> is invalid HTTP method",
		SUBTENANT = "subtenant";
	//
	final HttpResponse execute(final String addr, final HTTPMethod method)
	throws IOException {
		//
		if(method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		final MutableWSRequest httpReq = reqConf.createRequest().setMethod(method);
		//
		if(HTTPMethod.PUT.equals(method)) {
			httpReq.setUriPath(WSRequestConfigImpl.PREFIX_URI + SUBTENANT);
			httpReq.setHeader(
				new BasicHeader(
					WSRequestConfig.KEY_EMC_FS_ACCESS,
					Boolean.toString(reqConf.getFileAccessEnabled())
				)
			);
		} else {
			httpReq.setUriPath(WSRequestConfigImpl.PREFIX_URI + SUBTENANT + "/" + value);
		}
		//
		reqConf.applyHeadersFinally(httpReq);
		return reqConf.execute(addr, httpReq);
	}
	//
	@Override
	public final boolean exists(final String addr)
	throws IllegalStateException {
		boolean flagExists = false;
		//
		if(value != null && value.length() > 0) {
			try {
				final HttpResponse httpResp = execute(addr, HTTPMethod.HEAD);
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
							final StringBuilder msg = new StringBuilder(
								statusLine.getReasonPhrase()
							);
							if(httpEntity != null) {
								try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
									httpEntity.writeTo(buff);
									msg.append('\n').append(buff.toString());
								}
							}
							throw new IllegalStateException(msg.toString());
						}
					}
					EntityUtils.consumeQuietly(httpEntity);
				}
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "HTTP request execution failure");
			}
		}
		//
		return flagExists;
	}
	//
	@Override
	public final void create(final String addr)
	throws IllegalStateException {
		try {
			final HttpResponse httpResp = execute(addr, HTTPMethod.PUT);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						if(httpResp.containsHeader(KEY_SUBTENANT_ID)) {
							value = httpResp.getLastHeader(KEY_SUBTENANT_ID).getValue();
							LOG.info(Markers.MSG, "Subtenant \"{}\" created", value);
						} else {
							LOG.warn(
								Markers.ERR, "Storage response doesn't contain the header {}",
								KEY_SUBTENANT_ID
							);
						}
					} else {
						final StringBuilder msg = new StringBuilder(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.append('\n').append(buff.toString());
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
			LogUtil.exception(LOG, Level.WARN, e, "HTTP request execution failure");
		}
	}
	//
	@Override
	public final void delete(final String addr)
	throws IllegalStateException {
		try {
			final HttpResponse httpResp = execute(addr, HTTPMethod.DELETE);
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
						final StringBuilder msg = new StringBuilder(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.append('\n').append(buff.toString());
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
			LogUtil.exception(LOG, Level.WARN, e, "HTTP request execution failure");
		}
	}
}
