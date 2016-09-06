package com.emc.mongoose.storage.driver.http.base;

import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.Monitor;
import com.emc.mongoose.ui.log.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicReference;

import static com.emc.mongoose.model.api.io.task.IoTask.Status.FAIL_TIMEOUT;
import static com.emc.mongoose.model.api.io.task.IoTask.Status.FAIL_UNKNOWN;
import static com.emc.mongoose.model.api.io.task.IoTask.Status.RESP_FAIL_AUTH;
import static com.emc.mongoose.model.api.io.task.IoTask.Status.RESP_FAIL_CLIENT;
import static com.emc.mongoose.model.api.io.task.IoTask.Status.RESP_FAIL_NOT_FOUND;
import static com.emc.mongoose.model.api.io.task.IoTask.Status.RESP_FAIL_SPACE;
import static com.emc.mongoose.model.api.io.task.IoTask.Status.RESP_FAIL_SVC;
import static com.emc.mongoose.model.api.io.task.IoTask.Status.SUCC;

/**
 Created by kurila on 05.09.16.
 */
public class HttpClientHandlerBase<I extends Item, O extends IoTask<I>>
extends SimpleChannelInboundHandler<HttpObject> {

	private final static Logger LOG = LogManager.getLogger();
	
	private final AtomicReference<Monitor<I, O>> monitorRef;
	
	public HttpClientHandlerBase(final AtomicReference<Monitor<I, O>> monitorRef) {
		this.monitorRef = monitorRef;
	}

	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final HttpObject msg) {
		
		final IoTask ioTask = ctx.channel().attr(HttpDriver.ATTR_KEY_IOTASK).get();
	
		if(msg instanceof HttpResponse) {
			ioTask.setRespTimeStart(System.nanoTime() / 1000);
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
			return;
		}
		
		if(msg instanceof HttpContent) {
			if(ioTask instanceof DataIoTask) {
				final DataIoTask dataIoTask = (DataIoTask) ioTask;
				if(dataIoTask.getDataLatency() == -1) { // if not set yet - 1st time
					dataIoTask.setRespDataTimeStart(System.nanoTime() / 1000);
				}
				final ByteBuf content = ((HttpContent) msg).content();
				dataIoTask.setCountBytesDone(
					dataIoTask.getCountBytesDone() + content.readableBytes()
				);
			}
			if(msg instanceof LastHttpContent) {
				ioTask.setRespTimeDone(System.nanoTime() / 1000);
				ctx.close();
				monitorRef.get().ioTaskCompleted((O) ioTask);
			}
		}
	}
	
	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
		LogUtil.exception(LOG, Level.WARN, cause, "HTTP client handler failure");
		final IoTask ioTask = ctx.channel().attr(HttpDriver.ATTR_KEY_IOTASK).get();
		ctx.close();
		ioTask.setStatus(FAIL_UNKNOWN);
		monitorRef.get().ioTaskCompleted((O) ioTask);
	}
}
