package com.emc.mongoose.storage.adapter.swift;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.impl.data.model.GenericWSContainerBase;
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
/**
 Created by kurila on 03.03.15.
 */
public final class WSContainerImpl<T extends WSObject>
extends GenericWSContainerBase<T>
implements Container<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public WSContainerImpl(
		final WSRequestConfigImpl<T> reqConf, final String name, final boolean versioningEnabled
	) {
		super(reqConf, name, versioningEnabled);
	}
	//
	@Override
	public final boolean exists(final String addr)
	throws IllegalStateException {
		boolean flagExists = false;
		//
		try {
			final HttpResponse httpResp = execute(
				addr, WSRequestConfig.METHOD_HEAD, null, batchSize
			);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						LOG.debug(Markers.MSG, "Container \"{}\" exists", name);
						flagExists = true;
					} else if(statusCode == HttpStatus.SC_NOT_FOUND) {
						LOG.debug(Markers.MSG, "Container \"{}\" doesn't exist", name);
					} else {
						final StringBuilder msg = new StringBuilder("Check container \"")
							.append(name).append("\" failure: ")
							.append(statusLine.getReasonPhrase());
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
		//
		return flagExists;
	}
	//
	@Override
	public final void create(final String addr)
	throws IllegalStateException {
		try {
			final HttpResponse httpResp = execute(
				addr, WSRequestConfig.METHOD_PUT, null, batchSize
			);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						LOG.info(Markers.MSG, "Container \"{}\" created", name);
					} else {
						final StringBuilder msg = new StringBuilder("Create container \"")
							.append(name).append("\" failure: ")
							.append(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.append('\n').append(buff.toString());
							}
						}
						LOG.warn(
							Markers.ERR, "Create container \"{}\" response ({}): {}",
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
				addr, WSRequestConfig.METHOD_DELETE, null, batchSize
			);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						LOG.info(Markers.MSG, "Container \"{}\" deleted", name);
					} else {
						final StringBuilder msg = new StringBuilder("Delete container \"")
							.append(name).append("\" failure: ")
							.append(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.append('\n').append(buff.toString());
							}
						}
						LOG.warn(
							Markers.ERR, "Delete container \"{}\" response ({}): {}",
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
	public final void setVersioning(final String addr, final boolean enabledFlag) {
		try {
			final HttpResponse httpResp = execute(
				addr, WSRequestConfig.METHOD_POST, null, batchSize
			);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						LOG.info(Markers.MSG, "Container \"{}\" created", name);
					} else {
						final StringBuilder msg = new StringBuilder("Create container \"")
							.append(name).append("\" failure: ")
							.append(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.append('\n').append(buff.toString());
							}
						}
						LOG.warn(
							Markers.ERR, "Create container \"{}\" response ({}): {}",
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
	private final static String MSG_INVALID_METHOD = "<NULL> is invalid HTTP method";
	//
	final HttpResponse execute(
		final String addr, final String method, final String nextMarker,
		final long maxCount
	) throws IOException {
		//
		if(method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		//
		final HttpEntityEnclosingRequest httpReq;
		//
		switch(method) {
			case WSRequestConfig.METHOD_GET:
				if(nextMarker == null) {
					httpReq = reqConf.createGenericRequest(
						method,
						"/" + WSRequestConfigImpl.class.cast(reqConf).getSvcBasePath() + "/" +
							reqConf.getNameSpace() + "/" + name + "?format=json&limit=" + maxCount
					);
				} else {
					httpReq = reqConf.createGenericRequest(
						method,
						"/" + WSRequestConfigImpl.class.cast(reqConf).getSvcBasePath() + "/" +
							reqConf.getNameSpace() + "/" + name + "?format=json&limit=" + maxCount +
							"&marker=" + nextMarker
					);
				}
				break;
			case WSRequestConfig.METHOD_PUT:
				httpReq = reqConf.createGenericRequest(
					method,
					"/" + WSRequestConfigImpl.class.cast(reqConf).getSvcBasePath() + "/" +
						reqConf.getNameSpace() + "/" + name
				);
				httpReq.setHeader(
					new BasicHeader(
						WSRequestConfig.KEY_EMC_FS_ACCESS,
						Boolean.toString(reqConf.getFileAccessEnabled())
					)
				);
				break;
			case WSRequestConfig.METHOD_POST:
				httpReq = reqConf.createGenericRequest(
					method,
					"/" + WSRequestConfigImpl.class.cast(reqConf).getSvcBasePath() + "/" +
						reqConf.getNameSpace() + "/" + name
				);
				if(reqConf.getVersioning()) {
					httpReq.setHeader(
						new BasicHeader(
							WSRequestConfigImpl.KEY_X_VERSIONING,
							WSRequestConfigImpl.DEFAULT_VERSIONS_CONTAINER
						)
					);
				} else {
					httpReq.setHeader(new BasicHeader(WSRequestConfigImpl.KEY_X_VERSIONING, ""));
				}
				break;
			default:
				httpReq = reqConf.createGenericRequest(
					method,
					"/" + WSRequestConfigImpl.class.cast(reqConf).getSvcBasePath() + "/" +
						reqConf.getNameSpace() + "/" + name
				);
		}
		//
		reqConf.applyHeadersFinally(httpReq);
		return reqConf.execute(addr, httpReq);
	}
}
