package com.emc.mongoose.storage.driver.net.http.base;

import com.emc.mongoose.model.io.task.DataIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.load.LoadType;
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
public class BasicClientApiHandler<I extends Item, O extends IoTask<I>>
extends ClientHandlerBase<HttpObject, I, O> {

	private static final Logger LOG = LogManager.getLogger();
	
	public BasicClientApiHandler(
		final HttpStorageDriverBase<I, O> driver, final boolean verifyFlag
	) {
		super(driver, verifyFlag);
	}

	@Override
	protected void handle(final Channel channel, final O ioTask, final HttpObject msg)
	throws IOException {
		
		if(msg instanceof HttpResponse) {
			ioTask.startResponse();
			final HttpResponse httpResponse = (HttpResponse) msg;
			final HttpResponseStatus httpResponseStatus = httpResponse.status();
			final HttpStatusClass statusClass = httpResponseStatus.codeClass();
			switch(statusClass) {
				case INFORMATIONAL:
					ioTask.setStatus(RESP_FAIL_CLIENT);
					break;
				case SUCCESS:
					ioTask.setStatus(SUCC);
					break;
				case REDIRECTION:
					ioTask.setStatus(RESP_FAIL_CLIENT);
					break;
				case CLIENT_ERROR:
					if(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE.equals(httpResponseStatus)) {
						ioTask.setStatus(RESP_FAIL_SVC);
					} else if(HttpResponseStatus.REQUEST_URI_TOO_LONG.equals(httpResponseStatus)) {
						ioTask.setStatus(RESP_FAIL_SVC);
					} else if(HttpResponseStatus.UNAUTHORIZED.equals(httpResponseStatus)) {
						ioTask.setStatus(RESP_FAIL_AUTH);
					} else if(HttpResponseStatus.FORBIDDEN.equals(httpResponseStatus)) {
						ioTask.setStatus(RESP_FAIL_AUTH);
					} else if(HttpResponseStatus.NOT_FOUND.equals(httpResponseStatus)) {
						ioTask.setStatus(RESP_FAIL_NOT_FOUND);
					} else {
						ioTask.setStatus(RESP_FAIL_CLIENT);
					}
					break;
				case SERVER_ERROR:
					if(HttpResponseStatus.GATEWAY_TIMEOUT.equals(httpResponseStatus)) {
						ioTask.setStatus(FAIL_TIMEOUT);
					} else if(HttpResponseStatus.INSUFFICIENT_STORAGE.equals(httpResponseStatus)) {
						ioTask.setStatus(RESP_FAIL_SPACE);
					} else {
						ioTask.setStatus(RESP_FAIL_SVC);
					}
					break;
				case UNKNOWN:
					ioTask.setStatus(FAIL_UNKNOWN);
					break;
			}
			
			/*final String cl = httpResponse.headers().get(HttpHeaderNames.CONTENT_LENGTH);
			if(cl != null) {
				try {
					final int contentLength = Integer.parseInt(cl);
					if(contentLength < 1) {
						if(ioTask instanceof DataIoTask) {
							final DataIoTask dataIoTask = (DataIoTask) ioTask;
							dataIoTask.setRespDataTimeStart(System.nanoTime() / 1000);
						}
						ioTask.setRespTimeDone(System.nanoTime() / 1000);
						ctx.close();
						ioTaskOutputRef.get().ioTaskCompleted((O) ioTask);
					}
				} catch(final NumberFormatException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Invalid response content length value");
					ioTask.setStatus(RESP_FAIL_SVC);
				}
			}*/
			
			return;
		}

		if(msg instanceof HttpContent) {
			if(LoadType.READ.equals(ioTask.getLoadType())) {
				if(ioTask instanceof DataIoTask) {
					final DataIoTask dataIoTask = (DataIoTask) ioTask;
					final long countBytesDone = dataIoTask.getCountBytesDone();
					if(dataIoTask.getDataLatency() == -1) { // if not set yet - 1st time
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
