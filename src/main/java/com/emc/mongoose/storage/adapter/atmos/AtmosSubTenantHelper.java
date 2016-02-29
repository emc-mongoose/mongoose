package com.emc.mongoose.storage.adapter.atmos;
//
import static com.emc.mongoose.storage.adapter.atmos.SubTenant.KEY_SUBTENANT_ID;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
//
import com.emc.mongoose.core.impl.item.token.BasicToken;
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
public class AtmosSubTenantHelper {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	@SuppressWarnings("FieldCanBeLocal")
	private final HttpRequestConfigImpl reqConf;
	private volatile String subTenant = null;
	//
	public AtmosSubTenantHelper(final HttpRequestConfigImpl reqConf, final String value) {
		this.reqConf = reqConf;
		this.subTenant = value;
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
				method, HttpRequestConfigImpl.PREFIX_URI + SUBTENANT + "s/" + subTenant
			);
		}
		//
		return reqConf.execute(addr, httpReq, timeOut, timeUnit);
	}
	//
	public final boolean exists(final String addr)
	throws IllegalStateException {
		boolean flagExists = false;
		//
		if(subTenant != null && subTenant.length() > 0) {
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
							LOG.debug(Markers.MSG, "Subtenant \"{}\" exists", subTenant);
							flagExists = true;
						} else if(statusCode == HttpStatus.SC_NOT_FOUND) {
							LOG.debug(Markers.MSG, "Subtenant \"{}\" doesn't exist", subTenant);
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
							subTenant = httpResp.getLastHeader(KEY_SUBTENANT_ID).getValue();
							LOG.info(Markers.MSG, "Subtenant \"{}\" created", subTenant);
							reqConf.setAuthToken(new BasicToken(subTenant));
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
							Markers.ERR, "Create subtenant \"{}\" response ({}): {}",
							subTenant, statusCode, msg.toString()
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
						LOG.info(Markers.MSG, "Subtenant \"{}\" deleted", subTenant);
					} else {
						final StringBuilder msg = new StringBuilder(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.append('\n').append(buff.toString());
							}
						}
						LOG.warn(
							Markers.ERR, "Delete subtenant \"{}\" response ({}): {}",
							subTenant, statusCode, msg.toString()
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
