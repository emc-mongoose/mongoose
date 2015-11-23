package com.emc.mongoose.storage.adapter.s3;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ContainerHelper;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
//
import com.emc.mongoose.core.impl.data.model.WSContainerHelperBase;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.util.EntityUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 02.10.14.
 */
public class WSBucketHelper<T extends WSObject, C extends Container<T>>
extends WSContainerHelperBase<T, C>
implements BucketHelper<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static byte
		CONTENT_VER_CONF_ENABLED[] = (
				"<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
				"<Status>Enabled</Status></VersioningConfiguration>"
			).getBytes(StandardCharsets.UTF_8),
		CONTENT_VER_CONF_DISABLED[] = (
				"<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
				"<Status>Suspended</Status></VersioningConfiguration>"
			).getBytes(StandardCharsets.UTF_8);
	//
	public WSBucketHelper(
		final WSRequestConfigImpl<T, C> reqConf, final String name, final boolean versioningEnabled
	) {
		super(reqConf, name, versioningEnabled);
	}
	private final static String MSG_INVALID_METHOD = "<NULL> is invalid HTTP method";
	//
	HttpResponse execute(
		final String addr, final String method, final long timeOut, final TimeUnit timeUnit
	)
	throws IOException {
		return execute(addr, method, null, ContainerHelper.DEFAULT_PAGE_SIZE, timeOut, timeUnit);
	}
	//
	HttpResponse execute(final String addr, final String method, final boolean versioning)
	throws IOException {
		final HttpEntityEnclosingRequest
			httpReq = reqConf.createGenericRequest(method, "/" + name + "?" + URL_ARG_VERSIONING);
		//
		httpReq.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_XML.getMimeType());
		if(versioning) {
			httpReq.setEntity(
				new NByteArrayEntity(CONTENT_VER_CONF_ENABLED, ContentType.APPLICATION_XML)
			);
		} else {
			httpReq.setEntity(
				new NByteArrayEntity(CONTENT_VER_CONF_DISABLED, ContentType.APPLICATION_XML)
			);
		}
		reqConf.applyHeadersFinally(httpReq);
		//
		return reqConf.execute(
			addr, httpReq, WSRequestConfig.REQUEST_NO_PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS
		);
	}
	//
	HttpResponse execute(
		final String addr, final String method,  final String marker, final long limit,
		final long timeOut, final TimeUnit timeUnit
	) throws IOException {
		//
		if(method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		//
		final HttpEntityEnclosingRequest httpReq;
		if(WSRequestConfig.METHOD_PUT.equals(method)) {
			httpReq = reqConf.createGenericRequest(method, "/" + name);
			if(reqConf.getFileAccessEnabled()) {
				httpReq.setHeader(
					new BasicHeader(
						WSRequestConfigImpl.KEY_EMC_FS_ACCESS, Boolean.toString(true)
					)
				);
			}
		} else if(WSRequestConfig.METHOD_GET.equals(method)) {
			if(marker == null) {
				httpReq = reqConf.createGenericRequest(
					method, "/" + name + "?" + URL_ARG_MAX_KEYS + "=" + limit
				);
			} else {
				httpReq = reqConf.createGenericRequest(
					method,
					"/" + name + "?" + URL_ARG_MAX_KEYS + "=" + limit + "&" +
						URL_ARG_MARKER + "=" + marker
				);
			}
		} else {
			httpReq = reqConf.createGenericRequest(method, "/" + name);
		}
		reqConf.applyHeadersFinally(httpReq);
		//
		return reqConf.execute(addr, httpReq, timeOut, timeUnit);
	}
	//
	@Override
	public final boolean exists(final String addr)
	throws IllegalStateException {
		boolean flagExists = false;
		//
		try {
			final HttpResponse httpResp = execute(
				addr, WSRequestConfig.METHOD_HEAD,
				WSRequestConfig.REQUEST_NO_PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS
			);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						LOG.debug(Markers.MSG, "Bucket \"{}\" exists", name);
						flagExists = true;
					} else if(statusCode == HttpStatus.SC_NOT_FOUND) {
						LOG.debug(Markers.MSG, "Bucket \"{}\" doesn't exist", name);
					} else {
						final StringBuilder msg = new StringBuilder("Check bucket \"")
							.append(name).append("\" failure: ")
							.append(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.append('\n').append(buff.toString());
							} catch(final Exception e) {
								// ignore
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
		return flagExists;
	}
	//
	public void setVersioning(final String addr, final boolean enabledFlag) {
		try {
			final HttpResponse httpResp = execute(addr, WSRequestConfig.METHOD_PUT, enabledFlag);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						LOG.info(
							Markers.MSG, "Bucket \"{}\" versioning {}",
							name, enabledFlag ? "enabled" : "disabled"
						);
					} else {
						final StringBuilder msg = new StringBuilder("Bucket versioning \"")
							.append(name).append("\" failure: ")
							.append(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.append('\n').append(buff.toString());
							} catch(final Exception e) {
								// ignore
							}
						}
						LOG.warn(
							Markers.ERR, "Bucket versioning \"{}\" response ({}): {}",
							name, statusCode, msg.toString()
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
	public final void create(final String addr)
	throws IllegalStateException {
		//
		try {
			final HttpResponse httpResp = execute(
				addr, WSRequestConfig.METHOD_PUT,
				WSRequestConfig.REQUEST_NO_PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS
			);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						LOG.info(Markers.MSG, "Bucket \"{}\" created", name);
					} else {
						final StringBuilder msg = new StringBuilder("Create bucket \"")
							.append(name).append("\" failure: ")
							.append(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.append('\n').append(buff.toString());
							} catch(final Exception e) {
								// ignore
							}
						}
						LOG.warn(
							Markers.ERR, "Create bucket \"{}\" response ({}): {}",
							name, statusCode, msg.toString()
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
		//
		try {
			final HttpResponse httpResp = execute(
				addr, WSRequestConfig.METHOD_DELETE,
				WSRequestConfig.REQUEST_NO_PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS
			);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine==null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						LOG.info(Markers.MSG, "Bucket \"{}\" deleted", name);
					} else {
						final StringBuilder msg = new StringBuilder("Delete bucket \"")
							.append(name).append("\" failure: ")
							.append(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.append('\n').append(buff.toString());
							} catch(final Exception e) {
								// ignore
							}
						}
						LOG.warn(
							Markers.ERR, "Delete bucket \"{}\" response ({}): {}",
							name, statusCode, msg.toString()
						);
					}
				}
				EntityUtils.consumeQuietly(httpEntity);
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "HTTP request execution failure");
		}
		//
	}
}
