package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.ui.log.LogUtil;

import static com.emc.mongoose.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.model.io.task.IoTask.Status.INTERRUPTED;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_IO;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_UNKNOWN;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 Created by kurila on 04.10.16.
 Contains the content validation functionality
 */
public abstract class ResponseHandlerBase<M, I extends Item, O extends IoTask<I>>
extends SimpleChannelInboundHandler<M> {
	
	protected final NetStorageDriverBase<I, O> driver;
	protected final boolean verifyFlag;
	
	protected ResponseHandlerBase(final NetStorageDriverBase<I, O> driver, boolean verifyFlag) {
		this.driver = driver;
		this.verifyFlag = verifyFlag;
	}
	
	@Override @SuppressWarnings("unchecked")
	protected final void channelRead0(final ChannelHandlerContext ctx, final M msg)
	throws Exception {
		final Channel channel = ctx.channel();
		final O ioTask = (O) channel.attr(NetStorageDriver.ATTR_KEY_IOTASK).get();
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			handle(channel, ioTask, msg);
		}
	}
	
	protected abstract void handle(final Channel channel, final O ioTask, final M msg)
	throws IOException;

	@Override @SuppressWarnings("unchecked")
	public final void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
	throws IOException {
		final Channel channel = ctx.channel();
		final O ioTask = (O) channel.attr(NetStorageDriver.ATTR_KEY_IOTASK).get();
		if(ioTask != null) {
			if(driver.isInterrupted() || driver.isClosed()) {
				ioTask.setStatus(INTERRUPTED);
			} else if(cause instanceof PrematureChannelClosureException) {
				LogUtil.exception(Level.WARN, cause, "Premature channel closure");
				ioTask.setStatus(FAIL_IO);
			} else {
				LogUtil.exception(Level.WARN, cause, "Client handler failure");
				ioTask.setStatus(FAIL_UNKNOWN);
			}
			if(!driver.isInterrupted()) {
				try {
					driver.complete(channel, ioTask);
				} catch(final Exception e) {
					LogUtil.exception(Level.DEBUG, e, "Failed to complete the I/O task");
				}
			}
		}
	}

	@Override
	public final void userEventTriggered(final ChannelHandlerContext ctx, final Object evt)
	throws Exception {
		if(evt instanceof IdleStateEvent) {
			throw new SocketTimeoutException();
		}
	}
}
