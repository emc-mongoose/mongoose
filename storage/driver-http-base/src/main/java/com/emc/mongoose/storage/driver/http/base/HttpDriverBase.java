package com.emc.mongoose.storage.driver.http.base;

import com.emc.mongoose.common.concurrent.FutureTaskBase;
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.api.load.Balancer;
import com.emc.mongoose.model.impl.io.AsyncCurrentDateInput;
import com.emc.mongoose.model.impl.item.BasicDataItem;
import com.emc.mongoose.model.impl.item.BasicMutableDataItem;
import com.emc.mongoose.model.impl.load.BasicBalancer;
import com.emc.mongoose.model.util.LoadType;
import com.emc.mongoose.storage.driver.base.DriverBase;
import static com.emc.mongoose.common.concurrent.BlockingQueueTaskSequencer.INSTANCE;
import static com.emc.mongoose.model.api.item.Item.SLASH;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.emc.mongoose.storage.driver.http.base.data.DataItemFileRegion;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 29.07.16.
 */
public abstract class HttpDriverBase<I extends Item, O extends IoTask<I>>
extends DriverBase<I, O>
implements HttpDriver<I, O> {
	
	private final static Logger LOG = LogManager.getLogger();
	
	private final String storageNodeAddrs[];
	private final int storageNodePort;
	protected final HttpHeaders sharedHeaders = new DefaultHttpHeaders();
	protected final SecretKeySpec secretKey;
	
	private final Balancer<String> storageNodeBalancer;
	private final EventLoopGroup workerGroup;
	protected final Bootstrap bootstrap;
	
	protected HttpDriverBase(
		final String runId, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final String srcContainer, final SocketConfig socketConfig
	) throws IllegalStateException {
		super(runId, storageConfig.getAuthConfig(), loadConfig, srcContainer);
		try {
			if(secret == null) {
				secretKey = null;
			} else {
				secretKey = new SecretKeySpec(secret.getBytes(UTF_8.name()), SIGN_METHOD);
			}
		} catch(final UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		storageNodeAddrs = storageConfig.getNodeConfig().getAddrs().toArray(new String[]{});
		storageNodePort = storageConfig.getPort();
		storageNodeBalancer = new BasicBalancer<>(storageNodeAddrs);
		
		final HttpClientHandlerBase<I, O> apiSpecificHandler = getApiSpecificHandler();
		
		workerGroup = new NioEventLoopGroup(0, new NamingThreadFactory("test"));
		bootstrap = new Bootstrap();
		bootstrap.group(workerGroup);
		bootstrap.channel(NioSocketChannel.class);
		//bootstrap.option(ChannelOption.ALLOCATOR, ByteBufAllocator)
		//bootstrap.option(ChannelOption.ALLOW_HALF_CLOSURE)
		//bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, )
		//bootstrap.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR)
		//bootstrap.option(ChannelOption.AUTO_READ)
		//bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS)
		//bootstrap.option(ChannelOption.SO_RCVBUF);
		//bootstrap.option(ChannelOption.SO_SNDBUF);
		//bootstrap.option(ChannelOption.SO_BACKLOG, socketConfig.getBindBackLogSize());
		bootstrap.option(ChannelOption.SO_KEEPALIVE, socketConfig.getKeepAlive());
		bootstrap.option(ChannelOption.SO_LINGER, socketConfig.getLinger());
		bootstrap.option(ChannelOption.SO_REUSEADDR, socketConfig.getReuseAddr());
		//bootstrap.option(ChannelOption.SO_TIMEOUT, socketConfig.getTimeoutMillisec());
		bootstrap.option(ChannelOption.TCP_NODELAY, socketConfig.getTcpNoDelay());
		bootstrap.handler(
			new HttpClientChannelInitializer(storageConfig.getSsl(), apiSpecificHandler)
		);
	}
	
	protected abstract HttpClientHandlerBase<I, O> getApiSpecificHandler();
	
	protected HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
	throws URISyntaxException {
		final I item = ioTask.getItem();
		final LoadType ioType = ioTask.getLoadType();
		final HttpMethod httpMethod = getHttpMethod(ioType);
		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		final String dstUriPath = getDstUriPath(item, ioTask);
		final HttpRequest httpRequest = new DefaultHttpRequest(
			HTTP_1_1, httpMethod, dstUriPath, httpHeaders
		);
		httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		httpHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateInput.INSTANCE.get());
		switch(ioType) {
			case CREATE:
				if(srcContainer == null) {
					if(item instanceof DataItem) {
						try {
							httpHeaders.set(
								HttpHeaderNames.CONTENT_LENGTH, ((DataItem) item).size()
							);
						} catch(final IOException ignored) {
						}
					} else {
						httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
					}
				} else {
					applyCopyHeaders(httpHeaders, item);
					httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				}
				break;
			case READ:
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				break;
			case UPDATE:
				if(item instanceof MutableDataItem) {
					// TODO set ranges headers conditionally
				} else {
					throw new IllegalStateException();
				}
				break;
			case DELETE:
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				break;
		}
		applyMetaDataHeaders(httpHeaders);
		applyAuthHeaders(httpMethod, dstUriPath, httpHeaders);
		return httpRequest;
	}
	
	protected HttpMethod getHttpMethod(final LoadType loadType) {
		switch(loadType) {
			case READ:
				return HttpMethod.GET;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				return HttpMethod.PUT;
		}
	}
	
	protected String getDstUriPath(final I item, final O ioTask) {
		return SLASH + ioTask.getDstPath() + SLASH + item.getName();
	}
	
	protected String getSrcUriPath(final I object)
	throws URISyntaxException {
		if(object == null) {
			throw new IllegalArgumentException("No object");
		}
		final String objPath = object.getPath();
		if(objPath.endsWith(SLASH)) {
			return objPath + object.getName();
		} else {
			return objPath + SLASH + object.getName();
		}
	}
	
	protected abstract void applyCopyHeaders(final HttpHeaders httpHeaders, final I obj)
	throws URISyntaxException;
	
	protected void applySharedHeaders(final HttpHeaders httpHeaders) {
		// TODO apply dynamic headers also
	}
	
	protected void applyMetaDataHeaders(final HttpHeaders httpHeaders) {
	}
	
	protected abstract void applyAuthHeaders(
		final HttpMethod httpMethod, final String dstUriPath, final HttpHeaders httpHeaders
	);
	
	private final class HttpRequestFutureTask
	extends FutureTaskBase
	implements GenericFutureListener<Future<Void>> {
		
		private final O ioTask;
		
		public HttpRequestFutureTask(final O ioTask) {
			this.ioTask = ioTask;
		}
		
		@Override
		public final void run() {
			
			final String bestNode;
			if(storageNodeAddrs.length == 1) {
				bestNode = storageNodeAddrs[0];
			} else {
				try {
					bestNode = storageNodeBalancer.get();
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to get the best node");
					return;
				}
			}
			ioTask.setNodeAddr(bestNode);
			
			final LoadType ioType = ioTask.getLoadType();
			final I item = ioTask.getItem();
			final Channel channel;
			try {
				channel = bootstrap.connect(bestNode, storageNodePort).sync().channel();
			} catch(final InterruptedException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to get the connection to \"{}\"", bestNode
				);
				return;
			} catch(final RejectedExecutionException | IllegalStateException e) {
				LogUtil.exception(
					LOG, Level.DEBUG, e, "Failed to get the connection to \"{}\"", bestNode
				);
				return;
			}
			
			channel.attr(ATTR_KEY_IOTASK).set(ioTask);
			
			try {
				ioTask.setReqTimeStart(System.nanoTime() / 1000);
				channel.write(getHttpRequest(ioTask, bestNode));
				if(LoadType.CREATE.equals(ioType)) {
					if(item instanceof DataItem) {
						final DataItem dataItem = (DataItem) item;
						channel
							.write(new DataItemFileRegion<>(dataItem))
							.addListener(this);
						((DataIoTask) ioTask).setCountBytesDone(dataItem.size());
					}
				} else if(LoadType.UPDATE.equals(ioType)) {
					if(item instanceof MutableDataItem) {
						final MutableDataItem mdi = (MutableDataItem) item;
						final DataIoTask dataIoTask = (DataIoTask) ioTask;
						if(mdi.hasScheduledUpdates()) {
							long nextRangeOffset, nextRangeSize;
							BasicDataItem nextUpdatedRange;
							for(int i = 0; i < mdi.getCountRangesTotal(); i ++) {
								if(mdi.isCurrLayerRangeUpdating(i)) {
									nextRangeOffset = BasicMutableDataItem.getRangeOffset(i);
									nextRangeSize = mdi.getRangeSize(i);
									nextUpdatedRange = new BasicDataItem(
										(BasicDataItem) mdi, nextRangeOffset, nextRangeSize, true
									);
									channel
										.write(new DataItemFileRegion<>(nextUpdatedRange))
										.addListener(this);
									dataIoTask.setCountBytesDone(
										dataIoTask.getCountBytesDone() + nextRangeSize
									);
								}
							}
						}
					}
				}
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to write the data");
			} catch(final URISyntaxException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to build the request URI");
			}
			
			channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		}
		
		@Override
		public final void operationComplete(final Future<Void> future)
		throws Exception {
			ioTask.setReqTimeDone(System.nanoTime() / 1000);
		}
	}
	
	@Override
	public void put(final O task) {
		INSTANCE.submit(new HttpRequestFutureTask(task));
	}
	
	@Override
	public int put(final List<O> tasks, final int from, final int to) {
		for(int i = from; i < to; i ++) {
			INSTANCE.submit(new HttpRequestFutureTask(tasks.get(i)));
		}
		return to - from;
	}
	
	@Override
	public int put(final List<O> tasks) {
		return put(tasks, 0, tasks.size());
	}
	
	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return false;
	}
	
	@Override
	public boolean isFullThrottleEntered() {
		return false;
	}
	
	@Override
	public boolean isFullThrottleExited() {
		return false;
	}
	
	@Override
	protected void doStart()
	throws IllegalStateException {
	}
	
	@Override
	protected void doShutdown()
	throws IllegalStateException {
	}
	
	@Override
	protected void doInterrupt()
	throws IllegalStateException {
		final Future f = workerGroup.shutdownGracefully(0, 1, TimeUnit.NANOSECONDS);
		try {
			f.await(1, TimeUnit.SECONDS);
		} catch(final InterruptedException e) {
			LOG.warn(Markers.ERR, "Failed to interrupt the HTTP storage driver gracefully");
		}
	}
	
	@Override
	protected void doClose()
	throws IOException {
		super.doClose();
		for(int i = 0; i < storageNodeAddrs.length; i ++) {
			storageNodeAddrs[i] = null;
		}
		sharedHeaders.clear();
		try {
			secretKey.destroy();
		} catch(final DestroyFailedException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to clear the secret key");
		}
		storageNodeBalancer.close();
		workerGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS);
	}
}
