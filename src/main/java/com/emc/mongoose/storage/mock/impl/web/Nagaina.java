package com.emc.mongoose.storage.mock.impl.web;

import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.conf.RunTimeConfig;

import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;

import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.base.StorageMockBase;

import com.emc.mongoose.storage.mock.impl.web.request.*;
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

	private final EventLoopGroup dispatchGroup;
	private final EventLoopGroup workerGroup;
//	private final NagainaHandlerMapper protocolHandlerMapper;
	private final NagainaRequestHandlerBase s3RequestHandler;
	private final NagainaRequestHandlerBase swiftRequestHandler;
	private final NagainaRequestHandlerBase atmosRequestHandler;
	private final int portStart;
	private Channel channel;

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
		dispatchGroup = new NioEventLoopGroup(headCount, new DefaultThreadFactory("dispatcher"));
		LOG.info(Markers.MSG, "Starting with {} head(s)", headCount);
		workerGroup = new NioEventLoopGroup();
//		protocolHandlerMapper = new NagainaHandlerMapper<>(RunTimeConfig.getContext(), this);
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
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(dispatchGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						              @Override
						              protected void initChannel(SocketChannel socketChannel) throws Exception {
							              ChannelPipeline pipeline = socketChannel.pipeline();
							              pipeline.addLast(new HttpServerCodec());
//							              pipeline.addLast(protocolHandlerMapper);
							              pipeline.addLast(s3RequestHandler);
							              pipeline.addLast(swiftRequestHandler);
							              pipeline.addLast(atmosRequestHandler);
						              }
					              }
					);
			channel = bootstrap.bind(portStart).sync().channel();
			LOG.info(Markers.MSG, "Listening the port {}", portStart);
		} catch (InterruptedException e) {
			LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to start the head at port #{}", portStart
			);
		}
	}

	@Override
	protected final void await() {
		try {
			channel.closeFuture().sync();
		} catch (InterruptedException e) {
			LOG.info(Markers.MSG, "Interrupting Nagaina");
		}
	}

	@Override
	public void close() throws IOException {
		dispatchGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
		super.close();
	}
}
