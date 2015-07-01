package com.emc.mongoose.storage.adapter.s3;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
/**
 Created by kurila on 02.10.14.
 */
public class WSBucketImpl<T extends WSObject>
implements Bucket<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String VERSIONING_ENTITY_CONTENT =
		"<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
		"<Status>Enabled</Status></VersioningConfiguration>";
	private final static String
		VERSIONING_URL_PART = "/?versioning",
		MAX_KEYS_URL_PART = "?max-keys=",
		MARKER_URL_PART = "&marker=";
	//
	private final WSRequestConfigImpl<T> reqConf;
	private String name;
	private boolean versioningEnabled;
	//
	public WSBucketImpl(
		final WSRequestConfigImpl<T> reqConf, final String name, final boolean versioningEnabled
	) {
		this.reqConf = reqConf;
		//
		if(name == null || name.length() == 0) {
			final Date dt = Calendar.getInstance(LogUtil.TZ_UTC, LogUtil.LOCALE_DEFAULT).getTime();
			this.name = "mongoose-" + LogUtil.FMT_DT.format(dt);
		} else {
			this.name = name;
		}
		this.versioningEnabled = versioningEnabled;
	}
	//
	@Override
	public final String getName() {
		return toString();
	}
	//
	@Override
	public final String toString() {
		return name;
	}
	//
	private final static String MSG_INVALID_METHOD = "<NULL> is invalid HTTP method";
	//
	HttpResponse execute(final String addr, final MutableWSRequest.HTTPMethod method)
	throws IOException {
		return execute(addr, method, false);
	}
	//
	HttpResponse execute(final String addr, final MutableWSRequest.HTTPMethod method, final boolean versioning)
	throws IOException {
		return execute(addr, method, versioning, null, WSRequestConfig.PAGE_SIZE);
	}
	//
	HttpResponse execute(
		final String addr, final MutableWSRequest.HTTPMethod method,
		final boolean versioning, final String bucketListingMarker, final long bucketMaxKeys
	) throws IOException {
		//
		if (method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		final MutableWSRequest httpReq = reqConf
			.createRequest().setMethod(method).setUriPath("/" + name);
		//
		switch(method) {
			case PUT:
				if (reqConf.getFileAccessEnabled()) {
					httpReq.setHeader(
						new BasicHeader(
							WSRequestConfigImpl.KEY_EMC_FS_ACCESS, Boolean.toString(true)
						)
					);
				}
				if (versioning) {
					httpReq.setUriPath(httpReq.getUriPath() + VERSIONING_URL_PART);
					httpReq.setEntity(
						new StringEntity(VERSIONING_ENTITY_CONTENT, ContentType.APPLICATION_XML)
					);
				}
				break;
		}
		//
		reqConf.applyHeadersFinally(httpReq);
		// this must not be in canonical request.
		// set max-keys value when get new bucket's list.
		if (MutableWSRequest.HTTPMethod.GET.equals(method)) {
			httpReq.setUriPath(httpReq.getUriPath() + MAX_KEYS_URL_PART + bucketMaxKeys);
			// if it is possible to get next bucket's list bucketListingMarker must be in URI request.
			if (bucketListingMarker != null) {
				httpReq.setUriPath(httpReq.getUriPath() + MARKER_URL_PART + bucketListingMarker);
			}
		}
		return reqConf.execute(addr, httpReq);
	}
	//
	HttpResponse getListingPage(final String addr, final String bucketListingMarker)
	throws IOException {
		return execute(
			addr, MutableWSRequest.HTTPMethod.GET, false, bucketListingMarker,
			WSRequestConfig.PAGE_SIZE
		);
	}
	//
	@Override
	public final boolean exists(final String addr)
	throws IllegalStateException {
		boolean flagExists = false;
		//
		try {
			final HttpResponse httpResp = execute(addr, MutableWSRequest.HTTPMethod.HEAD);
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
		//
		if (flagExists && versioningEnabled){
			enableVersioning(addr, MutableWSRequest.HTTPMethod.PUT);
		}
		//
		return flagExists;
	}
	//
	private void enableVersioning(final String addr, MutableWSRequest.HTTPMethod method) {
		try {
			final HttpResponse httpResp = execute(addr, method, true);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine == null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						LOG.info(Markers.MSG, "Bucket \"{}\" versioning enabled", name);
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
			final HttpResponse httpResp = execute(addr, MutableWSRequest.HTTPMethod.PUT);
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
			final HttpResponse httpResp = execute(addr, MutableWSRequest.HTTPMethod.DELETE);
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	// DataItemInput<T> implementation /////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected final AtomicReference<String> nextMarkerRef = new AtomicReference<>(null);
	//
	@Override
	public T read()
	throws EOFException, IOException {
		return null;
	}
	//
	@Override
	public int read(final List<T> buffer) throws IOException {
		return 0;
	}
	//
	@Override
	public void reset()
	throws IOException {
	}
	//
	@Override
	public void close()
	throws IOException {
	}
}
