package com.emc.mongoose.storage.driver.net.http.base;

import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.path.PathIoTask;
import com.emc.mongoose.model.io.task.token.TokenIoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.storage.driver.net.base.ResponseHandlerBase;
import static com.emc.mongoose.storage.driver.net.base.data.ResponseContentUtil.verifyChunk;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_TIMEOUT;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_UNKNOWN;
import static com.emc.mongoose.model.io.task.IoTask.Status.RESP_FAIL_AUTH;
import static com.emc.mongoose.model.io.task.IoTask.Status.RESP_FAIL_CLIENT;
import static com.emc.mongoose.model.io.task.IoTask.Status.RESP_FAIL_CORRUPT;
import static com.emc.mongoose.model.io.task.IoTask.Status.RESP_FAIL_NOT_FOUND;
import static com.emc.mongoose.model.io.task.IoTask.Status.RESP_FAIL_SPACE;
import static com.emc.mongoose.model.io.task.IoTask.Status.RESP_FAIL_SVC;
import static com.emc.mongoose.model.io.task.IoTask.Status.SUCC;

import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 Created by kurila on 05.09.16.
 */
public abstract class HttpResponseHandlerBase<I extends Item, O extends IoTask<I>>
extends ResponseHandlerBase<HttpObject, I, O> {

	private static final Logger LOG = LogManager.getLogger();
	
	protected HttpResponseHandlerBase(
		final HttpStorageDriverBase<I, O> driver,
		final boolean verifyFlag
	) {
		super(driver, verifyFlag);
	}

	protected boolean handleResponseStatus(
		final O ioTask, final HttpStatusClass statusClass, final HttpResponseStatus responseStatus
	) {
		switch(statusClass) {
			case INFORMATIONAL:
				LOG.warn(Markers.ERR, "{}: {}", ioTask.toString(), responseStatus.toString());
				ioTask.setStatus(RESP_FAIL_CLIENT);
				break;
			case SUCCESS:
				ioTask.setStatus(SUCC);
				return true;
			case REDIRECTION:
				LOG.warn(Markers.ERR, "{}: {}", ioTask.toString(), responseStatus.toString());
				ioTask.setStatus(RESP_FAIL_CLIENT);
				break;
			case CLIENT_ERROR:
				LOG.warn(Markers.ERR, "{}: {}", ioTask.toString(), responseStatus.toString());
				if(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE.equals(responseStatus)) {
					ioTask.setStatus(RESP_FAIL_SVC);
				} else if(HttpResponseStatus.REQUEST_URI_TOO_LONG.equals(responseStatus)) {
					ioTask.setStatus(RESP_FAIL_SVC);
				} else if(HttpResponseStatus.UNAUTHORIZED.equals(responseStatus)) {
					ioTask.setStatus(RESP_FAIL_AUTH);
				} else if(HttpResponseStatus.FORBIDDEN.equals(responseStatus)) {
					ioTask.setStatus(RESP_FAIL_AUTH);
				} else if(HttpResponseStatus.NOT_FOUND.equals(responseStatus)) {
					ioTask.setStatus(RESP_FAIL_NOT_FOUND);
				} else {
					ioTask.setStatus(RESP_FAIL_CLIENT);
				}
				break;
			case SERVER_ERROR:
				LOG.warn(Markers.ERR, "{}: {}", ioTask.toString(), responseStatus.toString());
				if(HttpResponseStatus.GATEWAY_TIMEOUT.equals(responseStatus)) {
					ioTask.setStatus(FAIL_TIMEOUT);
				} else if(HttpResponseStatus.INSUFFICIENT_STORAGE.equals(responseStatus)) {
					ioTask.setStatus(RESP_FAIL_SPACE);
				} else {
					ioTask.setStatus(RESP_FAIL_SVC);
				}
				break;
			case UNKNOWN:
				LOG.warn(Markers.ERR, "{}: {}", ioTask.toString(), responseStatus.toString());
				ioTask.setStatus(FAIL_UNKNOWN);
				break;
		}
		
		return false;
	}
	
	protected abstract void handleResponseHeaders(final O ioTask, final HttpHeaders respHeaders);

	protected void handleResponseContentChunk(
		final Channel channel, final O ioTask, final ByteBuf contentChunk
	) throws IOException {
		if(IoType.READ.equals(ioTask.getIoType())) {
			if(ioTask instanceof DataIoTask) {
				final DataIoTask dataIoTask = (DataIoTask) ioTask;
				final long countBytesDone = dataIoTask.getCountBytesDone();
				if(dataIoTask.getRespDataTimeStart() == 0) { // if not set yet - 1st time
					dataIoTask.startDataResponse();
				}
				final int chunkSize = contentChunk.readableBytes();
				if(chunkSize > 0) {
					if(verifyFlag) {
						if(!RESP_FAIL_CORRUPT.equals(ioTask.getStatus())) {
							verifyChunk(dataIoTask, countBytesDone, contentChunk, chunkSize);
						}
					} else {
						dataIoTask.setCountBytesDone(countBytesDone + chunkSize);
					}
				}
			} else if(ioTask instanceof PathIoTask) {
				final PathIoTask pathIoTask = (PathIoTask) ioTask;
				final long countBytesDone = pathIoTask.getCountBytesDone();
				if(pathIoTask.getRespDataTimeStart() == 0) { // if not set yet - 1st time
					pathIoTask.startDataResponse();
				}
				final int chunkSize = contentChunk.readableBytes();
				if(chunkSize > 0) {
					pathIoTask.setCountBytesDone(countBytesDone + chunkSize);
				}
			} else if(ioTask instanceof TokenIoTask) {
				final TokenIoTask tokenIoTask = (TokenIoTask) ioTask;
				final long countBytesDone = tokenIoTask.getCountBytesDone();
				if(tokenIoTask.getRespDataTimeStart() == 0) { // if not set yet - 1st time
					tokenIoTask.startDataResponse();
				}
				final int chunkSize = contentChunk.readableBytes();
				if(chunkSize > 0) {
					tokenIoTask.setCountBytesDone(countBytesDone + chunkSize);
				}
			} else {
				throw new AssertionError("Not implemented yet");
			}
		}
	}

	protected void handleResponseContentFinish(final Channel channel, final O ioTask) {
		driver.complete(channel, ioTask);
	}

	@Override
	protected final void handle(final Channel channel, final O ioTask, final HttpObject msg)
	throws IOException {
		
		if(msg instanceof HttpResponse) {
			try {
				ioTask.startResponse();
			} catch(final IllegalStateException e) {
				LogUtil.exception(LOG, Level.TRACE, e, "{}", ioTask.toString());
			}
			final HttpResponse httpResponse = (HttpResponse) msg;
			final HttpResponseStatus httpResponseStatus = httpResponse.status();
			handleResponseStatus(ioTask, httpResponseStatus.codeClass(), httpResponseStatus);
			handleResponseHeaders(ioTask, httpResponse.headers());
			if(msg instanceof FullHttpResponse) {
				final ByteBuf fullRespContent = ((FullHttpResponse) msg).content();
				handleResponseContentChunk(channel, ioTask, fullRespContent);
			}
		}

		if(msg instanceof HttpContent) {
			handleResponseContentChunk(channel, ioTask, ((HttpContent) msg).content());
			if(msg instanceof LastHttpContent) {
				handleResponseContentFinish(channel, ioTask);
			}
		}
	}
}
