package com.emc.mongoose.storage.adapter.atmos;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
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
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 02.10.14.
 */
public class WSSubTenantImpl<T extends HttpDataItem>
implements SubTenant<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	@SuppressWarnings("FieldCanBeLocal")
	private final HttpRequestConfigImpl<T, ? extends Container<T>> reqConf;
	private String value = null;
	//
	public WSSubTenantImpl(
		final HttpRequestConfigImpl<T, ? extends Container<T>> reqConf, final String value
	) {
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
	final HttpResponse execute(
		final String addr, final String method, final long timeOut, final TimeUnit timeUnit
	) throws IOException {
		//
		if(method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		//
		final HttpEntityEnclosingRequest httpReq;
		if(HttpRequestConfig.METHOD_PUT.equals(method)) {
			httpReq = reqConf.createGenericRequest(
				method, HttpRequestConfigImpl.PREFIX_URI + SUBTENANT
			);
			httpReq.setHeader(
				new BasicHeader(
					HttpRequestConfig.KEY_EMC_FS_ACCESS,
					Boolean.toString(reqConf.getFileAccessEnabled())
				)
			);
		} else {
			httpReq = reqConf.createGenericRequest(
				method, HttpRequestConfigImpl.PREFIX_URI + SUBTENANT + "s/" + value
			);
		}
		//
		reqConf.applyHeadersFinally(httpReq);
		return reqConf.execute(addr, httpReq, timeOut, timeUnit);
	}
	//
	@Override
	public final boolean exists(final String addr)
	throws IllegalStateException {
		boolean flagExists = false;
		//
		if(value != null && value.length() > 0) {
			try {
				final HttpResponse httpResp = execute(
					addr, HttpRequestConfig.METHOD_HEAD,
					HttpRequestConfig.REQUEST_NO_PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS
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
			final HttpResponse httpResp = execute(
				addr, HttpRequestConfig.METHOD_PUT,
				HttpRequestConfig.REQUEST_NO_PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS
			);
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
			final HttpResponse httpResp = execute(
				addr, HttpRequestConfig.METHOD_DELETE,
				HttpRequestConfig.REQUEST_NO_PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS
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
