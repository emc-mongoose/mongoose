package com.emc.mongoose.web.api.impl;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.api.impl.BasicObjectIOTask;
import com.emc.mongoose.util.io.HTTPContentInputStream;
import com.emc.mongoose.util.io.HTTPContentOutputStream;
import com.emc.mongoose.util.pool.InstancePool;
import com.emc.mongoose.web.api.MutableHTTPRequest;
import com.emc.mongoose.web.api.WSIOTask;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.commons.lang.text.StrBuilder;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.protocol.HttpContext;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 06.06.14.
 */
public class BasicWSIOTask<T extends WSObject>
extends BasicObjectIOTask<T>
implements WSIOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static BasicWSIOTask POISON = new BasicWSIOTask() {
		@Override
		public final String toString() {
			return "<POISON>";
		}
	};
	// BEGIN pool related things
	private final static InstancePool<BasicWSIOTask>
		POOL_WEB_IO_TASKS = new InstancePool<>(BasicWSIOTask.class);
	//
	@SuppressWarnings("unchecked")
	public static <T extends WSObject> BasicWSIOTask<T> getInstanceFor(
		final RequestConfig<T> reqConf, final T dataItem, final String nodeAddr
	) {
		final BasicWSIOTask<T> ioTask = (BasicWSIOTask<T>) POOL_WEB_IO_TASKS.take(reqConf, dataItem, nodeAddr);
		//LOG.trace(Markers.MSG, "{}: took instance for node @ {}", ioTask.hashCode(), ioTask.nodeAddr);
		return ioTask;
	}
	//
	@Override
	public void close() {
		/*synchronized(LOG) {
			LOG.trace(Markers.MSG, "{}: release instance", hashCode());
			final StackTraceElement stackTrace[] = Thread.currentThread().getStackTrace();
			for(final StackTraceElement ste: stackTrace) {
				LOG.trace(Markers.MSG, ste.toString());
			}
		}*/
		POOL_WEB_IO_TASKS.release(this);
	}
	// END pool related things
	protected WSRequestConfig<T> wsReqConf = null; // overrides RequestBase.reqConf field
	protected MutableHTTPRequest httpRequest = null;
	//
	@Override
	public WSIOTask<T> setRequestConfig(final RequestConfig<T> reqConf) {
		if(this.wsReqConf == null) { // request instance has not been configured yet?
			this.wsReqConf = (WSRequestConfig<T>) reqConf;
			httpRequest = wsReqConf.createRequest();
		} else { // cleanup
			resetRequest();
		}
		super.setRequestConfig(reqConf);
		return this;
	}
	//
	@Override
	public final WSIOTask<T> setDataItem(final T dataItem) {
		try {
			wsReqConf.applyDataItem(httpRequest, dataItem);
			super.setDataItem(dataItem);
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to apply data item");
		}
		return this;
	}
	//
	private final static Map<String, HttpHost> HTTP_HOST_MAP = new ConcurrentHashMap<>();
	@Override
	public final WSIOTask<T> setNodeAddr(final String nodeAddr) {
		super.setNodeAddr(nodeAddr);
		HttpHost tgtHost;
		if(HTTP_HOST_MAP.containsKey(nodeAddr)) {
			tgtHost = HTTP_HOST_MAP.get(nodeAddr);
		} else {
			tgtHost = new HttpHost(nodeAddr, wsReqConf.getPort(), wsReqConf.getScheme());
			HTTP_HOST_MAP.put(nodeAddr, tgtHost);
		}
		httpRequest.setUriAddr(tgtHost.toURI());
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private volatile Exception exception = null;
	@SuppressWarnings("FieldCanBeLocal")
	private volatile int respStatusCode = -1;
	//
	////////////////////////////////////////////////////////////////////////////////////////////////
	// HttpAsyncRequestProducer implementation /////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final HttpHost getTarget() {
		return HTTP_HOST_MAP.get(nodeAddr);
	}
	//
	private volatile HttpEntity reqEntity = null;
	//
	@Override
	public final HttpRequest generateRequest()
	throws IOException, HttpException {
		wsReqConf.applyHeadersFinally(httpRequest);
		reqEntity = httpRequest.getEntity();
		reqTimeStart = System.nanoTime() / 1000;
		return httpRequest;
	}
	//
	@Override
	public final void produceContent(final ContentEncoder out, final IOControl ioCtl)
	throws IOException {
		try(final OutputStream outStream = HTTPContentOutputStream.getInstance(out, ioCtl)) {
			reqEntity.writeTo(outStream);
		}
	}
	//
	@Override
	public final void requestCompleted(final HttpContext context) {
		reqTimeDone = System.nanoTime() / 1000;
	}
	//
	@Override
	public final boolean isRepeatable() {
		return reqEntity == null || reqEntity.isRepeatable();
	}
	//
	@Override
	public final void resetRequest() {
		respStatusCode = -1;
		exception = null;
		reqEntity = null;
		httpRequest.setEntity(reqEntity);
		httpRequest.removeHeaders(HttpHeaders.RANGE);
		httpRequest.removeHeaders(WSRequestConfig.KEY_EMC_SIG);
		httpRequest.removeHeaders(HttpHeaders.CONTENT_TYPE);
	}
	/*
	@Override @SuppressWarnings("unchecked")
	public final BasicWSIOTask<T> reuse(final Object... args) {
		return (BasicWSIOTask<T>) super.reuse(args);
	}*/
	////////////////////////////////////////////////////////////////////////////////////////////////
	// HttpAsyncResponseConsumer implementation ////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void responseReceived(final HttpResponse response)
	throws IOException, HttpException {
		//
		respTimeStart = System.nanoTime() / 1000;
		final StatusLine status = response.getStatusLine();
		respStatusCode = status.getStatusCode();
		//
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "{}: {}/{} <- {} {}{}", hashCode(), respStatusCode, status.getReasonPhrase(),
				httpRequest.getMethod(), httpRequest.getUriAddr(), httpRequest.getUriPath()
			);
		}
		//
		if(respStatusCode < 200 || respStatusCode >= 300) {
			switch(respStatusCode) {
				case (400):
					LOG.debug(Markers.ERR, "Incorrect request: \"{}\"", httpRequest.getRequestLine());
					result = Result.FAIL_CLIENT;
					break;
				case (403):
					LOG.debug(Markers.ERR, "Access failure");
					result = Result.FAIL_AUTH;
					break;
				case (404):
					LOG.debug(
						Markers.ERR, "Not found: {}{}",
						httpRequest.getUriAddr(), httpRequest.getUriPath()
					);
					result = Result.FAIL_NOT_FOUND;
					break;
				case (416):
					LOG.debug(Markers.ERR, "Incorrect range");
					if(LOG.isTraceEnabled(Markers.ERR)) {
						for(final Header rangeHeader : httpRequest.getHeaders(HttpHeaders.RANGE)) {
							LOG.trace(
								Markers.ERR, "Incorrect range \"{}\" for data item: \"{}\"",
								rangeHeader.getValue(), dataItem
							);
						}
					}
					result = Result.FAIL_CLIENT;
					break;
				case (500):
					LOG.debug(Markers.ERR, "Storage internal failure");
					result = Result.FAIL_SVC;
					break;
				case (503):
					LOG.warn(Markers.ERR, "Storage prays about a mercy");
					result = Result.FAIL_SVC;
					break;
				case (507):
					LOG.debug(Markers.ERR, "Not enough space is left on the storage");
					result = Result.FAIL_NO_SPACE;
				default:
					LOG.debug(Markers.ERR, "Unsupported response code: {}", respStatusCode);
					result = Result.FAIL_UNKNOWN;
			}
		} else {
			result = Result.SUCC;
			wsReqConf.receiveResponse(response, dataItem);
		}
	}
	//
	@Override
	public final void consumeContent(final ContentDecoder in, final IOControl ioCtl)
	throws IOException {
		try(final InputStream contentStream = HTTPContentInputStream.getInstance(in, ioCtl)) {
			if(respStatusCode < 200 || respStatusCode >= 300) { // failure
				final BufferedReader contentStreamBuff = new BufferedReader(
					new InputStreamReader(contentStream)
				);
				final StrBuilder msgBuilder = new StrBuilder();
				String nextLine;
				do {
					nextLine = contentStreamBuff.readLine();
					if(nextLine == null) {
						LOG.debug(
							Markers.ERR, "Response failure code \"{}\", content: \"{}\"",
							respStatusCode, msgBuilder.toString()
						);
					} else {
						msgBuilder.append(nextLine);
					}
				} while(nextLine != null);
			} else {
				wsReqConf.consumeContent(contentStream, ioCtl, dataItem);
			}
		}
	}
	//
	@Override
	public final void responseCompleted(final HttpContext context) {
		respTimeDone = System.nanoTime() / 1000;
		complete();
	}
	//
	@Override
	public final void failed(final Exception e) {
		exception = e;
		ExceptionHandler.trace(LOG, Level.DEBUG, e, "Response processing failure");
	}
	//
	@Override
	public final Exception getException() {
		return exception;
	}
	//
	@Override
	public final boolean isDone() {
		return respTimeDone != 0;
	}
	//
	@Override
	public final boolean cancel() {
		return false;
	}
}
