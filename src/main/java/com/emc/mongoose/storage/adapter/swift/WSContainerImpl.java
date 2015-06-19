package com.emc.mongoose.storage.adapter.swift;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
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
import java.util.Calendar;
import java.util.Date;
/**
 Created by kurila on 03.03.15.
 */
public final class WSContainerImpl<T extends WSObject>
implements Container<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final WSRequestConfigImpl<T> reqConf;
	private String name;
	//
	public WSContainerImpl(final WSRequestConfigImpl<T> reqConf, final String name) {
		this.reqConf = reqConf;
		//
		if(name == null || name.length() == 0) {
			final Date dt = Calendar.getInstance(LogUtil.TZ_UTC, LogUtil.LOCALE_DEFAULT).getTime();
			this.name = "mongoose-" + LogUtil.FMT_DT.format(dt);
		} else {
			this.name = name;
		}
	}
	//
	@Override
	public final String getName() {
		return name;
	}
	//
	@Override
	public final String toString() {
		return getName();
	}
	//
	@Override
	public final boolean exists(final String addr)
	throws IllegalStateException {
		boolean flagExists = false;
		//
		try {
			final HttpResponse httpResp = execute(addr,  MutableWSRequest.HTTPMethod.HEAD);
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
			final HttpResponse httpResp = execute(addr, MutableWSRequest.HTTPMethod.PUT);
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
			final HttpResponse httpResp = execute(addr, MutableWSRequest.HTTPMethod.DELETE);
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
	private final static String MSG_INVALID_METHOD = "<NULL> is invalid HTTP method";
	//
	final HttpResponse execute(final String addr, final MutableWSRequest.HTTPMethod method)
	throws IOException {
		//
		if(method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		//
		final MutableWSRequest httpReq = reqConf
			.createRequest()
			.setMethod(method)
			.setUriPath("/"+reqConf.getSvcBasePath()+"/"+reqConf.getNameSpace()+"/"+name);
		//
		switch(method) {
			case GET:
				// if method is get add json format parameter to uri path
				httpReq.setUriPath(httpReq.getUriPath() + "?format=json");
				break;
			case PUT:
				httpReq.setHeader(
					new BasicHeader(
						WSRequestConfig.KEY_EMC_FS_ACCESS,
						Boolean.toString(reqConf.getFileAccessEnabled())
					)
				);
				break;
		}
		//
		reqConf.applyHeadersFinally(httpReq);
		return reqConf.execute(addr, httpReq);
	}
}
