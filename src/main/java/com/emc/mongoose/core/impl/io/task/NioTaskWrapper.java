package com.emc.mongoose.core.impl.io.task;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.ContentUtil;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.executor.HttpLoadExecutor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.EntityAsyncContentProducer;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 Created by kurila on 13.04.16.
 */
public class NioTaskWrapper<T extends Item, A extends IoTask<T>>
extends BasicHttpContext
implements HttpAsyncRequestProducer, HttpAsyncResponseConsumer<A>, HttpContext {
	
	private final static Logger LOG = LogManager.getLogger();
	
	protected final A ioTask;
	protected final T item;
	protected final LoadType ioType;
	protected final String nodeAddr;
	protected final HttpLoadExecutor<T, A> loadExecutor;
	private volatile HttpAsyncContentProducer contentProducer = null;

	public NioTaskWrapper(
		final A ioTask, final String nodeAddr, final HttpLoadExecutor<T, A> loadExecutor
	) {
		this.ioTask = ioTask;
		item = ioTask.getItem();
		ioType = ioTask.getLoadType();
		this.nodeAddr = nodeAddr;
		this.loadExecutor = loadExecutor;
	}

	@Override
	public final HttpHost getTarget() {
		return loadExecutor.getNodeHost(nodeAddr);
	}

	@Override
	public final HttpRequest generateRequest()
	throws IOException, HttpException {
		final HttpEntityEnclosingRequest request = loadExecutor.createRequest(ioTask);
		final HttpEntity httpEntity = request.getEntity();
		if(httpEntity instanceof HttpAsyncContentProducer) {
			contentProducer = (HttpAsyncContentProducer) httpEntity;
		} else if(httpEntity != null) {
			contentProducer = new EntityAsyncContentProducer(httpEntity);
		}
		return request;
	}

	@Override
	public final void requestCompleted(final HttpContext context) {
		ioTask.markRequestDone();
	}

	@Override
	public void produceContent(final ContentEncoder encoder, final IOControl ioctrl)
	throws IOException {
		if(contentProducer != null) {
			contentProducer.produceContent(encoder, ioctrl);
		} else {
			encoder.complete();
		}
	}

	@Override
	public final void responseReceived(final HttpResponse response) {
		ioTask.markResponseStart();
		final StatusLine status = response.getStatusLine();
		final int respStatusCode = status.getStatusCode();
		//
		if(respStatusCode < 200 || respStatusCode >= 300) {
			LOG.debug(Markers.ERR, "I/O task #{}: got response \"{}\"", hashCode(), status);
			//
			switch(respStatusCode) {
				case HttpStatus.SC_CONTINUE:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_CLIENT);
					break;
				case HttpStatus.SC_BAD_REQUEST:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_CLIENT);
					break;
				case HttpStatus.SC_UNAUTHORIZED:
				case HttpStatus.SC_FORBIDDEN:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_AUTH);
					break;
				case HttpStatus.SC_NOT_FOUND:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_NOT_FOUND);
					break;
				case HttpStatus.SC_METHOD_NOT_ALLOWED:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_CLIENT);
					break;
				case HttpStatus.SC_CONFLICT:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_CLIENT);
					break;
				case HttpStatus.SC_LENGTH_REQUIRED:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_CLIENT);
					break;
				case HttpStatus.SC_REQUEST_TOO_LONG:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_SVC);
					break;
				case HttpStatus.SC_REQUEST_URI_TOO_LONG:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_CLIENT);
					break;
				case HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_SVC);
					break;
				case HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_CLIENT);
					break;
				case 429:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_SVC);
					break;
				case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_SVC);
					break;
				case HttpStatus.SC_NOT_IMPLEMENTED:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_SVC);
					break;
				case HttpStatus.SC_BAD_GATEWAY:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_SVC);
					break;
				case HttpStatus.SC_SERVICE_UNAVAILABLE:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_SVC);
					break;
				case HttpStatus.SC_GATEWAY_TIMEOUT:
					ioTask.setStatus(IoTask.Status.FAIL_TIMEOUT);
					break;
				case HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_SVC);
					break;
				case HttpStatus.SC_INSUFFICIENT_STORAGE:
					ioTask.setStatus(IoTask.Status.RESP_FAIL_SPACE);
					break;
				default:
					ioTask.setStatus(IoTask.Status.FAIL_UNKNOWN);
					break;
			}
		} else {
			ioTask.setStatus(IoTask.Status.SUCC);
			// TODO Atmos: extract Location header value and set the object id
		}
	}

	@Override
	public void consumeContent(final ContentDecoder decoder, final IOControl ioctrl) {
		ContentUtil.consumeQuietly(decoder, Constants.BUFF_SIZE_LO);
	}

	@Override
	public void responseCompleted(final HttpContext context) {
		ioTask.markResponseDone(0);
	}

	@Override
	public final void failed(final Exception e) {
	}

	@Override
	public final Exception getException() {
		return null;
	}

	@Override
	public final A getResult() {
		return ioTask;
	}

	@Override
	public final boolean isDone() {
		return false;
	}

	@Override
	public final boolean isRepeatable() {
		return contentProducer != null && contentProducer.isRepeatable();
	}

	@Override
	public final void resetRequest()
	throws IOException {
	}

	@Override
	public final void close()
	throws IOException {
		if(contentProducer != null) {
			contentProducer.close();
		}
	}

	@Override
	public final boolean cancel() {
		return false;
	}
}
