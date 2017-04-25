package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.model.io.task.IoTask.Status.CANCELLED;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_IO;
import static com.emc.mongoose.model.io.task.IoTask.Status.FAIL_UNKNOWN;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.timeout.IdleStateEvent;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 Created by kurila on 04.10.16.
 Contains the content validation functionality
 */
public abstract class ResponseHandlerBase<M, I extends Item, O extends IoTask<I>>
extends SimpleChannelInboundHandler<M> {
	
	private static final Logger LOG = LogManager.getLogger();

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
		handle(channel, ioTask, msg);
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
				ioTask.setStatus(CANCELLED);
			} else if(!driver.isInterrupted() && !driver.isClosed()) {
				if(cause instanceof PrematureChannelClosureException) {
					LogUtil.exception(LOG, Level.WARN, cause, "Premature channel closure");
					ioTask.setStatus(FAIL_IO);
				} else {
					LogUtil.exception(LOG, Level.WARN, cause, "Client handler failure");
					ioTask.setStatus(FAIL_UNKNOWN);
				}
			}
			if(!driver.isInterrupted()) {
				try {
					driver.complete(channel, ioTask);
				} catch(final Exception e) {
					LogUtil.exception(LOG, Level.DEBUG, e, "Failed to complete the I/O task");
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
