package com.emc.mongoose.core.impl.io.task;
// mongoose-common
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.io.HTTPContentEncoderChannel;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.collections.InstancePool;
// mongoose-core-api
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.task.WSIOTask;
// mongoose-core-impl
import com.emc.mongoose.core.impl.io.req.WSRequestImpl;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
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
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
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
		return ioTask;
	}
	//
	@Override
	public void release() {
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LogUtil.trace(LOG, Level.TRACE, LogUtil.MSG, "Releasing the task #" + hashCode());
		}
		resetRequest();
		POOL_WEB_IO_TASKS.release(this);
	}
	// END pool related things
	private WSRequestConfig<T> wsReqConf = null; // overrides RequestBase.reqConf field
	private HeaderGroup sharedHeaders = null;
	private final MutableWSRequest httpRequest = new WSRequestImpl(
		MutableWSRequest.HTTPMethod.PUT, null, null
	);
	private volatile HttpEntity reqEntity = null;
	//
	@Override
	public synchronized WSIOTask<T> setRequestConfig(final RequestConfig<T> reqConf) {
		if(reqConf != null && !reqConf.equals(wsReqConf)) {
			this.wsReqConf = (WSRequestConfig<T>) reqConf;
			//
			final HeaderGroup headers = wsReqConf.getSharedHeaders();
			sharedHeaders = new HeaderGroup();
			for(final Header header : headers.getAllHeaders()) {
				sharedHeaders.updateHeader(header);
			}
			//
			if(!httpRequest.getMethod().equals(wsReqConf.getHTTPMethod())) {
				httpRequest.setMethod(wsReqConf.getHTTPMethod());
			}
			//
			super.setRequestConfig(reqConf);
		}
		return this;
	}
	//
	@Override
	public final WSIOTask<T> setDataItem(final T dataItem) {
		try {
			super.setDataItem(dataItem);
			wsReqConf.applyDataItem(httpRequest, dataItem);
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.WARN, e, "Failed to apply data item");
		}
		return this;
	}
	//
	private final static Map<String, HttpHost> HTTP_HOST_MAP = new ConcurrentHashMap<>();
	@Override
	public final WSIOTask<T> setNodeAddr(final String nodeAddr)
	throws IllegalStateException {
		super.setNodeAddr(nodeAddr);
		HttpHost tgtHost = null;
		if(HTTP_HOST_MAP.containsKey(nodeAddr)) {
			tgtHost = HTTP_HOST_MAP.get(nodeAddr);
		} else if(nodeAddr != null) {
			if(nodeAddr.contains(RequestConfig.HOST_PORT_SEP)) {
				try {
					final String nodeAddrParts[] = nodeAddr.split(RequestConfig.HOST_PORT_SEP);
					if(nodeAddrParts.length == 2) {
						tgtHost = new HttpHost(
							nodeAddrParts[0],
							Integer.valueOf(nodeAddrParts[1]), wsReqConf.getScheme()
						);
					} else {
						throw new InterruptedException("Stop due to irrecoverable failure");
					}
				} catch(final Exception e) {
					LogUtil.failure(
						LOG, Level.WARN, e, "Invalid syntax of storage address: " + nodeAddr
					);
					throw new IllegalStateException("Stop due to unrecoverable failure");
				}
			} else {
				tgtHost = new HttpHost(
					nodeAddr, wsReqConf.getPort(), wsReqConf.getScheme()
				);
			}
			HTTP_HOST_MAP.put(nodeAddr, tgtHost);
		}
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
		return HTTP_HOST_MAP.get(nodeAddr);
	}
	//
	@Override
	public final HttpRequest generateRequest()
	throws IOException, HttpException {
		try {
			wsReqConf.applyHeadersFinally(httpRequest);
			reqEntity = httpRequest.getEntity();
			if(LOG.isTraceEnabled(LogUtil.MSG)) {
				LOG.trace(
					LogUtil.MSG, "Task #{}: generating the request w/ {} bytes of content",
					hashCode(), reqEntity == null ? "NO" : reqEntity.getContentLength()
				);
			}
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.WARN, e, "Failed to apply the final headers");
		}
		reqTimeStart = System.nanoTime() / 1000;
		return httpRequest;
	}
	//
	@Override
	public final void produceContent(final ContentEncoder out, final IOControl ioCtl)
	throws IOException {
		try(final WritableByteChannel chanOut = HTTPContentEncoderChannel.getInstance(out)) {
			if(reqEntity != null) {
				if(dataItem.isAppending()) {
					dataItem.writeAugment(chanOut);
				} else if(dataItem.hasUpdatedRanges()) {
					dataItem.writeUpdates(chanOut);
				} else {
					dataItem.write(chanOut);
				}
			}
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.WARN, e, "Producing content failure");
		}
	}
	//
	@Override
	public final void requestCompleted(final HttpContext context) {
		reqTimeDone = System.nanoTime() / 1000;
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(LogUtil.MSG, "Task #{}: request sent completely", hashCode());
		}
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
				httpRequest.clearHeaders();
				httpRequest.setHeaders(sharedHeaders.getAllHeaders());
				httpRequest.setEntity(reqEntity);
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(
						LogUtil.MSG, "Task #{}: reset the request, left headers: {}, shared headers: {}",
						hashCode(), Arrays.toString(httpRequest.getAllHeaders()),
						Arrays.toString(sharedHeaders.getAllHeaders())
					);
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
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(LogUtil.MSG, "#{}, got response \"{}\"", hashCode(), status);
		}
		respStatusCode = status.getStatusCode();
		//
		if(respStatusCode < 200 || respStatusCode >= 300) {
			//
			final StringBuilder msgBuff = new StringBuilder();
			//
			switch(respStatusCode) {
				case (100):
					msgBuff.append("\"100/continue\" response is not supported\n");
					this.status = Status.FAIL_CLIENT;
					break;
				case (400):
					synchronized(LOG) {
						msgBuff
							.append("Incorrect request: \"")
							.append(httpRequest.getRequestLine()).append("\"\n");
						if(LOG.isTraceEnabled(LogUtil.ERR)) {
							for(final Header rangeHeader : httpRequest.getAllHeaders()) {
								msgBuff
									.append('\t').append(rangeHeader.getName()).append(": ")
									.append(rangeHeader.getValue()).append('\n');
							}
						}
					}
					this.status = Status.FAIL_CLIENT;
					break;
				case (403):
					msgBuff.append("Access failure for data item: \"").append(dataItem);
					if(LOG.isTraceEnabled(LogUtil.ERR)) {
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
					this.status = Status.FAIL_AUTH;
					break;
				case (404):
					msgBuff
						.append("Not found: ").append(httpRequest.getUriAddr())
						.append(httpRequest.getUriPath()).append('\n');
					this.status = Status.FAIL_NOT_FOUND;
					break;
				case (405):
					msgBuff
						.append("The operation is not allowed: \"")
						.append(httpRequest.getRequestLine())
						.append("\"\n");
					this.status = Status.FAIL_CLIENT;
					break;
				case (409):
					msgBuff
						.append("Conflicting resource modification on \"")
						.append(httpRequest.getUriPath())
						.append("\"\n");
					this.status = Status.FAIL_CLIENT;
					break;
				case (411):
					msgBuff
						.append("Content length is required\n");
					this.status = Status.FAIL_CLIENT;
					break;
				case (413):
					msgBuff
						.append("Content is too large: ")
						.append(SizeUtil.formatSize(transferSize))
						.append('\n');
					this.status = Status.FAIL_SVC;
					break;
				case (414):
					msgBuff
						.append("URI is too long: \"")
						.append(httpRequest.getUriPath()).append("\"\n");
					this.status = Status.FAIL_CLIENT;
					break;
				case (415):
					msgBuff
						.append("Unsupported media type: \"")
						.append(httpRequest.getEntity().getContentType())
						.append("\"\n");
					this.status = Status.FAIL_SVC;
					break;
				case (416):
					synchronized(LOG) {
						msgBuff.append("Incorrect range\n");
						if(LOG.isTraceEnabled(LogUtil.ERR)) {
							for(
								final Header rangeHeader : httpRequest.getHeaders(HttpHeaders.RANGE)
							) {
								msgBuff
									.append("Incorrect range ").append(rangeHeader.getValue())
									.append(" for data item ").append(dataItem.getId())
									.append('\n');
							}
						}
					}
					this.status = Status.FAIL_CLIENT;
					break;
				case (429):
					msgBuff.append("Storage prays about a mercy\n");
					this.status = Status.FAIL_SVC;
					break;
				case (500):
					msgBuff.append("Storage internal failure\n");
					this.status = Status.FAIL_SVC;
					break;
				case (501):
					msgBuff.append("Not implemented\n");
					this.status = Status.FAIL_SVC;
					break;
				case (502):
					msgBuff.append("Bad gateway\n");
					this.status = Status.FAIL_SVC;
					break;
				case (503):
					msgBuff.append("Storage prays about a mercy\n");
					this.status = Status.FAIL_SVC;
					break;
				case (504):
					msgBuff.append("Gateway timeout\n");
					this.status = Status.FAIL_TIMEOUT;
					break;
				case (505):
					msgBuff
						.append("HTTP version is not supported: ")
						.append(httpRequest.getProtocolVersion())
						.append("\n");
					this.status = Status.FAIL_TIMEOUT;
					break;
				case (507):
					msgBuff.append("Not enough space is left on the storage\n");
					this.status = Status.FAIL_NO_SPACE;
					break;
				default:
					msgBuff
						.append("Unsupported response code: ").append(respStatusCode)
						.append('\n');
					this.status = Status.FAIL_UNKNOWN;
					break;
			}
			//
			LOG.debug(
				LogUtil.ERR, "Task #{}: {}{}/{} <- {} {}{}",
				hashCode(), msgBuff, respStatusCode, status.getReasonPhrase(),
				httpRequest.getMethod(), httpRequest.getUriAddr(), httpRequest.getUriPath()
			);
		} else {
			this.status = Status.SUCC;
			wsReqConf.receiveResponse(response, dataItem);
		}
	}
	//
	@Override
	public final void consumeContent(final ContentDecoder in, final IOControl ioCtl)
	throws IOException {
		if(respStatusCode < 200 || respStatusCode >= 300) { // failure, no big data is expected
			final StringBuilder msgBuilder = new StringBuilder();
			final ByteBuffer bbuff = ByteBuffer.allocate(0x1000);
			while(in.read(bbuff) >= 0) {
				msgBuilder.append(bbuff.asCharBuffer().toString());
				bbuff.clear();
			}
			if(LOG.isTraceEnabled(LogUtil.ERR)) {
				LOG.trace(LogUtil.ERR, msgBuilder);
			}
		} else {
			if(!wsReqConf.consumeContent(in, ioCtl, dataItem)) { // content corruption
				status = Status.FAIL_CORRUPT;
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
		/*if(wsReqConf != null && !wsReqConf.isClosed()) {
			TraceLogger.failure(LOG, Level.WARN, e, "I/O task failure");
		} else if(
			ConnectionClosedException.class.isInstance(e) ||
			IllegalStateException.class.isInstance(e)
		) {
			TraceLogger.failure(LOG, Level.TRACE, e, "Looks like dropped I/O task");
		} else {
			TraceLogger.failure(LOG, Level.WARN, e, "I/O task failure");
		}*/
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
	}
	//
	@Override
	public final void cancelled() {
	}
}
