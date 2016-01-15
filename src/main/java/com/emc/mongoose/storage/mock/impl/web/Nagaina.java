package com.emc.mongoose.storage.mock.impl.web;

import com.emc.mongoose.common.conf.RunTimeConfig;

import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;

import com.emc.mongoose.core.impl.item.data.ContentSourceBase;
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.base.StorageMockBase;

import com.emc.mongoose.storage.mock.impl.web.request.NagainaRequestHandlerBase;
import com.emc.mongoose.storage.mock.impl.web.request.NagainaS3RequestHandler;
import com.emc.mongoose.storage.mock.impl.web.request.NagainaSwiftRequestHandler;
import com.emc.mongoose.storage.mock.impl.web.request.NagainaAtmosRequestHandler;
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

import java.io.IOException;

public class Nagaina<T extends WSObjectMock>
		extends StorageMockBase<T>
		implements WSMock<T> {

	private final static Logger LOG = LogManager.getLogger();

	private final EventLoopGroup[] dispatchGroups;
	private final EventLoopGroup[] workerGroups;
	private final Channel[] channels;
	private final NagainaRequestHandlerBase s3RequestHandler;
	private final NagainaRequestHandlerBase swiftRequestHandler;
	private final NagainaRequestHandlerBase atmosRequestHandler;
	private final int portStart;

	public Nagaina(RunTimeConfig rtConfig) {
		this(
				rtConfig.getStorageMockHeadCount(),
				rtConfig.getApiTypePort(rtConfig.getApiName()),
				rtConfig.getStorageMockCapacity(),
				rtConfig.getStorageMockContainerCapacity(),
				rtConfig.getStorageMockContainerCountLimit(),
				rtConfig.getBatchSize(),
				rtConfig.getItemSrcFile(),
				rtConfig.getLoadMetricsPeriodSec(),
				rtConfig.getFlagServeJMX(),
				rtConfig.getStorageMockMinConnLifeMilliSec(),
				rtConfig.getStorageMockMaxConnLifeMilliSec()
		);
	}

	@SuppressWarnings("unchecked")
	public Nagaina(
			final int headCount, final int portStart,
			final int storageCapacity, final int containerCapacity, final int containerCountLimit,
			final int batchSize, final String dataSrcPath, final int metricsPeriodSec,
			final boolean jmxServeFlag, final int minConnLifeMilliSec, final int maxConnLifeMilliSec //todo use connections vars
	) {
		super((Class<T>) BasicWSObjectMock.class, ContentSourceBase.getDefault(),
				storageCapacity, containerCapacity, containerCountLimit, batchSize, dataSrcPath, metricsPeriodSec,
				jmxServeFlag);
		this.portStart = portStart;
		dispatchGroups = new NioEventLoopGroup[headCount];
		workerGroups = new NioEventLoopGroup[headCount];
		channels = new Channel[headCount];
		LOG.info(Markers.MSG, "Starting with {} head(s)", headCount);
		s3RequestHandler = new NagainaS3RequestHandler<>(RunTimeConfig.getContext(), this);
		swiftRequestHandler = new NagainaSwiftRequestHandler<>(RunTimeConfig.getContext(), this);
		atmosRequestHandler = new NagainaAtmosRequestHandler<>(RunTimeConfig.getContext(), this);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected T newDataObject(String id, long offset, long size) {
		return (T) new BasicWSObjectMock(id, offset, size, 0, contentSrc);
	}

	@Override
	protected final void startListening() {
		for (int i = 0; i < dispatchGroups.length; i++) {
			try {
				// the first arg is a number of threads (0 as default)
				dispatchGroups[i] = new NioEventLoopGroup(0, new DefaultThreadFactory("dispatcher-" + i));
				workerGroups[i] = new NioEventLoopGroup();
				ServerBootstrap bootstrap = new ServerBootstrap();
				bootstrap.group(dispatchGroups[i], workerGroups[i])
						.channel(NioServerSocketChannel.class)
//					.handler(new LoggingHandler(LogLevel.WARN))
						.childHandler(new ChannelInitializer<SocketChannel>() {
							              @Override
							              protected void initChannel(SocketChannel socketChannel) throws Exception {
								              ChannelPipeline pipeline = socketChannel.pipeline();
								              pipeline.addLast(new HttpServerCodec());
								              pipeline.addLast(swiftRequestHandler);
								              pipeline.addLast(s3RequestHandler);
								              pipeline.addLast(atmosRequestHandler);
							              }
						              }
						);
				// todo check if there should be used just using of THE ONLY ONE bootstrap and several channels
				channels[i] = bootstrap.bind(portStart + i).sync().channel();
			} catch (InterruptedException e) {
				LogUtil.exception(
						LOG, Level.ERROR, e, "Failed to start the head at port #{}", portStart + i
				);
			}
		}
		if (dispatchGroups.length > 1) {
			LOG.info(Markers.MSG, "Listening the ports {} .. {}",
					portStart, portStart + dispatchGroups.length - 1);
		} else {
			LOG.info(Markers.MSG, "Listening the port {}", portStart);
		}
	}

	@Override
	protected final void await() {
		for (Channel channel : channels) {
			try {
				channel.closeFuture().await(); // todo check if it should execute in different threads or some similar way
			} catch (InterruptedException e) {
				LOG.info(Markers.MSG, "Interrupting the Nagaina");
			}
		}
	}

	@Override
	public void close() throws IOException {
		for (int i = 0; i < dispatchGroups.length; i++) {
			closeEventLoopGroup(dispatchGroups[i]);
			closeEventLoopGroup(workerGroups[i]);
		}
		super.close();
	}

	private void closeEventLoopGroup(EventLoopGroup group) {
		if (group != null) {
			try {
				group.shutdownGracefully();
				LOG.debug(
						Markers.MSG, "EventLoopGroup \"{}\" shutted down successfully",
						group
				);
			} catch (Exception e) {
				LogUtil.exception(
						LOG, Level.WARN, e, "Closing EventLoopGroup \"{}\" failure",
						group
				);
			}
		}
	}

}

