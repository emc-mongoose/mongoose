package com.emc.mongoose.storage.mock.impl.http;
import com.emc.mongoose.common.conf.AppConfig;
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
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
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
	@SuppressWarnings("unchecked")
	public Nagaina(final AppConfig appConfig) {
		super(
				((Class<T>) BasicHttpDataMock.class), ContentSourceBase.getDefaultInstance(),
				appConfig.getStorageMockCapacity(), appConfig.getStorageMockContainerCapacity(),
				appConfig.getStorageMockContainerCountLimit(), appConfig.getItemSrcBatchSize(),
				appConfig.getItemSrcFile(), appConfig.getLoadMetricsPeriod(),
				appConfig.getNetworkServeJmx());
		portStart = appConfig.getStoragePort();
		final int headCount = appConfig.getStorageMockHeadCount();
		dispatchGroups = new EventLoopGroup[headCount];
		workerGroups = new EventLoopGroup[headCount];
		channels = new Channel[headCount];
		LOG.info(Markers.MSG, "Starting with {} head(s)", headCount);
		s3RequestHandler = new NagainaS3RequestHandler<>(appConfig, this);
		swiftRequestHandler = new NagainaSwiftRequestHandler<>(appConfig, this);
		atmosRequestHandler = new NagainaAtmosRequestHandler<>(appConfig, this);
	}
	//
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
				dispatchGroups[i] = new EpollEventLoopGroup(0, new DefaultThreadFactory("dispatcher-" + i));
				workerGroups[i] = new EpollEventLoopGroup();
				ServerBootstrap bootstrap = new ServerBootstrap();
				bootstrap.group(dispatchGroups[i], workerGroups[i])
					.channel(EpollServerSocketChannel.class)
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
			} catch(final Exception e) {
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
		for(final Channel channel : channels) {
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

