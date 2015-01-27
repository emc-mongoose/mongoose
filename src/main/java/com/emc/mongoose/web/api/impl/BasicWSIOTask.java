package com.emc.mongoose.web.api.impl;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.api.impl.BasicObjectIOTask;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.io.HTTPContentInputStream;
import com.emc.mongoose.util.io.HTTPContentOutputStream;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.pool.InstancePool;
import com.emc.mongoose.web.api.MutableHTTPRequest;
import com.emc.mongoose.web.api.WSIOTask;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
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
		LOG.trace(
			Markers.MSG,
			String.format(
				"linked task #%d to {%s, %s, %s}",
				ioTask.hashCode(), reqConf, dataItem.getId(), nodeAddr
			)
		);
		return ioTask;
	}
	//
	@Override
	public void close() {
		if(isClosed.compareAndSet(false, true)) {
			POOL_WEB_IO_TASKS.release(this);
		}
	}
	/*
	@Override @SuppressWarnings("unchecked")
	public BasicWSIOTask<T> reuse(final Object... args) {
		super.reuse(args);
		return this;
	}*/
	// END pool related things
	protected WSRequestConfig<T> wsReqConf = null; // overrides RequestBase.reqConf field
	protected final MutableHTTPRequest httpRequest = HTTPMethod.GET.createRequest();
	//
	@Override
	public synchronized WSIOTask<T> setRequestConfig(final RequestConfig<T> reqConf) {
		this.wsReqConf = (WSRequestConfig<T>) reqConf;
		if(!httpRequest.getMethod().equals(wsReqConf.getHTTPMethod())) {
			httpRequest.setMethod(wsReqConf.getHTTPMethod());
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
			TraceLogger.failure(LOG, Level.WARN, e, "Failed to apply data item");
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
		try {
			wsReqConf.applyHeadersFinally(httpRequest);
		} catch(final Exception e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Failed to apply the final headers");
		}
		try {
			reqEntity = httpRequest.getEntity();
		} catch(final Exception e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Failed to get the request entity");
		}
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
	@Override @SuppressWarnings("SynchronizeOnNonFinalField")
	public final void resetRequest() {
		respStatusCode = -1;
		exception = null;
		reqEntity = null;
		if(httpRequest != null) {
			synchronized(httpRequest) {
				httpRequest.setEntity(reqEntity);
				final Map<String, String> sharedHeadersMap = wsReqConf.getSharedHeadersMap();
				final Header headers[] = httpRequest.getAllHeaders().clone();
				for(final Header header : headers) {
					if(!sharedHeadersMap.containsKey(header.getName())) {
						httpRequest.removeHeaders(header.getName());
					}
				}
			}
		}
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
		if(respStatusCode < 200 || respStatusCode >= 300) {
			//
			final StringBuilder msgBuff = new StringBuilder();
			//
			switch(respStatusCode) {
				case (100):
					msgBuff.append("\"100/continue\" response is not supported\n");
					result = Result.FAIL_CLIENT;
					break;
				case (400):
					synchronized(LOG) {
						msgBuff
							.append("Incorrect request: \"")
							.append(httpRequest.getRequestLine())
							.append("\"\n");
						if(LOG.isTraceEnabled(Markers.ERR)) {
							for(final Header rangeHeader : httpRequest.getAllHeaders()) {
								msgBuff
									.append(rangeHeader.getName())
									.append(": ")
									.append(rangeHeader.getValue())
									.append('\n');
							}
						}
					}
					result = Result.FAIL_CLIENT;
					break;
				case (403):
					msgBuff
						.append("Access failure for data item ")
						.append(dataItem.getId())
						.append('\n');
					result = Result.FAIL_AUTH;
					break;
				case (404):
					msgBuff
						.append("Not found: ")
						.append(httpRequest.getUriAddr())
						.append(httpRequest.getUriPath())
						.append('\n');
					result = Result.FAIL_NOT_FOUND;
					break;
				case (405):
					msgBuff
						.append("The operation is not allowed: \"")
						.append(httpRequest.getRequestLine())
						.append("\"\n");
					result = Result.FAIL_CLIENT;
					break;
				case (409):
					msgBuff
						.append("Conflicting resource modification on \"")
						.append(httpRequest.getUriPath())
						.append("\"\n");
					result = Result.FAIL_CLIENT;
					break;
				case (411):
					msgBuff
						.append("Content length is required\n");
					result = Result.FAIL_CLIENT;
					break;
				case (413):
					msgBuff
						.append("Content is too large: ")
						.append(RunTimeConfig.formatSize(transferSize))
						.append('\n');
					result = Result.FAIL_SVC;
					break;
				case (414):
					msgBuff
						.append("URI is too long: \"")
						.append(httpRequest.getUriPath())
						.append("\"\n");
					result = Result.FAIL_CLIENT;
					break;
				case (415):
					msgBuff
						.append("Unsupported media type: \"")
						.append(httpRequest.getEntity().getContentType())
						.append("\"\n");
					result = Result.FAIL_SVC;
					break;
				case (416):
					synchronized(LOG) {
						msgBuff.append("Incorrect range\n");
						if(LOG.isTraceEnabled(Markers.ERR)) {
							for(
								final Header rangeHeader : httpRequest.getHeaders(HttpHeaders.RANGE)
							) {
								msgBuff
									.append("Incorrect range ")
									.append(rangeHeader.getValue())
									.append(" for data item ")
									.append(dataItem.getId())
									.append('\n');
							}
						}
					}
					result = Result.FAIL_CLIENT;
					break;
				case (429):
					msgBuff.append("Storage prays about a mercy\n");
					result = Result.FAIL_SVC;
					break;
				case (500):
					msgBuff.append("Storage internal failure\n");
					result = Result.FAIL_SVC;
					break;
				case (501):
					msgBuff.append("Not implemented\n");
					result = Result.FAIL_SVC;
					break;
				case (502):
					msgBuff.append("Bad gateway\n");
					result = Result.FAIL_SVC;
					break;
				case (503):
					msgBuff.append("Storage prays about a mercy\n");
					result = Result.FAIL_SVC;
					break;
				case (504):
					msgBuff.append("Gateway timeout\n");
					result = Result.FAIL_TIMEOUT;
					break;
				case (505):
					msgBuff
						.append("HTTP version is not supported: ")
						.append(httpRequest.getProtocolVersion())
						.append("\n");
					result = Result.FAIL_TIMEOUT;
					break;
				case (507):
					msgBuff.append("Not enough space is left on the storage\n");
					result = Result.FAIL_NO_SPACE;
					break;
				default:
					msgBuff
						.append("Unsupported response code: ")
						.append(respStatusCode)
						.append('\n');
					result = Result.FAIL_UNKNOWN;
					break;
			}
			//
			LOG.debug(
				Markers.ERR, "{}{}/{} <- {} {}{}",
				msgBuff, respStatusCode, status.getReasonPhrase(), httpRequest.getMethod(),
				httpRequest.getUriAddr(), httpRequest.getUriPath()
			);
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
					if(nextLine == null && LOG.isTraceEnabled(Markers.ERR)) {
						LOG.trace(
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
		TraceLogger.failure(LOG, Level.DEBUG, e, "Response processing failure");
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
