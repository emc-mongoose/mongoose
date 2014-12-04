package com.emc.mongoose.web.api.impl.provider.s3;
//
import com.emc.mongoose.web.api.WSClient;
import com.emc.mongoose.web.api.WSIOTask;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.util.EntityUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
/**
 Created by kurila on 02.10.14.
 */
public class Bucket<T extends WSObject>
implements com.emc.mongoose.object.api.provider.s3.Bucket<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	final RequestConfig<T> reqConf;
	final String name;
	//
	public Bucket(final RequestConfig<T> reqConf, final String name) {
		this.reqConf = reqConf;
		//
		if(name == null || name.length() == 0) {
			final Date
				dt = Calendar.getInstance(
					TimeZone.getTimeZone("GMT+0"), Locale.ROOT
				).getTime();
			this.name = "mongoose-" + WSRequestConfig.FMT_DT.format(dt);
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
	private final static String MSG_INVALID_METHOD = "<NULL> is invalid HTTP method";
	//
	final HttpResponse execute(final WSIOTask.HTTPMethod method)
	throws IOException {
		//
		if(method == null) {
			throw new IllegalArgumentException(MSG_INVALID_METHOD);
		}
		//
		final HttpRequest httpReq = new BasicHttpRequest(method.name(), "/" + name);
		reqConf.applyHeadersFinally(httpReq);
		final WSClient<T> httpClient = reqConf.getClient();
		//
		if(httpClient == null) {
			throw new IllegalStateException("No HTTP client specified");
		}
		return httpClient.execute(
			new HttpHost(reqConf.getAddr(), reqConf.getPort(), reqConf.getScheme()), httpReq
		);
	}
	//
	@Override
	public final boolean exists()
	throws IllegalStateException {
		boolean flagExists = false;
		//
		try {
			final HttpResponse httpResp = execute(WSIOTask.HTTPMethod.HEAD);
			if(httpResp != null) {
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine==null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode==HttpStatus.SC_OK) {
						LOG.debug(Markers.MSG, "Bucket \"{}\" exists", name);
						flagExists = true;
					} else {
						final String statusMsg = statusLine.getReasonPhrase();
						LOG.debug(
							Markers.MSG, "Checking bucket \"{}\" response: {}/{}",
							name, statusCode, statusMsg
						);
					}
				}
				EntityUtils.consumeQuietly(httpResp.getEntity());
			}
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "HTTP request execution failure");
		}
		//
		return flagExists;
	}
	//
	@Override
	public final void create()
	throws IllegalStateException {
		//
		try {
			final HttpResponse httpResp = execute(WSIOTask.HTTPMethod.PUT);
			if(httpResp != null) {
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine==null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode==HttpStatus.SC_OK) {
						LOG.info(Markers.MSG, "Bucket \"{}\" created", name);
					} else {
						final String statusMsg = statusLine.getReasonPhrase();
						LOG.debug(
							Markers.MSG, "Creating bucket \"{}\" response: {}/{}",
							name, statusCode, statusMsg
						);
					}
				}
				EntityUtils.consumeQuietly(httpResp.getEntity());
			}
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "HTTP request execution failure");
		}
	}
	//
	@Override
	public final void delete()
	throws IllegalStateException {
		//
		try {
			final HttpResponse httpResp = execute(WSIOTask.HTTPMethod.DELETE);
			if(httpResp != null) {
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine==null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode==HttpStatus.SC_OK) {
						LOG.info(Markers.MSG, "Bucket \"{}\" deleted", name);
					} else {
						final String statusMsg = statusLine.getReasonPhrase();
						LOG.debug(
							Markers.MSG, "Deleting bucket \"{}\" response: {}/{}",
							name, statusCode, statusMsg
						);
					}
				}
				EntityUtils.consumeQuietly(httpResp.getEntity());
			}
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "HTTP request execution failure");
		}
		//
	}
	//
}
