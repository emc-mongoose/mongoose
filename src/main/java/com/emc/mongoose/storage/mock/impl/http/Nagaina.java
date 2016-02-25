package com.emc.mongoose.storage.mock.impl.http;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.item.data.ContentSourceBase;
import com.emc.mongoose.storage.mock.api.HttpDataItemMock;
import com.emc.mongoose.storage.mock.api.HttpStorageMock;
import com.emc.mongoose.storage.mock.impl.base.StorageMockBase;
import com.emc.mongoose.storage.mock.impl.http.request.NagainaRequestHandlerBase;
import com.emc.mongoose.storage.mock.impl.http.request.NagainaS3RequestHandler;
import com.emc.mongoose.storage.mock.impl.http.request.NagainaSwiftRequestHandler;
import com.emc.mongoose.storage.mock.impl.http.request.NagainaAtmosRequestHandler;
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
//
public class Nagaina<T extends HttpDataItemMock>
extends StorageMockBase<T>
implements HttpStorageMock<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final EventLoopGroup[] dispatchGroups;
	private final EventLoopGroup[] workerGroups;
	private final Channel[] channels;
	private final NagainaRequestHandlerBase s3RequestHandler;
	private final NagainaRequestHandlerBase swiftRequestHandler;
	private final NagainaRequestHandlerBase atmosRequestHandler;
	private final int portStart;
	//
	public Nagaina(final AppConfig appConfig) {
		this(
			appConfig.getStorageHttpMockHeadCount(),
			appConfig.getStorageHttpApi_Port(),
			appConfig.getStorageHttpMockCapacity(),
			appConfig.getStorageHttpMockContainerCapacity(),
			appConfig.getStorageHttpMockContainerCountLimit(),
			appConfig.getItemSrcBatchSize(),
			appConfig.getItemSrcFile(),
			appConfig.getLoadMetricsPeriod(),
			appConfig.getNetworkServeJmx(),
			0, 0
		);
	}
	//
	@SuppressWarnings("unchecked")
	public Nagaina(
		final int headCount, final int portStart,
		final int storageCapacity, final int containerCapacity, final int containerCountLimit,
		final int batchSize, final String dataSrcPath, final int metricsPeriodSec,
		final boolean jmxServeFlag, final int minConnLifeMilliSec, final int maxConnLifeMilliSec //todo use connections vars
	) {
		super(
			(Class<T>) BasicHttpDataMock.class, ContentSourceBase.getDefault(),
			storageCapacity, containerCapacity, containerCountLimit, batchSize, dataSrcPath, metricsPeriodSec,
			jmxServeFlag
		);
		this.portStart = portStart;
		dispatchGroups = new NioEventLoopGroup[headCount];
		workerGroups = new NioEventLoopGroup[headCount];
		channels = new Channel[headCount];
		LOG.info(Markers.MSG, "Starting with {} head(s)", headCount);
		final AppConfig ctxAppConfig = BasicConfig.THREAD_CONTEXT.get();
		s3RequestHandler = new NagainaS3RequestHandler<>(ctxAppConfig, this);
		swiftRequestHandler = new NagainaSwiftRequestHandler<>(ctxAppConfig, this);
		atmosRequestHandler = new NagainaAtmosRequestHandler<>(ctxAppConfig, this);
	}
	@SuppressWarnings("unchecked")
	@Override
	protected T newDataObject(String id, long offset, long size) {
		return (T) new BasicHttpDataMock(id, offset, size, 0, contentSrc);
	}
	@Override
	protected final void startListening() {
		for(int i = 0; i < dispatchGroups.length; i++) {
			try {
				// the first arg is a number of threads (0 as default)
				dispatchGroups[i] = new NioEventLoopGroup(0, new DefaultThreadFactory("dispatcher-" + i));
				workerGroups[i] = new NioEventLoopGroup();
				ServerBootstrap bootstrap = new ServerBootstrap();
				bootstrap.group(dispatchGroups[i], workerGroups[i])
					.channel(NioServerSocketChannel.class)
//					.handler(new LoggingHandler(LogLevel.WARN))
					.childHandler(
						new ChannelInitializer<SocketChannel>() {
							@Override
							protected void initChannel(final SocketChannel socketChannel)
							throws Exception {
								ChannelPipeline pipeline = socketChannel.pipeline();
								pipeline.addLast(new HttpServerCodec());
								pipeline.addLast(swiftRequestHandler);
								pipeline.addLast(atmosRequestHandler);
								pipeline.addLast(s3RequestHandler);
							}
						}
					);
				// todo check if there should be used just using of THE ONLY ONE bootstrap and several channels
				channels[i] = bootstrap.bind(portStart + i).sync().channel();
			} catch(InterruptedException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to start the head at port #{}", portStart + i
				);
			}
		}
		if(dispatchGroups.length > 1) {
			LOG.info(Markers.MSG, "Listening the ports {} .. {}",
				portStart, portStart + dispatchGroups.length - 1);
		} else {
			LOG.info(Markers.MSG, "Listening the port {}", portStart);
		}
	}
	@Override
	protected final void await() {
		for(Channel channel : channels) {
			try {
				channel.closeFuture().await(); // todo check if it should execute in different threads or some similar way
			} catch(InterruptedException e) {
				LOG.info(Markers.MSG, "Interrupting the Nagaina");
			}
		}
	}
	@Override
	public void close() throws IOException {
		for(int i = 0; i < dispatchGroups.length; i++) {
			closeEventLoopGroup(dispatchGroups[i]);
			closeEventLoopGroup(workerGroups[i]);
		}
		super.close();
	}
	private void closeEventLoopGroup(EventLoopGroup group) {
		if(group != null) {
			try {
				group.shutdownGracefully();
				LOG.debug(
					Markers.MSG, "EventLoopGroup \"{}\" shutted down successfully",
					group
				);
			} catch(Exception e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Closing EventLoopGroup \"{}\" failure",
					group
				);
			}
		}
	}
}

