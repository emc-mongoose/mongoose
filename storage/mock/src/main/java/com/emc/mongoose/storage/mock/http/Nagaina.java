package com.emc.mongoose.storage.mock.http;

import com.emc.mongoose.common.config.CommonConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.storage.mock.StorageMock;
import com.emc.mongoose.storage.mock.http.request.AtmosRequestHandler;
import com.emc.mongoose.storage.mock.http.request.RequestHandlerBase;
import com.emc.mongoose.storage.mock.http.request.S3RequestHandler;
import com.emc.mongoose.storage.mock.http.request.SwiftRequestHandler;
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

import java.util.concurrent.TimeUnit;

/**
 Created on 11.07.16.
 */
public class Nagaina implements StorageMock {

	private final static Logger LOG = LogManager.getLogger();

	private final int port = 0;
	private final EventLoopGroup[] dispatchGroups = null;
	private final EventLoopGroup[] workGroups = null;
	private final Channel[] channels = null;
	private final RequestHandlerBase s3RequestHandler, swiftRequestHandler, atmosRequestHandler;

	@SuppressWarnings("ConstantConditions")
	public Nagaina(final CommonConfig commonConfig) {
//		port = commonConfig.getStorageConfig().getPort();
//		final int headCount = mockConfig.getHeadCount();
//		dispatchGroups = new NioEventLoopGroup[headCount];
//		workGroups = new NioEventLoopGroup[headCount];
//		channels = new Channel[headCount];
//		LOG.info(Markers.MSG, "Starting with {} head(s)", headCount);
		s3RequestHandler = new S3RequestHandler();
		swiftRequestHandler = new SwiftRequestHandler();
		atmosRequestHandler = new AtmosRequestHandler();
	}

	@Override
	public void start()
	throws IllegalStateException {
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
		return false;
	}

	@Override
	public boolean await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException {
		return false;
	}

	@Override
	public void interrupt()
	throws IllegalStateException {
	}

	@Override
	public boolean isInterrupted() {
		return false;
	}
}
