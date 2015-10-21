package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.ContentUtil;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.api.io.task.WSContainerIOTask;
//
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.protocol.BasicHttpContext;
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
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
/**
 Created by kurila on 20.10.15.
 */
public class BasicWSContainerTask<T extends WSObject, C extends Container<T>>
extends BasicIOTask<C>
implements WSContainerIOTask<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile Exception exception = null;
	@SuppressWarnings("FieldCanBeLocal")
	private volatile int respStatusCode = -1;
	//
	public BasicWSContainerTask(
		final C item, final String nodeAddr, final RequestConfig reqConf
	) {
		super(item, nodeAddr, reqConf);
	}
	//
	@Override
	public final HttpHost getTarget() {
		return ((WSRequestConfig) reqConf).getNodeHost(nodeAddr);
	}
	//
	@Override
	public final HttpRequest generateRequest()
	throws IOException, HttpException {
		final HttpEntityEnclosingRequest httpRequest;
		try {
			httpRequest = ((WSRequestConfig) reqConf).createContainerRequest(item, nodeAddr);
		} catch(final URISyntaxException e) {
			throw new HttpException("Failed to generate the request", e);
		}
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "I/O task #{}: generated the request: {}",
				hashCode(), httpRequest
			);
		}
		reqTimeStart = System.nanoTime() / 1000;
		return httpRequest;
	}
	//
	@Override
	public final void produceContent(final ContentEncoder encoder, final IOControl ioctrl)
	throws IOException {

	}
	//
	@Override
	public final void requestCompleted(final HttpContext context) {
		reqTimeDone = System.nanoTime() / 1000;
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "I/O task #{}: request sent completely", hashCode());
		}
	}
	//
	@Override
	public final void responseReceived(final HttpResponse response)
	throws IOException, HttpException {
		//
		respTimeStart = System.nanoTime() / 1000;
		final StatusLine status = response.getStatusLine();
		respStatusCode = status.getStatusCode();
		//
		if(respStatusCode < 200 || respStatusCode >= 300) {
			LOG.debug(Markers.ERR, "I/O task #{}: got response \"{}\"", hashCode(), status);
			//
			switch(respStatusCode) {
				case HttpStatus.SC_CONTINUE:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_BAD_REQUEST:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_UNAUTHORIZED:
				case HttpStatus.SC_FORBIDDEN:
					this.status = Status.RESP_FAIL_AUTH;
					break;
				case HttpStatus.SC_NOT_FOUND:
					this.status = Status.RESP_FAIL_NOT_FOUND;
					break;
				case HttpStatus.SC_METHOD_NOT_ALLOWED:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_CONFLICT:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_LENGTH_REQUIRED:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_REQUEST_TOO_LONG:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_REQUEST_URI_TOO_LONG:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case 429:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_NOT_IMPLEMENTED:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_BAD_GATEWAY:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_SERVICE_UNAVAILABLE:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_GATEWAY_TIMEOUT:
					this.status = Status.FAIL_TIMEOUT;
					break;
				case HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_INSUFFICIENT_STORAGE:
					this.status = Status.RESP_FAIL_SPACE;
					break;
				default:
					this.status = Status.FAIL_UNKNOWN;
					break;
			}
		} else {
			this.status = Status.SUCC;
		}
	}
	//
	@Override
	public final void consumeContent(final ContentDecoder decoder, final IOControl ioctrl)
	throws IOException {
		try {
			if(respStatusCode < 200 || respStatusCode >= 300) { // failure, no user data is expected
				consumeFailedResponseContent(decoder, ioctrl);
			} else {
				ContentUtil.consumeQuietly(decoder, Constants.BUFF_SIZE_LO);
			}
		} catch(final ClosedChannelException e) {
			status = Status.CANCELLED;
			LogUtil.exception(LOG, Level.TRACE, e, "Output channel closed during the operation");
		} catch(final IOException e) {
			if(!reqConf.isClosed()) {
				LogUtil.exception(LOG, Level.DEBUG, e, "I/O failure during content consuming");
			}
		}
	}
	//
	private void consumeFailedResponseContent(final ContentDecoder in, final IOControl ioCtl)
	throws IOException {
		final ByteBuffer bbuff = ByteBuffer.allocate(Constants.BUFF_SIZE_LO);
		while(in.read(bbuff) >= 0 && bbuff.remaining() > 0);
		LOG.debug(
			Markers.ERR, "#{}: {} - {}", hashCode(), status.description,
			new String(bbuff.array(), 0, bbuff.position(), StandardCharsets.UTF_8)
		);
	}
	//
	@Override
	public final void responseCompleted(final HttpContext context) {
		respTimeDone = System.nanoTime() / 1000;
	}
	//
	@Override
	public final void failed(final Exception e) {
		if(e instanceof ConnectionClosedException | e instanceof CancelledKeyException) {
			LogUtil.exception(LOG, Level.TRACE, e, "I/O task dropped while executing");
			status = Status.CANCELLED;
			exception = e;
			respTimeDone = System.nanoTime() / 1000;
		} else {
			LogUtil.exception(LOG, Level.DEBUG, e, "I/O task failure");
			status = Status.FAIL_UNKNOWN;
			exception = e;
			respTimeDone = System.nanoTime() / 1000;
		}
	}
	//
	@Override
	public final Exception getException() {
		return exception;
	}
	//
	@Override
	public final WSContainerIOTask<T, C> getResult() {
		return this;
	}
	//
	@Override
	public final boolean isDone() {
		return respTimeDone != 0;
	}
	//
	@Override
	public final boolean isRepeatable() {
		return WSObject.IS_CONTENT_REPEATABLE;
	}
	//
	@Override
	public final void resetRequest()
	throws IOException {
		respStatusCode = -1;
		status = Status.FAIL_UNKNOWN;
		exception = null;
	}
	//
	@Override
	public final boolean cancel() {
		LOG.debug(Markers.MSG, "{}: I/O task cancel", hashCode());
		return false;
	}
	//
	@Override
	public final void close() {
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// HttpContext implementation //////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final HttpContext wrappedHttpCtx = new BasicHttpContext();
	//
	@Override
	public final Object getAttribute(final String id) {
		return wrappedHttpCtx.getAttribute(id);
	}
	//
	@Override
	public final void setAttribute(final String id, final Object obj) {
		wrappedHttpCtx.setAttribute(id, obj);
	}
	//
	@Override
	public final Object removeAttribute(final String id) {
		return wrappedHttpCtx.removeAttribute(id);
	}
}
