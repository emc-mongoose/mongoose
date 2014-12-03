package com.emc.mongoose.web.api.impl;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.impl.RequestBase;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.impl.UniformData;
import com.emc.mongoose.util.pool.BasicInstancePool;
import com.emc.mongoose.web.api.WSClient;
import com.emc.mongoose.web.api.WSRequest;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.TruncatedChunkException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
/**
 Created by kurila on 06.06.14.
 */
public class BasicWSRequest<T extends WSObject>
extends RequestBase<T>
implements WSRequest<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static WSRequest<WSObject> POISON = new BasicWSRequest<WSObject>() {
		@Override
		public final void execute()
		throws InterruptedException {
			throw new InterruptedException("Attempted to eat the poison");
		}
	};
	//
	protected WSRequestConfig<T> wsReqConf = null; // overrides RequestBase.reqConf field
	protected HttpRequestBase httpRequest = null;
	public BasicWSRequest() {
		super();
	}
	//
	@SuppressWarnings("unchecked")
	public static Request getInstanceFor(
		final RequestConfig reqConf, final DataItem dataItem
	) {
		Request request;
		if(dataItem == null) {
			LOG.debug(Markers.MSG, "Preparing poison request");
			request = POISON;
		} else {
			BasicInstancePool pool;
			synchronized(POOL_MAP) {
				if(POOL_MAP.containsKey(reqConf)) {
					pool = POOL_MAP.get(reqConf);
				} else {
					pool = new BasicInstancePool<>(BasicWSRequest.class);
					POOL_MAP.put(reqConf, pool);
				}
			}
			request = RequestBase.class.cast(pool.take())
				.setRequestConfig(reqConf)
				.setDataItem(dataItem);
			//assert request != null;
		}
		return request;
	}
	//
	@Override
	public WSRequest<T> setRequestConfig(final RequestConfig<T> reqConf) {
		if(this.wsReqConf == null) { // request instance has not been configured yet?
			this.wsReqConf = (WSRequestConfig<T>) reqConf;
			switch(wsReqConf.getLoadType()) {
				case CREATE:
					httpRequest = com.emc.mongoose.web.api.impl.provider.atmos.RequestConfig
						.class.isInstance(wsReqConf) ?
							new HttpPost() : new HttpPut();
					break;
				case READ:
					httpRequest = new HttpGet();
					break;
				case DELETE:
					httpRequest = new HttpDelete();
					break;
				case UPDATE:
					httpRequest = new HttpPut();
					break;
				case APPEND:
					httpRequest = new HttpPut();
					break;
			}
		} else { // cleanup
			httpRequest.removeHeaders(HttpHeaders.RANGE);
			httpRequest.removeHeaders(WSRequestConfig.KEY_EMC_SIG);
		}
		super.setRequestConfig(reqConf);
		return this;
	}
	//
	@Override
	public final WSRequest<T> setDataItem(final T dataItem) {
		try {
			wsReqConf.applyDataItem(httpRequest, dataItem);
			super.setDataItem(dataItem);
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to apply data item");
		}
		return this;
	}
	//
	@Override
	public void execute()
	throws InterruptedException {
		//
		wsReqConf.applyHeadersFinally(httpRequest);
		//
		final WSClient<T> client = wsReqConf.getClient();
		final Future<Request.Result> futureResult = client.execute(this);
		try {
			futureResult.get();
		} catch(final InterruptedException e) {
			LOG.debug(Markers.ERR, "Interrupted");
		} catch(final ExecutionException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Request execution failure");
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private volatile Exception exception = null;
	private volatile boolean respFlagDone = false, respFlagCancel = false;
	@SuppressWarnings("FieldCanBeLocal")
	private volatile int respStatusCode = -1;
	//
	////////////////////////////////////////////////////////////////////////////////////////////////
	// HttpAsyncRequestProducer implementation /////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static Map<String, HttpHost> HTTP_HOST_MAP = new ConcurrentHashMap<>();
	//
	@Override
	public final HttpHost getTarget() {
		final String tgtAddr = wsReqConf.getAddr();
		if(!HTTP_HOST_MAP.containsKey(tgtAddr)) {
			HTTP_HOST_MAP.put(
				tgtAddr, new HttpHost(tgtAddr, wsReqConf.getPort(), wsReqConf.getScheme())
			);
		}
		return HTTP_HOST_MAP.get(tgtAddr);
	}
	//
	private volatile HttpEntity reqEntity = null;
	private volatile InputStream reqInStream = null;
	@SuppressWarnings("FieldCanBeLocal")
	private volatile int readBytesCount;
	//
	@Override
	public final HttpRequest generateRequest()
	throws IOException, HttpException {
		if(HttpEntityEnclosingRequest.class.isInstance(httpRequest)) {
			reqEntity = HttpEntityEnclosingRequest.class.cast(httpRequest).getEntity();
			reqInStream = reqEntity.getContent();
		}
		return httpRequest;
	}
	//
	private final ByteBuffer outPutBuff = ByteBuffer.allocate(UniformData.MAX_PAGE_SIZE);
	//
	@Override
	public final void produceContent(final ContentEncoder out, final IOControl ioCtl)
	throws IOException {
		try {
			if(reqInStream != null) {
				do {
					readBytesCount = reqInStream.read(outPutBuff.array());
					if(readBytesCount > 0) {
						outPutBuff.flip();
						out.write(outPutBuff);
						outPutBuff.compact();
					}
				} while(!out.isCompleted() && !outPutBuff.hasRemaining());
			}
		} finally {
			out.complete();
			reqInStream.reset();
		}
	}
	//
	@Override
	public final void requestCompleted(final HttpContext context) {
		reqTimeDone = System.nanoTime();
	}
	//
	@Override
	public final boolean isRepeatable() {
		return reqEntity == null || reqEntity.isRepeatable();
	}
	//
	@Override
	public final void resetRequest()
	throws IOException {
		reqTimeStart = 0;
		reqTimeDone = 0;
		reqEntity = null;
		reqInStream = null;
		respTimeStart = 0;
		respTimeDone = 0;
		respFlagCancel = false;
		respFlagDone = false;
		exception = null;
		outPutBuff.clear();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// HttpAsyncResponseConsumer implementation ////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void responseReceived(final HttpResponse response)
	throws IOException, HttpException {
		//
		respTimeStart = System.nanoTime();
		final StatusLine status = response.getStatusLine();
		respStatusCode = status.getStatusCode();
		//
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "{}/{} <- {} {}", respStatusCode, status.getReasonPhrase(),
				httpRequest.getMethod(), httpRequest.getURI()
			);
		}
		//
		if(respStatusCode < 200 || respStatusCode > 299) {
			switch(respStatusCode) {
				case (400):
					LOG.warn(Markers.ERR, "Incorrect request: \"{}\"", httpRequest.getRequestLine());
					result = Result.FAIL_CLIENT;
					break;
				case (403):
					LOG.warn(Markers.ERR, "Access failure");
					result = Result.FAIL_AUTH;
					break;
				case (404):
					LOG.warn(Markers.ERR, "Not found: {}", httpRequest.getURI());
					result = Result.FAIL_NOT_FOUND;
					break;
				case (416):
					LOG.warn(Markers.ERR, "Incorrect range");
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
					LOG.warn(Markers.ERR, "Storage internal failure");
					result = Result.FAIL_SVC;
					break;
				case (503):
					LOG.warn(Markers.ERR, "Storage prays about a mercy");
					result = Result.FAIL_SVC;
					break;
				case (507):
					LOG.warn(Markers.ERR, "Not enough space is left on the storage");
					result = Result.FAIL_NO_SPACE;
				default:
					LOG.error(Markers.ERR, "Unsupported response code: {}", respStatusCode);
					result = Result.FAIL_UNKNOWN;
			}
		}
	}
	//
	@Override
	public final void consumeContent(final ContentDecoder in, final IOControl ioCtl)
	throws IOException {
		wsReqConf.consumeResponse(in, ioCtl, dataItem, respStatusCode);
	}
	//
	@Override
	public final void responseCompleted(final HttpContext context) {
		respTimeDone = System.nanoTime();
		respFlagDone = true;
	}
	//
	@Override
	public final void failed(final Exception e) {
		exception = e;
	}
	//
	@Override
	public final Exception getException() {
		return exception;
	}
	//
	@Override
	public final boolean isDone() {
		return respFlagDone;
	}
	//
	@Override
	public final boolean cancel() {
		respFlagCancel = true;
		return respFlagCancel;
	}
}
