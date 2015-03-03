package com.emc.mongoose.object.api.impl.provider.swift;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.object.api.MutableWSRequest;
import com.emc.mongoose.object.api.WSIOTask;
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.object.load.WSLoadExecutor;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
//
import org.apache.commons.lang.text.StrBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
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
	private final String name;
	//
	public WSContainerImpl(final WSRequestConfigImpl<T> reqConf, final String name) {
		this.reqConf = reqConf;
		//
		if(name == null || name.length() == 0) {
			final Date dt = Calendar.getInstance(Main.TZ_UTC, Main.LOCALE_DEFAULT).getTime();
			this.name = "mongoose-" + Main.FMT_DT.format(dt);
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
	public final boolean exists(final LoadExecutor<T> client)
	throws IllegalStateException {
		boolean flagExists = false;
		//
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
						LOG.debug(Markers.MSG, "Container \"{}\" exists", name);
						flagExists = true;
					} else if(statusCode == HttpStatus.SC_NOT_FOUND) {
						LOG.debug(Markers.MSG, "Container \"{}\" doesn't exist", name);
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
						LOG.info(Markers.MSG, "Container \"{}\" created", name);
					} else {
						final StrBuilder msg = new StrBuilder(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.appendNewLine().append(buff.toString());
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
			TraceLogger.failure(LOG, Level.WARN, e, "HTTP request execution failure");
		}
	}
	//
	@Override
	public final void delete(final LoadExecutor<T> client)
	throws IllegalStateException {
		//
		try {
			final HttpResponse httpResp = execute(
				(WSLoadExecutor<T>) client, WSIOTask.HTTPMethod.DELETE
			);
			if(httpResp != null) {
				final HttpEntity httpEntity = httpResp.getEntity();
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine==null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						LOG.info(Markers.MSG, "Container \"{}\" deleted", name);
					} else {
						final StrBuilder msg = new StrBuilder(statusLine.getReasonPhrase());
						if(httpEntity != null) {
							try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
								httpEntity.writeTo(buff);
								msg.appendNewLine().append(buff.toString());
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
			TraceLogger.failure(LOG, Level.WARN, e, "HTTP request execution failure");
		}
	}
	//
	private final static String MSG_INVALID_METHOD = "<NULL> is invalid HTTP method";
	//
	private HttpResponse execute(
		final WSLoadExecutor<T> wsClient, final WSIOTask.HTTPMethod method
	) throws IOException {
		//
		if(method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		if(wsClient == null) {
			throw new IllegalStateException("No HTTP client specified");
		}
		//
		final MutableWSRequest httpReq = method
			.createRequest()
			.setUriPath(
				String.format(
					WSRequestConfigImpl.FMT_URI_CONTAINER_PATH,
					reqConf.getSvcBasePath(), reqConf.getNameSpace(), name
				)
			);
		reqConf.applyHeadersFinally(httpReq);
		return wsClient.execute(httpReq);
	}

}
