package com.emc.mongoose.storage.mock.impl.http;

import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.impl.base.StorageMockBase;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.impl.http.request.AtmosRequestHandler;
import com.emc.mongoose.storage.mock.impl.http.request.RequestHandlerBase;
import com.emc.mongoose.storage.mock.impl.http.request.S3RequestHandler;
import com.emc.mongoose.storage.mock.impl.http.request.SwiftRequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 Created on 11.07.16.
 */
public class Nagaina extends StorageMockBase<MutableDataItemMock>{


	private final static Logger LOG = LogManager.getLogger();

	private final int port;
	private final EventLoopGroup[] dispatchGroups;
	private final EventLoopGroup[] workGroups;
	private final Channel[] channels;
	private final RequestHandlerBase s3RequestHandler, swiftRequestHandler, atmosRequestHandler;

	@SuppressWarnings("ConstantConditions")
	public Nagaina(final Config config) {
		super(config.getStorageConfig().getMockConfig(), config.getLoadConfig().getMetricsConfig(), config.getItemConfig());
		final Config.StorageConfig storageConfig = config.getStorageConfig();
		port = storageConfig.getPort();
		final int headCount = storageConfig.getMockConfig().getHeadCount();
		dispatchGroups = new NioEventLoopGroup[headCount];
		workGroups = new NioEventLoopGroup[headCount];
		channels = new Channel[headCount];
		LOG.info(Markers.MSG, "Starting with {} head(s)", headCount);
		s3RequestHandler = new S3RequestHandler();
		swiftRequestHandler = new SwiftRequestHandler();
		atmosRequestHandler = new AtmosRequestHandler();
	}

	public void doStart()
	throws IllegalStateException {
		super.start();
		final int portsNumber = dispatchGroups.length;
		for (int i = 0; i < portsNumber; i++) {
			try {
				dispatchGroups[i] =
					new NioEventLoopGroup(0, new DefaultThreadFactory("dispatcher-" + i));
				workGroups[i] = new NioEventLoopGroup();
				final ServerBootstrap serverBootstrap = new ServerBootstrap();
				serverBootstrap.group(dispatchGroups[i], workGroups[i])
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(final SocketChannel socketChannel)
						throws Exception {
							final ChannelPipeline pipeline = socketChannel.pipeline();
							pipeline.addLast(new HttpServerCodec());
							pipeline.addLast(swiftRequestHandler);
							pipeline.addLast(atmosRequestHandler);
							pipeline.addLast(s3RequestHandler);
						}
					});
				channels[i] = serverBootstrap.bind(port + i).sync().channel();
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
	public boolean isStarted() {
		return false;
	}

	@Override
	public void shutdown()
	throws IllegalStateException {
		for(int i = 0; i < dispatchGroups.length; i++) {
			shutdownEventLoopGroup(dispatchGroups[i]);
			shutdownEventLoopGroup(workGroups[i]);
		}
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	private void shutdownEventLoopGroup(final EventLoopGroup group) {
		if(group != null) {
			try {
				group.shutdownGracefully();
				LOG.debug(
					Markers.MSG, "EventLoopGroup \"{}\" shutdown successfully",
					group
				);
			} catch(final Exception e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Closing EventLoopGroup \"{}\" failure",
					group
				);
			}
		}
	}

	@Override
	public boolean await()
	throws InterruptedException {
		return await(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	private static final long INTERVAL_VALUE = 1L;
	private static final TimeUnit INTERVAL_UNIT = TimeUnit.SECONDS;

	@Override
	public boolean await(long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		timeout = timeUnit.toNanos(timeout);
		final long intervalValue;
		if (timeout < INTERVAL_UNIT.toNanos(INTERVAL_VALUE)) {
			intervalValue = timeout;
		} else {
			intervalValue = INTERVAL_UNIT.toNanos(INTERVAL_VALUE);
		}
		while(timeout > 0) {
			final List<Boolean> statuses = Stream.of(channels).map(channel -> {
				try {
					return channel.closeFuture().await(intervalValue, TimeUnit.NANOSECONDS);
				} catch(final InterruptedException ignored) {
				}
				return null;
			}).collect(Collectors.toList());
			for (final Boolean status: statuses) {
				if (status == null) {
					throw new InterruptedException();
				} else if (status.equals(Boolean.FALSE)) {
					timeout -= intervalValue;
					break;
				}
			}
		}
		final List<Boolean> statuses = Stream.of(channels).map(channel -> {
			try {
				return channel.closeFuture().await(intervalValue, TimeUnit.NANOSECONDS);
			} catch(final InterruptedException ignored) {
			}
			return null;
		}).collect(Collectors.toList());
		for (final Boolean status: statuses) {
			if (status == null) {
				throw new InterruptedException();
			} else if (status.equals(Boolean.FALSE)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void interrupt()
	throws IllegalStateException {
	}

	@Override
	public boolean isInterrupted() {
		return false;
	}

	@Override
	protected MutableDataItemMock newDataObject(final String id, final long offset, final long size) {
		return null;
	}

	@Override
	protected void startListening() {
	}
}
