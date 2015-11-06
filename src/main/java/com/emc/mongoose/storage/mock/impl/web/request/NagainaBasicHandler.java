package com.emc.mongoose.storage.mock.impl.web.request;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.data.MutableDataItem;
import com.emc.mongoose.core.api.data.content.ContentSource;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;
import com.emc.mongoose.storage.mock.api.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.channel.ChannelHandler.*;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * Created by Ilia on 30.10.2015.
 */
@Sharable
public class NagainaBasicHandler<T extends WSObjectMock> extends SimpleChannelInboundHandler<Object> {

	private final static Logger LOG = LogManager.getLogger();

	protected final int batchSize;
	private final float rateLimit;
	private final AtomicInteger lastMilliDelay = new AtomicInteger(1);
	private final ContentSource contentSrc = ContentSourceBase.getDefault();

	protected final WSMock<T> sharedStorage;
	private final StorageIOStats ioStats;

	public NagainaBasicHandler(RunTimeConfig rtConfig, WSMock<T> sharedStorage) {
		this.rateLimit = rtConfig.getLoadLimitRate();
		this.batchSize = rtConfig.getBatchSize();
		this.sharedStorage = sharedStorage;
		this.ioStats = sharedStorage.getStats();
	}

	private AttributeKey<HttpRequest> currentHttpRequestKey = AttributeKey.valueOf("currentHttpRequestKey");
	private AttributeKey<HttpResponse> currentHttpResponseKey = AttributeKey.valueOf("currentHttpResponseKey");
	private AttributeKey<String> containerNameKey = AttributeKey.valueOf("containerNameKey");
	private AttributeKey<String> objIdKey = AttributeKey.valueOf("objIdKey");
	private AttributeKey<Long> contentLengthKey = AttributeKey.valueOf("contentLengthKey");

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof HttpRequest) {
			HttpRequest request = (HttpRequest) msg;
			ctx.attr(currentHttpRequestKey).set(request);
			ctx.attr(contentLengthKey).set(0L);
			QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
			String[] pathChunks = queryStringDecoder.path().split("/");
			if (pathChunks.length == 2) {
				ctx.attr(containerNameKey).set(pathChunks[1]);
			} else if (pathChunks.length >= 3) {
				ctx.attr(containerNameKey).set(pathChunks[1]);
				ctx.attr(objIdKey).set(pathChunks[2]);
			}
		}

		if (msg instanceof HttpContent) {
			HttpContent httpContent = (HttpContent) msg;
			ByteBuf content = httpContent.content();
			Long currentContentSize = ctx.attr(contentLengthKey).get();
			ctx.attr(contentLengthKey).set(currentContentSize + content.readableBytes());
		}

		if (msg instanceof LastHttpContent) {
			LastHttpContent trailer = (LastHttpContent) msg;
			if (!handleActually(trailer, ctx)) {
				ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
			}
		}
	}


	public final boolean handleActually(HttpObject currentObj, ChannelHandlerContext ctx) {
		HttpRequest request = ctx.attr(currentHttpRequestKey).get();
		String method = request.getMethod().toString().toUpperCase();
		String containerName = ctx.attr(containerNameKey).get();
		String objId = ctx.attr(objIdKey).get();
		Long size = ctx.attr(contentLengthKey).get();
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		response.headers().set(CONTENT_LENGTH, 0);
		ctx.attr(currentHttpResponseKey).set(response);
		if (containerName != null) {
			if (objId != null) {
				long offset;
				switch (method) {
					case WSRequestConfig.METHOD_POST:
					case WSRequestConfig.METHOD_PUT:
						offset = Long.parseLong(objId, MutableDataItem.ID_RADIX);
						break;
					default:
						offset = -1;
				}
				handleGenericDataReq(method, containerName, objId, offset, size, ctx);
			} else {
				handleGenericContainerReq(method, containerName, ctx);
			}
		} else {
			changeHttpResponseStatusInContext(ctx, BAD_REQUEST);
		}
		ctx.write(ctx.attr(currentHttpResponseKey).get());
		return true;
	}

	protected void handleGenericContainerReq(String method, String containerName, ChannelHandlerContext ctx) {
		switch (method) {
			case WSRequestConfig.METHOD_POST:
				break;
			case WSRequestConfig.METHOD_HEAD:
				handleContainerExists(containerName, ctx);
			case WSRequestConfig.METHOD_PUT:
				handleContainerCreate(containerName);
				break;
		}
	}

	private void handleContainerExists(String containerName, ChannelHandlerContext ctx) {
		if (sharedStorage.getContainer(containerName) == null) {
			changeHttpResponseStatusInContext(ctx, NOT_FOUND);
		}
	}

	private void handleContainerCreate(String containerName) {
		sharedStorage.createContainer(containerName);
	}

	protected void handleGenericDataReq(String method, String containerName, String objId,
	                                    Long offset, Long size, ChannelHandlerContext ctx) {
		switch (method) {
			case WSRequestConfig.METHOD_POST:
			case WSRequestConfig.METHOD_PUT:
				handleWrite(containerName, objId, offset, size, ctx);
		}
	}

	private void handleWrite(String containerName, String objId,
	                                 Long offset, Long size, ChannelHandlerContext ctx) {
		// TODO check usage of RANGE header
		try {
			sharedStorage.createObject(containerName, objId,
					offset, size);
			ioStats.markWrite(true, size);
		} catch (ContainerMockNotFoundException e) {
			changeHttpResponseStatusInContext(ctx, NOT_FOUND);
			ioStats.markWrite(false, size);
		} catch (StorageMockCapacityLimitReachedException e) {
			changeHttpResponseStatusInContext(ctx, INSUFFICIENT_STORAGE);
			ioStats.markWrite(false, size);
		}
	}

	private void changeHttpResponseStatusInContext(ChannelHandlerContext ctx, HttpResponseStatus status) {
		ctx.attr(currentHttpResponseKey).set(ctx.attr(currentHttpResponseKey).get().setStatus(status));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
