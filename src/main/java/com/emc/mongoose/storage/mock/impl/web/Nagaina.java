package com.emc.mongoose.storage.mock.impl.web;

import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.conf.RunTimeConfig;

import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;

import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.base.StorageMockBase;
import com.emc.mongoose.storage.mock.impl.web.request.NagainaBasicHandler;
import com.emc.mongoose.storage.mock.impl.web.net.BasicSocketEventDispatcher;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by ilya on 21.10.15.
 */
public class Nagaina<T extends WSObjectMock>
		extends StorageMockBase<T>
		implements WSMock<T> {

	private static int portStart = 9020;
	private final static Logger LOG = LogManager.getLogger();
	private Channel channel;
	private EventLoopGroup dispatchGroup = new NioEventLoopGroup(1);
	private EventLoopGroup workerGroup = new NioEventLoopGroup();

	private final BasicSocketEventDispatcher socketEventDispatcher = null;

	public Nagaina(RunTimeConfig rtConfig) {
		this(rtConfig, rtConfig.getStorageMockWorkersPerSocket());
	}

	private Nagaina(RunTimeConfig rtConfig, int ioThreadCount) {
		this(
				rtConfig.getStorageMockHeadCount(),
				ioThreadCount > 0 ? ioThreadCount : ThreadUtil.getWorkerCount(),
				rtConfig.getApiTypePort(rtConfig.getApiName()),
				rtConfig.getStorageMockCapacity(),
				rtConfig.getStorageMockContainerCapacity(),
				rtConfig.getStorageMockContainerCountLimit(),
				rtConfig.getBatchSize(),
				rtConfig.getItemSrcFPath(),
				rtConfig.getLoadMetricsPeriodSec(),
				rtConfig.getFlagServeJMX(),
				rtConfig.getStorageMockMinConnLifeMilliSec(),
				rtConfig.getStorageMockMaxConnLifeMilliSec()
		);
	}

	public Nagaina(
			final int headCount, final int ioThreadCount, final int portStart,
			final int storageCapacity, final int containerCapacity, final int containerCountLimit,
			final int batchSize, final String dataSrcPath, final int metricsPeriodSec,
			final boolean jmxServeFlag, final int minConnLifeMilliSec, final int maxConnLifeMilliSec
	) {
		super((Class<T>) BasicWSObjectMock.class, ContentSourceBase.getDefault(),
				storageCapacity, containerCapacity, containerCountLimit, batchSize, dataSrcPath, metricsPeriodSec,
				jmxServeFlag);
		this.portStart = portStart;

	}


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
						              protected void initChannel(SocketChannel ch) throws Exception {
							              ChannelPipeline pipeline = channel.pipeline();
							              pipeline.addLast(new HttpServerCodec());
							              pipeline.addLast(new NagainaBasicHandler());
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
