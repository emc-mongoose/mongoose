package com.emc.mongoose.web.api.impl;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.api.impl.BasicObjectIOTask;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.io.HTTPContentInputStream;
import com.emc.mongoose.util.io.HTTPContentOutputStream;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.collections.InstancePool;
import com.emc.mongoose.web.api.MutableHTTPRequest;
import com.emc.mongoose.web.api.WSIOTask;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.commons.lang.text.StrBuilder;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeader;
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
	) throws InterruptedException {
		final BasicWSIOTask<T> ioTask = (BasicWSIOTask<T>) POOL_WEB_IO_TASKS.take(reqConf, dataItem, nodeAddr);
		LOG.trace(
			Markers.MSG,
			String.format(
				"linked task #%d to {%s, %x, %s}",
				ioTask.hashCode(), reqConf, dataItem.getOffset(), nodeAddr
			)
		);
		return ioTask;
	}
	//
	@Override
	public void release() {
		if(isAvailable.compareAndSet(false, true)) {
			if(LOG.isTraceEnabled(Markers.MSG)) {
				TraceLogger.trace(
					LOG, Level.TRACE, Markers.MSG,
					String.format("Releasing the task #%d", hashCode())
				);
			}
			resetRequest();
			POOL_WEB_IO_TASKS.release(this);
		} else {
			LOG.warn(Markers.ERR, "Not closing already closed task #{}", hashCode());
		}
	}
	// END pool related things
	protected WSRequestConfig<T> wsReqConf = null; // overrides RequestBase.reqConf field
	protected Map<String, Header> sharedHeadersMap = null;
	protected final MutableHTTPRequest httpRequest = HTTPMethod.GET.createRequest();
	protected volatile HttpEntity reqEntity = null;
	//
	@Override
	public synchronized WSIOTask<T> setRequestConfig(final RequestConfig<T> reqConf) {
		if(reqConf != null && !reqConf.equals(wsReqConf)) {
			this.wsReqConf = (WSRequestConfig<T>) reqConf;
			//
			final Map<String, String> _headersMap = wsReqConf.getSharedHeadersMap();
			sharedHeadersMap = new ConcurrentHashMap<>();
			for(final String headerKey : _headersMap.keySet()) {
				sharedHeadersMap.put(
					headerKey, new BasicHeader(headerKey, _headersMap.get(headerKey))
				);
			}
			//
			if(!httpRequest.getMethod().equals(wsReqConf.getHTTPMethod())) {
				httpRequest.setMethod(wsReqConf.getHTTPMethod());
			}
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
			TraceLogger.failure(LOG, Level.WARN, e, "Failed to apply data item");
		}
		return this;
	}
	//
	private final static Map<String, HttpHost> HTTP_HOST_MAP = new ConcurrentHashMap<>();
	private final static String HOST_PORT_SEP = ":";
	@Override
	public final WSIOTask<T> setNodeAddr(final String nodeAddr)
	throws InterruptedException {
		super.setNodeAddr(nodeAddr);
		HttpHost tgtHost = null;
		if(HTTP_HOST_MAP.containsKey(nodeAddr)) {
			tgtHost = HTTP_HOST_MAP.get(nodeAddr);
		} else if(nodeAddr != null) {
			if(nodeAddr.contains(HOST_PORT_SEP)) {
				try {
					final String nodeAddrParts[] = nodeAddr.split(HOST_PORT_SEP);
					if(nodeAddrParts.length == 2) {
						tgtHost = new HttpHost(
							nodeAddrParts[0], Integer.valueOf(nodeAddrParts[1]), wsReqConf.getScheme()
						);
					} else {
						throw new InterruptedException("Stop due to irrecoverable failure");
					}
				} catch(final Exception e) {
					TraceLogger.failure(
						LOG, Level.WARN, e,
						String.format("Invalid syntax of storage address \"%s\"", nodeAddr)
					);
					throw new InterruptedException("Stop due to unrecoverable failure");
				}
			} else {
				tgtHost = new HttpHost(
					nodeAddr, wsReqConf.getPort(), wsReqConf.getScheme()
				);
			}
			HTTP_HOST_MAP.put(nodeAddr, tgtHost);
		}
		httpRequest.setUriAddr(tgtHost.toURI());
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
			if(LOG.isTraceEnabled()) {
				LOG.trace(
					Markers.MSG, "Task #{}: generating the request w/ {} bytes of content",
					hashCode(), reqEntity == null ? "NO" : reqEntity.getContentLength()
				);
			}
		} catch(final Exception e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Failed to apply the final headers");
		}
		reqTimeStart = System.nanoTime() / 1000;
		return httpRequest;
	}
	//
	@Override
	public final void produceContent(final ContentEncoder out, final IOControl ioCtl)
	throws IOException {
		try(final OutputStream outStream = HTTPContentOutputStream.getInstance(out, ioCtl)) {
			if(reqEntity != null) {
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "Task #{}, write out {} bytes",
						hashCode(), reqEntity.getContentLength()
					);
				}
				reqEntity.writeTo(outStream);
			}
		} catch(final InterruptedException e) {
			// do nothing
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
				for(final String headerKey : sharedHeadersMap.keySet()) {
					httpRequest.setHeader(sharedHeadersMap.get(headerKey));
				}
				httpRequest.setEntity(reqEntity);
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "Task #{}: reset the request, left headers: {}, shared headers: {}",
						hashCode(), Arrays.toString(httpRequest.getAllHeaders()),
						Arrays.toString(sharedHeadersMap.keySet().toArray())
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
						if(LOG.isTraceEnabled(Markers.ERR)) {
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
					msgBuff
						.append("Access failure for data item: \"").append(dataItem)
						.append("\"\nSource request headers:\n");
					if(LOG.isTraceEnabled(Markers.ERR)) {
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
						.append(RunTimeConfig.formatSize(transferSize))
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
						if(LOG.isTraceEnabled(Markers.ERR)) {
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
				Markers.ERR, "Task #{}: {}{}/{} <- {} {}{}",
				hashCode(), msgBuff, respStatusCode, status.getReasonPhrase(),
				httpRequest.getMethod(), httpRequest.getUriAddr(), httpRequest.getUriPath()
			);
		} else {
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Task #{} is successful", hashCode());
			}
			this.status = Status.SUCC;
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
		} catch(final InterruptedException e) {
			// do nothing
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
	public final AsyncIOTask.Status getResult() {
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
}
