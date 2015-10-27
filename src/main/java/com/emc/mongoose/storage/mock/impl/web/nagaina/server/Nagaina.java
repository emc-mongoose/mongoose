package com.emc.mongoose.storage.mock.impl.web.nagaina.server;

import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.conf.RunTimeConfig;

import com.emc.mongoose.core.impl.data.content.ContentSourceBase;

import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.base.StorageMockBase;
import com.emc.mongoose.storage.mock.impl.web.BasicWSObjectMock;
import com.emc.mongoose.storage.mock.impl.web.net.BasicSocketEventDispatcher;

import com.emc.mongoose.storage.mock.impl.web.nagaina.init.NagainaServerInitializer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by ilya on 21.10.15.
 */
public class Nagaina<T extends WSObjectMock>
		extends StorageMockBase<T>
		implements WSMock<T> {

	private static int port = 9020;
	private final static Logger LOG = LogManager.getLogger();

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
		this.port = portStart;

	}


	@Override
	protected T newDataObject(String id, long offset, long size) {
		return null;
	}

	@Override
	protected void startListening() {
		EventLoopGroup dispatchGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(dispatchGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new NagainaServerInitializer());
			Channel channel = bootstrap.bind(port).sync().channel();
			channel.closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			dispatchGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	@Override
	protected void await() {

	}

}
