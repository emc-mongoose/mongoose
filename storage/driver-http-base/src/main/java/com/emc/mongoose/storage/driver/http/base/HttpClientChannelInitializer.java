package com.emc.mongoose.storage.driver.http.base;

import com.emc.mongoose.common.net.ssl.SslContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import javax.net.ssl.SSLEngine;

/**
 Created by kurila on 01.08.16.
 */
public class HttpClientChannelInitializer
extends ChannelInitializer<SocketChannel> {
	
	private final boolean sslFlag;
	private final SimpleChannelInboundHandler<HttpObject> httpClientHandler;
	
	public HttpClientChannelInitializer(
		final boolean sslFlag, final SimpleChannelInboundHandler<HttpObject> httpClientHandler
	) {
		this.sslFlag = sslFlag;
		this.httpClientHandler = httpClientHandler;
	}
	
	@Override
	public final void initChannel(final SocketChannel sc)
	throws Exception {
		
		final ChannelPipeline pipeline = sc.pipeline();
		
		if(sslFlag) {
			final SSLEngine sslEngine = SslContext.INSTANCE.createSSLEngine();
			sslEngine.setUseClientMode(true);
			pipeline.addLast(new SslHandler(sslEngine));
		}
		
		pipeline.addLast(new HttpClientCodec());
		pipeline.addLast(new ChunkedWriteHandler());
		pipeline.addLast(httpClientHandler);
	}
}
