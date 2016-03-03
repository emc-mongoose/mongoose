package com.emc.mongoose.storage.adapter.s3;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.data.ContainerHelper;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
//
import com.emc.mongoose.core.impl.item.data.HttpContainerHelperBase;
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
public class HttpBucketHelper<T extends HttpDataItem, C extends Container<T>>
extends HttpContainerHelperBase<T, C>
implements BucketHelper<T, C> {
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
	public HttpBucketHelper(final HttpRequestConfigImpl<T, C> reqConf, final C container) {
		super(reqConf, container);
	}
	private final static String MSG_INVALID_METHOD = "<NULL> is invalid HTTP method";
	//
	HttpResponse execute(
		final String addr, final String method, final long timeOut, final TimeUnit timeUnit
	) throws IOException {
		return execute(addr, method, null, ContainerHelper.DEFAULT_PAGE_SIZE, timeOut, timeUnit);
	}
	//
	HttpResponse execute(final String addr, final String method, final boolean versioning)
	throws IOException {
		final HttpEntityEnclosingRequest httpReq = reqConf.createGenericRequest(
			method, "/" + containerName + "?" + URL_ARG_VERSIONING
		);
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
		//
		return reqConf.execute(
			addr, httpReq, HttpRequestConfig.REQUEST_NO_PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS
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
		if(HttpRequestConfig.METHOD_PUT.equals(method)) {
			httpReq = reqConf.createGenericRequest(method, "/" + containerName);
			if(reqConf.getFileAccessEnabled()) {
				httpReq.setHeader(
					new BasicHeader(
						HttpRequestConfigImpl.KEY_EMC_FS_ACCESS, Boolean.toString(true)
					)
				);
			}
		} else if(HttpRequestConfig.METHOD_GET.equals(method)) {
			if(marker == null) {
				httpReq = reqConf.createGenericRequest(
					method, "/" + containerName + "?" + URL_ARG_MAX_KEYS + "=" + limit
				);
			} else {
				httpReq = reqConf.createGenericRequest(
					method,
					"/" + containerName + "?" + URL_ARG_MAX_KEYS + "=" + limit + "&" +
						URL_ARG_MARKER + "=" + marker
				);
			}
		} else {
			httpReq = reqConf.createGenericRequest(method, "/" + containerName);
		}
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
					if(statusCode >= 200 && statusCode < 300) {
						LOG.debug(Markers.MSG, "Bucket \"{}\" exists", containerName);
						flagExists = true;
					} else if(statusCode == HttpStatus.SC_NOT_FOUND) {
						LOG.debug(Markers.MSG, "Bucket \"{}\" doesn't exist", containerName);
					} else {
						final StringBuilder msg = new StringBuilder("Check bucket \"")
							.append(containerName).append("\" failure: ")
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
			final HttpResponse httpResp = execute(addr, HttpRequestConfig.METHOD_PUT, enabledFlag);
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
							containerName, enabledFlag ? "enabled" : "disabled"
						);
					} else {
						final StringBuilder msg = new StringBuilder("Bucket versioning \"")
							.append(containerName).append("\" failure: ")
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
							containerName, statusCode, msg.toString()
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
						LOG.info(Markers.MSG, "Bucket \"{}\" created", containerName);
					} else {
						final StringBuilder msg = new StringBuilder("Create bucket \"")
							.append(containerName).append("\" failure: ")
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
							containerName, statusCode, msg.toString()
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
				addr, HttpRequestConfig.METHOD_DELETE,
				HttpRequestConfig.REQUEST_NO_PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS
			);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine==null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						LOG.info(Markers.MSG, "Bucket \"{}\" deleted", containerName);
					} else {
						final StringBuilder msg = new StringBuilder("Delete bucket \"")
							.append(containerName).append("\" failure: ")
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
							containerName, statusCode, msg.toString()
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
