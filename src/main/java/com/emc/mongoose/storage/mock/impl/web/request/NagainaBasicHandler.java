package com.emc.mongoose.storage.mock.impl.web.request;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.data.MutableDataItem;
import com.emc.mongoose.core.api.data.content.ContentSource;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;
import com.emc.mongoose.storage.mock.api.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;

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

//    private HttpRequest request;
    private AttributeKey<String> containerNameKey = AttributeKey.valueOf("containerNameKey");
	private AttributeKey<String> objIdKey = AttributeKey.valueOf("objIdKey");
	private AttributeKey<Long> contentSizeKey = AttributeKey.valueOf("contentSizeKey");

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
//            HttpRequest request = this.request = (HttpRequest) msg;
	        HttpRequest request = (HttpRequest) msg;

            ctx.attr(contentSizeKey).set(0L);

            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
            String[] pathChunks = queryStringDecoder.path().split("/");
            if (pathChunks.length >= 2) {
	            ctx.attr(containerNameKey).set(pathChunks[1]);
	            ctx.attr(objIdKey).set(pathChunks[2]);
            }
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            Long currentContentSize = ctx.attr(contentSizeKey).get();
	        ctx.attr(contentSizeKey).set(currentContentSize + content.readableBytes());

        }

        if (msg instanceof LastHttpContent) {
            LastHttpContent trailer = (LastHttpContent) msg;
            if (!handleGenericDataReq(trailer, ctx)) {
                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private HttpResponse handleWrite(HttpResponse response, String containerName, String objId, Long contentSize) {
        // TODO check usage of RANGE header
        try {
            sharedStorage.createObject(containerName, objId,
                    Long.parseLong(objId, MutableDataItem.ID_RADIX), contentSize);
            ioStats.markWrite(true, contentSize);
        } catch (ContainerMockNotFoundException e) {
            response.setStatus(NOT_FOUND);
            ioStats.markWrite(false, contentSize);
        } catch (StorageMockCapacityLimitReachedException e) {
            response.setStatus(INSUFFICIENT_STORAGE);
            ioStats.markWrite(false, contentSize);
        }
	    return response;
    }

	protected boolean handleGenericDataReq(HttpObject currentObj, ChannelHandlerContext ctx) {

		String containerName = ctx.attr(containerNameKey).get();
		String objId = ctx.attr(objIdKey).get();
		Long contentSize = ctx.attr(contentSizeKey).get();

//		boolean keepAlive = HttpHeaders.isKeepAlive(request); TODO is it necessary?
		HttpResponse response = new DefaultHttpResponse(
				HTTP_1_1, currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST);
//		if (keepAlive) {
			response.headers().set(CONTENT_LENGTH, 0);
			response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
//		}
//		if (containerName  == null || containerName.equals("")) {
//			response.setStatus(BAD_REQUEST);
//		}
//		else {
//			switch (request.getMethod().toString().toUpperCase()) {
//				case WSRequestConfig.METHOD_POST:
//					handleWrite(response, containerName, objId, contentSize);
//					break;
//				case WSRequestConfig.METHOD_PUT:
//					handleWrite(response, containerName, objId, contentSize);
//					break;
//				default:
//					handleWrite(response, containerName, objId, contentSize);
//			}
//		}
		handleWrite(response, containerName, objId, contentSize);
		ctx.write(response);
//		return keepAlive;
		return true;
	}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
