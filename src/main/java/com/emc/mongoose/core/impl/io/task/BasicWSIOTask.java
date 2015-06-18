package com.emc.mongoose.core.impl.io.task;
// mongoose-common
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.content.OutputChannel;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-core-api
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.task.WSIOTask;
// mongoose-core-impl
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
import com.emc.mongoose.core.impl.io.req.BasicWSRequest;
//
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
//
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
/**
 Created by kurila on 06.06.14.
 */
public class BasicWSIOTask<T extends WSObject>
extends BasicObjectIOTask<T>
implements WSIOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final HeaderGroup sharedHeaders;
	private final MutableWSRequest httpRequest = new BasicWSRequest(
		MutableWSRequest.HTTPMethod.PUT, null, null
	);
	private WSRequestConfig<T> wsReqConf = null; // overrides RequestBase.reqConf field
	//
	public BasicWSIOTask(final WSLoadExecutor<T> loadExecutor) {
		super(loadExecutor);
		//
		wsReqConf = (WSRequestConfig<T>) reqConf;
		//final HeaderGroup headers = wsReqConf.getSharedHeaders();
		sharedHeaders = wsReqConf.getSharedHeaders();
		/*for(final Header header : headers.getAllHeaders()) {
			sharedHeaders.updateHeader(header);
		}*/
		//
		if(!httpRequest.getMethod().equals(wsReqConf.getHTTPMethod())) {
			httpRequest.setMethod(wsReqConf.getHTTPMethod());
		}
	}
	//
	public final static Map<WSLoadExecutor, InstancePool<BasicWSIOTask>>
		INSTANCE_POOL_MAP = new HashMap<>();
	//
	public static BasicWSIOTask getInstance(
		final WSLoadExecutor loadExecutor, WSObject dataItem, final String nodeAddr
	) {
		InstancePool<BasicWSIOTask> instPool = INSTANCE_POOL_MAP.get(loadExecutor);
		if(instPool == null) {
			try {
				instPool = new InstancePool<>(
					BasicWSIOTask.class.getConstructor(WSLoadExecutor.class), loadExecutor
				);
				INSTANCE_POOL_MAP.put(loadExecutor, instPool);
			} catch(final NoSuchMethodException e) {
				throw new IllegalStateException(e);
			}
		}
		//
		return instPool.take(dataItem, nodeAddr);
	}
	//
	@Override
	public void release() {
		final InstancePool<BasicWSIOTask> instPool = INSTANCE_POOL_MAP.get(loadExecutor);
		if(instPool == null) {
			throw new IllegalStateException("No pool found to release back");
		} else {
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG,
					"Releasing the task #{} back into the pool for {}: {}",
					hashCode(), loadExecutor, instPool.toString()
				);
			}
			instPool.release(this);
		}
	}
	//
	@Override
	public final WSIOTask<T> setDataItem(final T dataItem) {
		try {
			super.setDataItem(dataItem);
			wsReqConf.applyDataItem(httpRequest, dataItem);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply data item");
		}
		return this;
	}
	//
	@Override
	public final WSIOTask<T> setNodeAddr(final String nodeAddr)
	throws IllegalStateException {
		super.setNodeAddr(nodeAddr);
		final HttpHost tgtHost = wsReqConf.getNodeHost(nodeAddr);
		if(tgtHost != null) {
			httpRequest.setUriAddr(tgtHost.toURI());
			httpRequest.setHeader(HTTP.TARGET_HOST, nodeAddr);
		}
		return this;
	}
	/**
	 * Warning: invoked implicitly and untimely in the depths of HttpCore lib.
	 * So does nothing
	 */
	@Override
	public final void close() {}
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
		return wsReqConf.getNodeHost(nodeAddr);
	}
	//
	@Override
	public final HttpRequest generateRequest()
	throws IOException, HttpException {
		try {
			wsReqConf.applyHeadersFinally(httpRequest);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply the final headers");
		}
		reqTimeStart = System.nanoTime() / 1000;
		return httpRequest;
	}
	//
	private final static ThreadLocal<OutputChannel>
		THRLOC_CHAN_OUT = new ThreadLocal<>();
	@Override
	public final void produceContent(final ContentEncoder out, final IOControl ioCtl)
	throws IOException {
		OutputChannel chanOut = THRLOC_CHAN_OUT.get();
		if(chanOut == null) {
			chanOut = new OutputChannel();
			THRLOC_CHAN_OUT.set(chanOut);
		}
		chanOut.setContentEncoder(out);
		try {
			if(httpRequest.getEntity() != null) {
				if(dataItem.isAppending()) {
					dataItem.writeAugment(chanOut);
				} else if(dataItem.hasUpdatedRanges()) {
					dataItem.writeUpdates(chanOut);
				} else {
					if(LOG.isTraceEnabled(Markers.MSG)) {
						final long
							t = System.nanoTime(),
							writtenCount = dataItem.write(chanOut),
							rate = 1000000000 * writtenCount / (System.nanoTime() - t);
						if(dataItem.getSize() != writtenCount) {
							LOG.trace(
								Markers.ERR, "{}: written {} bytes instead of {}",
								this, writtenCount, dataItem.getSize()
							);
						} else {
							LOG.trace(
								Markers.MSG, "{}: write rate {}/s", this, SizeUtil.formatSize(rate)
							);
						}
					} else {
						dataItem.write(chanOut);
					}
				}
			}
		} catch(final ClosedChannelException e) { // probably a manual interruption
			status = Status.CANCELLED;
			LogUtil.exception(LOG, Level.TRACE, e, "Output channel closed during the operation");
		} catch(final IOException e) {
			status = Status.FAIL_IO;
			LogUtil.exception(LOG, Level.DEBUG, e, "I/O failure during the data output");
		} catch(final Exception e) {
			status = Status.FAIL_UNKNOWN;
			LogUtil.exception(LOG, Level.ERROR, e, "Producing content failure");
		} finally {
			out.complete();
			chanOut.close();
		}
	}
	//
	@Override
	public final void requestCompleted(final HttpContext context) {
		reqTimeDone = System.nanoTime() / 1000;
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Task #{}: request sent completely", hashCode());
		}
	}
	//
	@Override
	public final boolean isRepeatable() {
		return WSObject.IS_CONTENT_REPEATABLE;
	}
	//
	@Override @SuppressWarnings("SynchronizeOnNonFinalField")
	public final void resetRequest() {
		respStatusCode = -1;
		status = Status.FAIL_UNKNOWN;
		exception = null;
		if(httpRequest != null) {
			httpRequest.clearHeaders();
			httpRequest.setHeaders(sharedHeaders.getAllHeaders());
			httpRequest.setEntity(null);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Task #{}: reset the request, left headers: {}, shared headers: {}",
					hashCode(), Arrays.toString(httpRequest.getAllHeaders()),
					Arrays.toString(sharedHeaders.getAllHeaders())
				);
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
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "#{}, got response \"{}\"", hashCode(), status);
		}
		respStatusCode = status.getStatusCode();
		//
		if(respStatusCode < 200 || respStatusCode >= 300) {
			//
			final StringBuilder msgBuff = new StringBuilder();
			//
			switch(respStatusCode) {
				case HttpStatus.SC_CONTINUE:
					msgBuff.append("\"100/continue\" response is not supported\n");
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_BAD_REQUEST:
					final RequestLine requestLine = httpRequest.getRequestLine();
					msgBuff
						.append("Incorrect request: \"")
						.append(requestLine.getMethod()).append(' ')
						.append(requestLine.getUri()).append("\"\n");
					if(LOG.isTraceEnabled(Markers.ERR)) {
						for(final Header rangeHeader : httpRequest.getAllHeaders()) {
							msgBuff
								.append('\t').append(rangeHeader.getName()).append(": ")
								.append(rangeHeader.getValue()).append('\n');
						}
					}
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_UNAUTHORIZED:
				case HttpStatus.SC_FORBIDDEN:
					msgBuff.append("Access failure for data item: \"").append(dataItem);
					if(LOG.isTraceEnabled(Markers.ERR)) {
						msgBuff.append("\"\nSource request headers:\n");
						for(final Header rangeHeader : httpRequest.getAllHeaders()) {
							msgBuff
								.append('\t').append(rangeHeader.getName()).append(": ")
								.append(rangeHeader.getValue()).append('\n');
						}
						msgBuff
							.append("Canonical request view:\n")
							.append(wsReqConf.getCanonical(httpRequest))
							.append('\n');
					}
					this.status = Status.RESP_FAIL_AUTH;
					break;
				case HttpStatus.SC_NOT_FOUND:
					msgBuff
						.append("Not found: ").append(httpRequest.getUriAddr())
						.append(httpRequest.getUriPath()).append('\n');
					this.status = Status.RESP_FAIL_NOT_FOUND;
					break;
				case HttpStatus.SC_METHOD_NOT_ALLOWED:
					msgBuff
						.append("The operation is not allowed: \"")
						.append(httpRequest.getRequestLine())
						.append("\"\n");
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_CONFLICT:
					msgBuff
						.append("Conflicting resource modification on \"")
						.append(httpRequest.getUriPath())
						.append("\"\n");
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_LENGTH_REQUIRED:
					msgBuff
						.append("Content length is required\n");
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_REQUEST_TOO_LONG:
					msgBuff
						.append("Content is too large: ")
						.append(SizeUtil.formatSize(transferSize))
						.append('\n');
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_REQUEST_URI_TOO_LONG:
					msgBuff
						.append("URI is too long: \"")
						.append(httpRequest.getUriPath()).append("\"\n");
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE:
					msgBuff
						.append("Unsupported media type: \"")
						.append(httpRequest.getEntity().getContentType())
						.append("\"\n");
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE:
					msgBuff.append("Incorrect range\n");
					if(LOG.isTraceEnabled(Markers.ERR)) {
						for(final Header rangeHeader : httpRequest.getHeaders(HttpHeaders.RANGE)) {
							msgBuff
								.append("Incorrect range ").append(rangeHeader.getValue())
								.append(" for data item ").append(dataItem.getId())
								.append('\n');
						}
					}
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case 429:
					msgBuff.append("Storage prays about a mercy\n");
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					msgBuff.append("Storage internal failure\n");
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_NOT_IMPLEMENTED:
					msgBuff.append("Not implemented\n");
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_BAD_GATEWAY:
					msgBuff.append("Bad gateway\n");
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_SERVICE_UNAVAILABLE:
					msgBuff.append("Storage prays about a mercy\n");
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_GATEWAY_TIMEOUT:
					msgBuff.append("Gateway timeout\n");
					this.status = Status.FAIL_TIMEOUT;
					break;
				case HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED:
					msgBuff
						.append("HTTP version is not supported: ")
						.append(httpRequest.getProtocolVersion())
						.append("\n");
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_INSUFFICIENT_STORAGE:
					msgBuff.append("Not enough space is left on the storage\n");
					this.status = Status.RESP_FAIL_SPACE;
					break;
				default:
					msgBuff
						.append("Unsupported response code: ").append(respStatusCode)
						.append('\n');
					this.status = Status.FAIL_UNKNOWN;
					break;
			}
			//
			if(LOG.isDebugEnabled(Markers.ERR)) {
				LOG.debug(
					Markers.ERR, "Task #{}: {}{}/{} <- {} {}{}",
					hashCode(), msgBuff, respStatusCode, status.getReasonPhrase(),
					httpRequest.getMethod(), httpRequest.getUriAddr(), httpRequest.getUriPath()
				);
			}
		} else {
			this.status = Status.SUCC;
			wsReqConf.receiveResponse(response, dataItem);
		}
	}
	//
	@Override
	public final void consumeContent(final ContentDecoder in, final IOControl ioCtl) {
		if(respStatusCode < 200 || respStatusCode >= 300) { // failure, no big data is expected
			final StringBuilder msgBuilder = new StringBuilder();
			final ByteBuffer bbuff = ByteBuffer.allocate(0x1000);
			try {
				while(in.read(bbuff) >= 0) {
					msgBuilder.append(bbuff.asCharBuffer().toString());
					bbuff.clear();
				}
				if(LOG.isTraceEnabled(Markers.ERR)) {
					LOG.trace(Markers.ERR, msgBuilder);
				}
			} catch(final ClosedChannelException e) { // probably a manual interruption
				status = Status.CANCELLED;
				LogUtil.exception(LOG, Level.TRACE, e, "Output channel closed during the operation");
			} catch(final IOException e) {
				if(!wsReqConf.isClosed()) {
					LogUtil.exception(LOG, Level.DEBUG, e, "I/O failure during content consuming");
				}
			}
		} else {
			if(!wsReqConf.consumeContent(in, ioCtl, dataItem)) { // content corruption
				status = Status.RESP_FAIL_CORRUPT;
			}
		}
	}
	//
	@Override
	public final void responseCompleted(final HttpContext context) {
		respTimeDone = System.nanoTime() / 1000;
	}
	//
	@Override
	public final IOTask.Status getResult() {
		return status;
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
		LOG.debug(Markers.MSG, "{}: I/O task cancel", hashCode());
		return false;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// HttpContext implementation //////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final HttpContext wrappedHttpCtx = new BasicHttpContext();
	//
	@Override
	public final Object getAttribute(final String s) {
		return wrappedHttpCtx.getAttribute(s);
	}
	@Override
	public final void setAttribute(final String s, final Object o) {
		wrappedHttpCtx.setAttribute(s, o);
	}
	@Override
	public final Object removeAttribute(final String s) {
		return wrappedHttpCtx.removeAttribute(s);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// FutureCallback<IOTask.Status> implementation ////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void completed(final IOTask.Status status) {
		complete();
	}
	/**
	 Overrides HttpAsyncRequestProducer.failed(Exception),
	 HttpAsyncResponseConsumer&lt;IOTask.Status&gt;.failed(Exception) and
	 FutureCallback&lt;IOTask.Status&gt;.failed(Exception)
	 @param e
	 */
	@Override
	public final void failed(final Exception e) {
		if(!wsReqConf.isClosed()) {
			LogUtil.exception(LOG, Level.DEBUG, e, "{}: I/O task failure", hashCode());
		}
		exception = e;
		status = Status.FAIL_UNKNOWN;
		complete();
	}
	//
	@Override
	public final void cancelled() {
		LOG.debug(Markers.MSG, "{}: I/O task canceled", hashCode());
		status = Status.CANCELLED;
		complete();
	}
}
