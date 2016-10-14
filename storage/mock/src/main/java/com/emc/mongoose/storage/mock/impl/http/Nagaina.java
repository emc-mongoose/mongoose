package com.emc.mongoose.storage.mock.impl.http;

import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.net.ssl.SslContext;
import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.impl.base.BasicMutableDataItemMock;
import com.emc.mongoose.storage.mock.impl.base.StorageMockBase;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created on 11.07.16.
 */
public final class Nagaina
extends StorageMockBase<MutableDataItemMock>{

	public static final String SVC_NAME = Nagaina.class.getSimpleName().toLowerCase();

	private static final Logger LOG = LogManager.getLogger();

	private final int port;
	private final EventLoopGroup[] dispatchGroups;
	private final EventLoopGroup[] workGroups;
	private final Channel[] channels;
	private final List<ChannelInboundHandler> handlers;

	@SuppressWarnings("ConstantConditions")
	public Nagaina(
		final StorageConfig storageConfig, final LoadConfig loadConfig, final ItemConfig itemConfig,
		final ContentSource contentSource, final List<ChannelInboundHandler> handlers
	) {
		super(
			storageConfig.getMockConfig(), loadConfig.getMetricsConfig(), itemConfig, contentSource
		);
		port = storageConfig.getPort();
		final int headCount = storageConfig.getMockConfig().getHeadCount();
		dispatchGroups = new EventLoopGroup[headCount];
		workGroups = new EventLoopGroup[headCount];
		channels = new Channel[headCount];
		LOG.info(Markers.MSG, "Starting with {} head(s)", headCount);
		this.handlers = handlers;
	}

	@Override
	protected void doStart()
	throws IllegalStateException {
		super.doStart();
		final int portsNumber = dispatchGroups.length;
		for(int i = 0; i < portsNumber; i++) {
			try {
				dispatchGroups[i] = new NioEventLoopGroup(
					ThreadUtil.getHardwareConcurrencyLevel(),
					new NamingThreadFactory("dispatcher@port#" + (port + i) + "-", true)
				);
				workGroups[i] = new NioEventLoopGroup(
					ThreadUtil.getHardwareConcurrencyLevel(),
					new NamingThreadFactory("ioworker@port#" + (port + i) + "-", true)
				);
				final ServerBootstrap serverBootstrap = new ServerBootstrap();
				final int currentIndex = i;
				serverBootstrap.group(dispatchGroups[i], workGroups[i])
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(final SocketChannel socketChannel)
						throws Exception {
							final ChannelPipeline pipeline = socketChannel.pipeline();
							if(currentIndex % 2 == 1) {
								pipeline.addLast(
									new SslHandler(SslContext.INSTANCE.createSSLEngine())
								);
							}
							pipeline.addLast(new HttpServerCodec());
							for (final ChannelInboundHandler handler: handlers) {
								pipeline.addLast(handler);
							}
						}
					});
				final ChannelFuture bind = serverBootstrap.bind(port + i);
				bind.sync();
				channels[i] = bind.sync().channel();
			} catch(final Exception e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to start the head at port #{}", port + i
				);
				throw new IllegalStateException();
			}
		}
		if(portsNumber > 1) {
			LOG.info(Markers.MSG, "Listening the ports {} .. {}",
				port, port + portsNumber - 1);
		} else {
			LOG.info(Markers.MSG, "Listening the port {}", port);
		}
	}
	
	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		try {
//			for(final Channel channel : channels) {
//				channel.closeFuture().await(timeout, timeUnit);
//			}
			channels[0].closeFuture().await(timeout, timeUnit); // one channel is enough
		} catch(final InterruptedException e) {
			LOG.info(Markers.MSG, "Interrupting the Nagaina");
		}

		return true;
	}

	@Override
	protected final void doClose()
	throws IOException {
		super.doClose();
		for(int i = 0; i < channels.length; i ++) {
			channels[i].close();
			channels[i] = null;
		}
		for(int i = 0; i < dispatchGroups.length; i ++) {
			dispatchGroups[i].shutdownGracefully(1, 1, TimeUnit.SECONDS);
			dispatchGroups[i] = null;
		}
		for(int i = 0; i < workGroups.length; i ++) {
			workGroups[i].shutdownGracefully(1, 1, TimeUnit.SECONDS);
			workGroups[i] = null;
		}
		handlers.clear();
	}

	@Override
	protected MutableDataItemMock newDataObject(
		final String id, final long offset, final long size
	) {
		return new BasicMutableDataItemMock(id, offset, size, 0, contentSrc);
	}

}
