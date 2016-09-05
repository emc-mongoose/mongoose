package com.emc.mongoose.storage.driver.http.base;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.ui.log.LogUtil;
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
 Created by kurila on 05.09.16.
 */
public class HttpClientHandlerBase<I extends Item, O extends IoTask<I>>
extends SimpleChannelInboundHandler<HttpObject> {

	private final static Logger LOG = LogManager.getLogger();
	
	private final HttpDriver<I, O> driver;
	
	public HttpClientHandlerBase(final HttpDriver<I, O> driver) {
		this.driver = driver;
	}

	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final HttpObject msg) {
	
		if(msg instanceof HttpResponse) {
			
		}
		
		if(msg instanceof HttpContent) {
			if(msg instanceof LastHttpContent) {
				ctx.close();
			}
		}
	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
		LogUtil.exception(LOG, Level.WARN, cause, "HTTP client handler failure");
		ctx.close();
	}
}
