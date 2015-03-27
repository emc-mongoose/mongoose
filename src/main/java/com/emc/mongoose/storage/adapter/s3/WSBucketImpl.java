package com.emc.mongoose.storage.adapter.s3;
//
import com.emc.mongoose.common.logging.Settings;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.TraceLogger;
import com.emc.mongoose.common.logging.Markers;
//
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.data.WSObject;
//
import org.apache.commons.lang.text.StrBuilder;
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
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
/**
 Created by kurila on 02.10.14.
 */
public class WSBucketImpl<T extends WSObject>
implements Bucket<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String VERSIONING_ENTITY_CONTENT =
		"<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">\n" +
		"	<Status>Enabled</Status>\n" +
		"	<MfaDelete>Disabled</MfaDelete>\n" +
		"</VersioningConfiguration>";
	//
	private final WSRequestConfigImpl<T> reqConf;
	private String name;
	//
	public WSBucketImpl(final WSRequestConfigImpl<T> reqConf, final String name) {
		this.reqConf = reqConf;
		//
		if(name == null || name.length() == 0) {
			final Date dt = Calendar.getInstance(Settings.TZ_UTC, Settings.LOCALE_DEFAULT).getTime();
			this.name = "mongoose-" + Settings.FMT_DT.format(dt);
		} else {
			this.name = name;
		}
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
		//
		if(method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		final MutableWSRequest httpReq = reqConf
			.createRequest().setMethod(method).setUriPath("/" + name);
		//
		final RunTimeConfig contextConfig = RunTimeConfig.getContext();
		switch(method) {
			case PUT:
				httpReq.setHeader(
					new BasicHeader(
						WSRequestConfigImpl.KEY_EMC_FS_ACCESS,
						Boolean.toString(contextConfig.getStorageFileAccessEnabled())
					)
				);
				if(RunTimeConfig.getContext().getStorageVersioningEnabled()) {
					httpReq.setEntity(
						new StringEntity(VERSIONING_ENTITY_CONTENT, ContentType.APPLICATION_XML)
					);
				}
				break;
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
						final StrBuilder msg = new StrBuilder("Check bucket \"")
							.append(name).append("\" failure: ")
							.append(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.appendNewLine().append(buff.toString());
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
			TraceLogger.failure(LOG, Level.WARN, e, "HTTP request execution failure");
		}
		//
		return flagExists;
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
						final StrBuilder msg = new StrBuilder("Create bucket \"")
							.append(name).append("\" failure: ")
							.append(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.appendNewLine().append(buff.toString());
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
			TraceLogger.failure(LOG, Level.WARN, e, "HTTP request execution failure");
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
						final StrBuilder msg = new StrBuilder("Delete bucket \"")
							.append(name).append("\" failure: ")
							.append(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.appendNewLine().append(buff.toString());
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
			TraceLogger.failure(LOG, Level.WARN, e, "HTTP request execution failure");
		}
		//
	}
}
