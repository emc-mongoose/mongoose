package com.emc.mongoose.storage.driver.http.s3;

import com.emc.mongoose.ui.log.LogUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 Created by kurila on 01.08.16.
 */
@ChannelHandler.Sharable
public final class HttpS3Handler
extends SimpleChannelInboundHandler<HttpObject> {
	
	private final static Logger LOG = LogManager.getLogger();
	
	@Override
	protected final void channelRead0(final ChannelHandlerContext ctx, final HttpObject msg) {
		
		if(msg instanceof HttpResponse) {
			
		}
		
		if(msg instanceof HttpContent) {
			if(msg instanceof LastHttpContent) {
				ctx.close();
			}
		}
	}
		
	@Override
	public final void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
		LogUtil.exception(LOG, Level.WARN, cause, "S3 client handler failure");
		ctx.close();
	}
}
