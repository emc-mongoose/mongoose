package com.emc.mongoose.storage.mock.impl.http;

import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig;
import com.emc.mongoose.ui.log.NamingThreadFactory;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.net.ssl.SslContext;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.storage.mock.api.DataItemMock;
import com.emc.mongoose.storage.mock.impl.base.BasicDataItemMock;
import com.emc.mongoose.storage.mock.impl.base.StorageMockBase;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.NetConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;

import org.apache.commons.lang.SystemUtils;

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
extends StorageMockBase<DataItemMock>{

	public static final String SVC_NAME = Nagaina.class.getSimpleName().toLowerCase();

	private static final Logger LOG = LogManager.getLogger();

	private EventLoopGroup dispatchGroup;
	private EventLoopGroup workGroup;
	private Channel channel;
	private final List<ChannelInboundHandler> handlers;

	@SuppressWarnings("ConstantConditions")
	public Nagaina(
		final StorageConfig storageConfig, final LoadConfig loadConfig, final ItemConfig itemConfig,
		final StepConfig stepConfig, final ContentSource contentSource,
		final List<ChannelInboundHandler> handlers
	) {
		super(
			storageConfig.getMockConfig(), stepConfig.getMetricsConfig(), itemConfig, contentSource
		);
		final NetConfig netConfig = storageConfig.getNetConfig();
		final int port = netConfig.getNodeConfig().getPort();
		this.handlers = handlers;

		try {
			if(SystemUtils.IS_OS_LINUX) {
				dispatchGroup = new EpollEventLoopGroup(
					ThreadUtil.getHardwareConcurrencyLevel(),
					new NamingThreadFactory("dispatcher@port#" + port + "-", true)
				);
				workGroup = new EpollEventLoopGroup(
					ThreadUtil.getHardwareConcurrencyLevel(),
					new NamingThreadFactory("ioworker@port#" + port + "-", true)
				);
			} else {
				dispatchGroup = new NioEventLoopGroup(
					ThreadUtil.getHardwareConcurrencyLevel(),
					new NamingThreadFactory("dispatcher@port#" + port + "-", true)
				);
				workGroup = new NioEventLoopGroup(
					ThreadUtil.getHardwareConcurrencyLevel(),
					new NamingThreadFactory("ioworker@port#" + port + "-", true)
				);
			}
			final ServerBootstrap serverBootstrap = new ServerBootstrap();
			serverBootstrap.group(dispatchGroup, workGroup)
				.channel(
					SystemUtils.IS_OS_LINUX ?
					EpollServerSocketChannel.class : NioServerSocketChannel.class
				)
				.childHandler(
					new ChannelInitializer<SocketChannel>() {
						@Override
						protected final void initChannel(final SocketChannel socketChannel)
						throws Exception {
							final ChannelPipeline pipeline = socketChannel.pipeline();
							if(netConfig.getSsl()) {
								pipeline.addLast(
									new SslHandler(SslContext.INSTANCE.createSSLEngine())
								);
							}
							pipeline.addLast(new HttpServerCodec());
							for(final ChannelInboundHandler handler: handlers) {
								pipeline.addLast(handler);
							}
						}
					}
				);
			final ChannelFuture bind = serverBootstrap.bind(port);
			bind.sync();
			channel = bind.sync().channel();
		} catch(final Exception e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to start the service at port #{}", port
			);
			throw new IllegalStateException();
		}
		LOG.info(Markers.MSG, "Listening the port #{}", port);
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		try {
			channel.closeFuture().await(timeout, timeUnit); // one channel is enough
		} catch(final InterruptedException e) {
			LOG.info(Markers.MSG, "Interrupting the Nagaina");
		}

		return true;
	}

	@Override
	protected final void doClose()
	throws IOException {
		super.doClose();
		channel.close();
		dispatchGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS);
		workGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS);
		handlers.clear();
	}

	@Override
	protected DataItemMock newDataObject(
		final String id, final long offset, final long size
	) {
		return new BasicDataItemMock(id, offset, size, 0, contentSrc);
	}

}
