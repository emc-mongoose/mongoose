package com.emc.mongoose.storage.mock.impl.web.request;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * Created by Ilia on 30.10.2015.
 */
public class NagainaBasicHandler extends SimpleChannelInboundHandler<Object> {

    private HttpRequest request;

    private String bucketName = "";
    private String itemId = "";
    private long contentLengthFromHeader = 0;
    private long contentLengthFromContentSum = 0;

    public String getBucketName() {
        return bucketName;
    }

    public String getItemId() {
        return itemId;
    }

    public long getContentLength() {
        return contentLengthFromContentSum;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest request = this.request = (HttpRequest) msg;

            if (request.headers().get(CONTENT_LENGTH) != null)
                contentLengthFromHeader = Long.parseLong(request.headers().get(CONTENT_LENGTH));
            contentLengthFromContentSum = 0;

            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
            String[] pathChunks = queryStringDecoder.path().split("/");
            if (pathChunks.length >= 2) {
                bucketName = pathChunks[1];
                itemId = pathChunks[2];
            }
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            contentLengthFromContentSum += content.readableBytes();

        }

        if (msg instanceof LastHttpContent) {
            LastHttpContent trailer = (LastHttpContent) msg;
            if (!writeResponse(trailer, ctx)) {
                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, currentObj.getDecoderResult().isSuccess()? OK : BAD_REQUEST,
                Unpooled.EMPTY_BUFFER);
        if (keepAlive) {
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        ctx.write(response);
        return keepAlive;
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
