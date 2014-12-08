package com.emc.mongoose.web.api.impl;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.impl.IOTaskBase;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.impl.UniformData;
import com.emc.mongoose.util.pool.BasicInstancePool;
import com.emc.mongoose.web.api.MutableHTTPRequest;
import com.emc.mongoose.web.api.WSClient;
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
import org.apache.http.nio.ContentDecoderChannel;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.ContentEncoderChannel;
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
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
/**
 Created by kurila on 06.06.14.
 */
public class BasicWSIOTask<T extends WSObject>
extends IOTaskBase<T>
implements WSIOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static WSIOTask<WSObject> POISON = new BasicWSIOTask<WSObject>() {
		@Override
		public final void execute()
		throws InterruptedException {
			throw new InterruptedException("Attempted to eat the poison");
		}
	};
	//
	protected WSRequestConfig<T> wsReqConf = null; // overrides RequestBase.reqConf field
	protected MutableHTTPRequest httpRequest = null;
	public BasicWSIOTask() {
		super();
	}
	//
	@SuppressWarnings("unchecked")
	public static AsyncIOTask getInstanceFor(
		final RequestConfig reqConf, final DataItem dataItem
	) {
		AsyncIOTask ioTask;
		if(dataItem == null) {
			LOG.debug(Markers.MSG, "Preparing poison request");
			ioTask = POISON;
		} else {
			BasicInstancePool pool;
			synchronized(POOL_MAP) {
				if(POOL_MAP.containsKey(reqConf)) {
					pool = POOL_MAP.get(reqConf);
				} else {
					pool = new BasicInstancePool<>(BasicWSIOTask.class);
					POOL_MAP.put(reqConf, pool);
				}
			}
			ioTask = IOTaskBase.class.cast(pool.take())
				.setRequestConfig(reqConf)
				.setDataItem(dataItem);
		}
		return ioTask;
	}
	//
	@Override
	public WSIOTask<T> setRequestConfig(final RequestConfig<T> reqConf) {
		if(this.wsReqConf == null) { // request instance has not been configured yet?
			this.wsReqConf = (WSRequestConfig<T>) reqConf;
			httpRequest = wsReqConf.createRequest();
		} else { // cleanup
			httpRequest.removeHeaders(HttpHeaders.RANGE);
			httpRequest.removeHeaders(WSRequestConfig.KEY_EMC_SIG);
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
	@Override
	public void execute()
	throws InterruptedException {
		//
		wsReqConf.applyHeadersFinally(httpRequest);
		//
		final WSClient<T> client = wsReqConf.getClient();
		final Future<AsyncIOTask.Result> futureResult = client.submit(this);
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
	//
	@Override
	public final HttpRequest generateRequest()
	throws IOException, HttpException {
		reqEntity = httpRequest.getEntity();
		return httpRequest;
	}
	//
	private final static class ContentOutputStream
	extends OutputStream {
		//
		private ByteBuffer bb = null;
		private byte[] bs = null; // Invoker's previous array
		private byte[] b1 = null;
		private final ContentEncoder out;
		private final IOControl ioCtl;
		//
		protected ContentOutputStream(final ContentEncoder out, final IOControl ioCtl) {
			this.out = out;
			this.ioCtl = ioCtl;
		}
		//
		@Override
		public synchronized void write(int b)
		throws IOException {
			if(b1 == null) {
				b1 = new byte[1];
			}
			b1[0] = (byte) b;
			this.write(b1);
		}
		//
		@Override
		public final synchronized void write(byte[] bs, int off, int len)
		throws IOException {
			if(
				(off < 0) || (off > bs.length) || (len < 0) || ((off + len) > bs.length) ||
				((off + len) < 0)
			) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return;
			}
			final ByteBuffer bb = ((this.bs == bs) ? this.bb : ByteBuffer.wrap(bs));
			bb.limit(Math.min(off + len, bb.capacity()));
			bb.position(off);
			this.bb = bb;
			this.bs = bs;
			int n;
			while(bb.remaining() > 0) {
				n = out.write(bb);
				if(n <= 0) {
					LOG.debug(Markers.ERR, "No bytes written");
					ioCtl.requestOutput();
				}
			}
		}
		//
		public final void close()
		throws IOException {
			out.complete();
		}
	}
	//
	@Override
	public final void produceContent(final ContentEncoder out, final IOControl ioCtl)
	throws IOException {
		try(final ContentOutputStream outStream = new ContentOutputStream(out, ioCtl)) {
			reqEntity.writeTo(outStream);
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, String.format("%s: failed to write the content", dataItem.getId()));
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
		result = Result.FAIL_UNKNOWN;
		reqTimeStart = 0;
		reqTimeDone = 0;
		reqEntity = null;
		respStatusCode = -1;
		respTimeStart = 0;
		respTimeDone = 0;
		respFlagCancel = false;
		respFlagDone = false;
		exception = null;
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
				Markers.MSG, "{}/{} <- {} {}{}", respStatusCode, status.getReasonPhrase(),
				httpRequest.getMethod(), httpRequest.getUriAddr(), httpRequest.getUriPath()
			);
		}
		//
		if(respStatusCode < 200 || respStatusCode >= 300) {
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
					LOG.warn(
						Markers.ERR, "Not found: {}{}",
						httpRequest.getUriAddr(), httpRequest.getUriPath()
					);
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
		} else {
			result = Result.SUCC;
			wsReqConf.receiveResponse(response, dataItem);
		}
	}
	//
	@Override
	public final void consumeContent(final ContentDecoder in, final IOControl ioCtl)
	throws IOException {
		final InputStream contentStream = Channels.newInputStream(new ContentDecoderChannel(in));
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
