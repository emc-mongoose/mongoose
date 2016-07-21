package com.emc.mongoose.storage.mock.impl.http.request;

import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageIoStats;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.ui.config.Config;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

/**
 Created on 12.07.16.
 */
@Sharable
public abstract class RequestHandlerBase<T extends MutableDataItemMock>
extends ChannelInboundHandlerAdapter {

	private final static Logger LOG = LogManager.getLogger();

	private final double rateLimit;
	private final AtomicInteger lastMilliDelay = new AtomicInteger(1);

	protected final StorageMock<T> sharedStorage;
	private final StorageIoStats ioStats;

	protected final String requestKey = "requestKey";
	protected final String responseStatusKey = "responseStatusKey";
	protected final String contentLengthKey = "contentLengthKey";
	protected final String ctxWriteFlagKey = "ctxWriteFlagKey";
	protected final String handlerStatus = "handlerStatus";

	public RequestHandlerBase(
		final Config.LoadConfig.LimitConfig limitConfig,
		final StorageMock<T> sharedStorage
	) {
		this.rateLimit = limitConfig.getRate();
		this.sharedStorage = sharedStorage;
		this.ioStats = sharedStorage.getStats();
		AttributeKey.<HttpRequest>valueOf(requestKey);
		AttributeKey.<HttpResponseStatus>valueOf(responseStatusKey);
		AttributeKey.<Long>valueOf(contentLengthKey);
		AttributeKey.<Boolean>valueOf(ctxWriteFlagKey);
		AttributeKey.<Boolean>valueOf(handlerStatus);
	}

	protected abstract boolean checkApiMatch(final HttpRequest request);

	@Override
	public void channelReadComplete(final ChannelHandlerContext ctx)
	throws Exception {
		if (!ctx.channel().attr(AttributeKey.<Boolean>valueOf(handlerStatus)).get()) {
			ctx.fireChannelReadComplete();
			return;
		}
		ctx.flush();
	}

	private void processHttpRequest(final ChannelHandlerContext ctx, final HttpRequest request) {
		final Channel channel = ctx.channel();
		final HttpHeaders headers = request.headers();
		channel.attr(AttributeKey.<HttpRequest>valueOf(requestKey)).set(request);
		if (headers.contains(CONTENT_LENGTH)) {
			channel.attr(AttributeKey.<Long>valueOf(contentLengthKey)).set(
				Long.parseLong(headers.get(CONTENT_LENGTH)));
		}
	}

	private void processHttpContent(final ChannelHandlerContext ctx, final Object msg) {
		final Channel channel = ctx.channel();
		if (msg instanceof HttpRequest) {
			if (!checkApiMatch((HttpRequest) msg)) {
				channel.attr(AttributeKey.<Boolean>valueOf(handlerStatus)).set(false);
				ctx.fireChannelRead(msg);
				return;
			}
			channel.attr(AttributeKey.<Boolean>valueOf(handlerStatus)).set(true);
			processHttpRequest(ctx, (HttpRequest) msg);
			ReferenceCountUtil.release(msg);
			return;
		}
		if (!channel.attr(AttributeKey.<Boolean>valueOf(handlerStatus)).get()) {
			ctx.fireChannelRead(msg);
			return;
		}
		if (msg instanceof LastHttpContent) {
			handle(ctx);
		}
		ReferenceCountUtil.release(msg);
	}

	public final void handle(final ChannelHandlerContext ctx) {
		if (rateLimit > 0) {
			if (ioStats.getWriteRate() + ioStats.getReadRate() + ioStats.getDeleteRate() > rateLimit) {
				try {
					Thread.sleep(lastMilliDelay.incrementAndGet());
				} catch (InterruptedException e) {
					return;
				}
			} else if (lastMilliDelay.get() > 0) {
				lastMilliDelay.decrementAndGet();
			}
		}
		doHandle(ctx);
	}

	protected abstract void doHandle(final ChannelHandlerContext ctx);

}
