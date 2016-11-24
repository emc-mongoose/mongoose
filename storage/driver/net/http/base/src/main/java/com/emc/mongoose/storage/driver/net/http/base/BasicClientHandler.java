package com.emc.mongoose.storage.driver.net.http.base;

import com.emc.mongoose.model.io.task.DataIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.storage.driver.net.base.ClientHandlerBase;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_TIMEOUT;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_UNKNOWN;
import static com.emc.mongoose.model.io.task.IoTask.Status.RESP_FAIL_AUTH;
import static com.emc.mongoose.model.io.task.IoTask.Status.RESP_FAIL_CLIENT;
import static com.emc.mongoose.model.io.task.IoTask.Status.RESP_FAIL_NOT_FOUND;
import static com.emc.mongoose.model.io.task.IoTask.Status.RESP_FAIL_SPACE;
import static com.emc.mongoose.model.io.task.IoTask.Status.RESP_FAIL_SVC;
import static com.emc.mongoose.model.io.task.IoTask.Status.SUCC;

import com.emc.mongoose.ui.log.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
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
public class BasicClientHandler<I extends Item, O extends IoTask<I, R>, R extends IoResult>
extends ClientHandlerBase<HttpObject, I, O, R> {

	private static final Logger LOG = LogManager.getLogger();
	
	public BasicClientHandler(
		final HttpStorageDriverBase<I, O, R> driver,
		final boolean verifyFlag
	) {
		super(driver, verifyFlag);
	}

	private boolean handleResponseStatus(
		final O ioTask, final HttpStatusClass statusClass, final HttpResponseStatus responseStatus
	) {
		switch(statusClass) {
			case INFORMATIONAL:
				ioTask.setStatus(RESP_FAIL_CLIENT);
				break;
			case SUCCESS:
				ioTask.setStatus(SUCC);
				return true;
			case REDIRECTION:
				ioTask.setStatus(RESP_FAIL_CLIENT);
				break;
			case CLIENT_ERROR:
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
				if(HttpResponseStatus.GATEWAY_TIMEOUT.equals(responseStatus)) {
					ioTask.setStatus(FAIL_TIMEOUT);
				} else if(HttpResponseStatus.INSUFFICIENT_STORAGE.equals(responseStatus)) {
					ioTask.setStatus(RESP_FAIL_SPACE);
				} else {
					ioTask.setStatus(RESP_FAIL_SVC);
				}
				break;
			case UNKNOWN:
				ioTask.setStatus(FAIL_UNKNOWN);
				break;
		}
		
		return false;
	}
	
	protected void handleResponseHeaders(final O ioTask, final HttpHeaders respHeaders) {
	}
	
	@Override
	protected void handle(final Channel channel, final O ioTask, final HttpObject msg)
	throws IOException {
		
		if(msg instanceof HttpResponse) {
			ioTask.startResponse();
			final HttpResponse httpResponse = (HttpResponse) msg;
			final HttpResponseStatus httpResponseStatus = httpResponse.status();
			handleResponseStatus(ioTask, httpResponseStatus.codeClass(), httpResponseStatus);
			handleResponseHeaders(ioTask, httpResponse.headers());
			return;
		}

		if(msg instanceof HttpContent) {
			if(IoType.READ.equals(ioTask.getIoType())) {
				if(ioTask instanceof DataIoTask) {
					final DataIoTask dataIoTask = (DataIoTask) ioTask;
					final long countBytesDone = dataIoTask.getCountBytesDone();
					if(dataIoTask.getRespDataTimeStart() > 0) { // if not set yet - 1st time
						dataIoTask.startDataResponse();
					}
					final ByteBuf contentChunk = ((HttpContent) msg).content();
					final int chunkSize = contentChunk.readableBytes();
					if(chunkSize > 0) {
						if(verifyFlag) {
							try {
								verifyChunk(channel, ioTask, contentChunk, chunkSize);
							} catch(final InterruptedException e) {
								LogUtil.exception(
									LOG, Level.WARN, e,
									"Failed to schedule the chunk verification task"
								);
							}
						} else {
							dataIoTask.setCountBytesDone(countBytesDone + chunkSize);
						}
					}
				}
			}
		}

		if(msg instanceof LastHttpContent) {
			driver.complete(channel, ioTask);
		}
	}
}
