package com.emc.mongoose.storage.driver.http.base;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.io.task.MutableDataIoTask;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.impl.io.AsyncPatternDefinedInput;
import com.emc.mongoose.model.util.LoadType;
import static com.emc.mongoose.model.api.io.PatternDefinedInput.PATTERN_CHAR;
import static com.emc.mongoose.model.api.item.Item.SLASH;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.emc.mongoose.storage.driver.http.base.request.CrudHttpRequestFactory;
import com.emc.mongoose.storage.driver.http.base.request.HttpRequestFactory;
import static com.emc.mongoose.ui.config.Config.StorageConfig.HttpConfig;
import com.emc.mongoose.storage.driver.net.base.NetStorageDriverBase;
import com.emc.mongoose.storage.driver.net.base.data.DataItemFileRegion;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

/**
 Created by kurila on 29.07.16.
 Netty-based concurrent HTTP client executing the submitted I/O tasks.
 */
public abstract class HttpStorageDriverBase<I extends Item, O extends IoTask<I>>
extends NetStorageDriverBase<I, O>
implements HttpStorageDriver<I, O> {
	
	private static final Logger LOG = LogManager.getLogger();
	private static final Map<String, Input<String>> HEADER_NAME_INPUTS = new ConcurrentHashMap<>();
	private static final Map<String, Input<String>> HEADER_VALUE_INPUTS = new ConcurrentHashMap<>();
	private static final Function<String, Input<String>> PATTERN_INPUT_FUNC = headerName -> {
		try {
			return new AsyncPatternDefinedInput(headerName);
		} catch(final UserShootHisFootException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to create the pattern defined input");
			return null;
		}
	};

	protected final HttpHeaders sharedHeaders = new DefaultHttpHeaders();
	protected final HttpHeaders dynamicHeaders = new DefaultHttpHeaders();
	protected final SecretKeySpec secretKey;

	private final Map<LoadType, HttpRequestFactory<I, O>>
		requestFactoryMap = new ConcurrentHashMap<>();
	private final Function<LoadType, HttpRequestFactory<I, O>> requestFactoryMapFunc;

	protected HttpStorageDriverBase(
		final String runId, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final String srcContainer, final boolean verifyFlag, final SocketConfig socketConfig
	) throws IllegalStateException {
		super(runId, loadConfig, storageConfig, socketConfig, srcContainer, verifyFlag);
		try {
			if(secret == null) {
				secretKey = null;
			} else {
				secretKey = new SecretKeySpec(secret.getBytes(UTF_8.name()), SIGN_METHOD);
			}
		} catch(final UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		requestFactoryMapFunc = loadType -> CrudHttpRequestFactory.getInstance(
			loadType, HttpStorageDriverBase.this, srcContainer
		);
		final HttpConfig httpConfig = storageConfig.getHttpConfig();
		final Map<String, String> headersMap = httpConfig.getHeaders();
		String headerValue;
		for(final String headerName : headersMap.keySet()) {
			headerValue = headersMap.get(headerName);
			if(-1 < headerName.indexOf(PATTERN_CHAR) || -1 < headerValue.indexOf(PATTERN_CHAR)) {
				dynamicHeaders.add(headerName, headerValue);
			} else {
				sharedHeaders.add(headerName, headerValue);
			}
		}
	}
	
	@Override
	protected ChannelInitializer<SocketChannel> getChannelInitializerImpl(
		final boolean sslFlag
	) {
		return new HttpClientChannelInitializer(sslFlag, getChannelHandlerImpl());
	}
	
	protected abstract HttpClientHandlerBase<I, O> getChannelHandlerImpl();
	
	@Override
	public final HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
	throws URISyntaxException {
		return requestFactoryMap
			.computeIfAbsent(ioTask.getLoadType(), requestFactoryMapFunc)
			.getHttpRequest(ioTask, nodeAddr);
	}
	
	@Override
	public HttpMethod getHttpMethod(final LoadType loadType) {
		switch(loadType) {
			case READ:
				return HttpMethod.GET;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				return HttpMethod.PUT;
		}
	}

	@Override
	public String getDstUriPath(final I item, final O ioTask) {
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

	@Override
	public final void applySharedHeaders(final HttpHeaders httpHeaders) {
		String sharedHeaderName;
		for(final Map.Entry<String, String> sharedHeader : sharedHeaders) {
			sharedHeaderName = sharedHeader.getKey();
			if(!httpHeaders.contains(sharedHeaderName)) {
				httpHeaders.add(sharedHeaderName, sharedHeader.getValue());
			}
		}
	}

	@Override
	public final void applyDynamicHeaders(final HttpHeaders httpHeaders) {

		String headerName;
		String headerValue;
		Input<String> headerNameInput;
		Input<String> headerValueInput;

		for(final Map.Entry<String, String> nextHeader : dynamicHeaders) {

			headerName = nextHeader.getKey();
			// header name is a generator pattern
			headerNameInput = HEADER_NAME_INPUTS.computeIfAbsent(headerName, PATTERN_INPUT_FUNC);
			if(headerNameInput == null) {
				continue;
			}
			// spin while header name generator is not ready
			try {
				while(null == (headerName = headerNameInput.get())) {
					LockSupport.parkNanos(1_000_000);
				}
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to calculate the header name");
				continue;
			}

			headerValue = nextHeader.getValue();
			// header value is a generator pattern
			headerValueInput = HEADER_VALUE_INPUTS.computeIfAbsent(headerValue, PATTERN_INPUT_FUNC);
			if(headerValueInput == null) {
				continue;
			}
			// spin while header value generator is not ready
			try {
				while(null == (headerValue = headerValueInput.get())) {
					LockSupport.parkNanos(1_000_000);
				}
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to calculate the header value");
				continue;
			}
			// put the generated header value into the request
			httpHeaders.set(headerName, headerValue);
		}
	}

	@Override
	public void applyMetaDataHeaders(final HttpHeaders httpHeaders) {
	}

	@Override
	public void applyAuthHeaders(
		final HttpMethod httpMethod, final String dstUriPath, final HttpHeaders httpHeaders
	) {
	}
	
	protected final FutureListener<Channel> getConnectionLeaseCallback(final O ioTask) {
		return new ConnectionLeaseCallback<>(ioTask, this);
	}
	
	private final static class ConnectionLeaseCallback<I extends Item, O extends IoTask<I>>
	implements FutureListener<Channel> {
		
		private final O ioTask;
		private final HttpStorageDriver<I, O> driver;
		
		public ConnectionLeaseCallback(final O ioTask, final HttpStorageDriver<I, O> driver) {
			this.ioTask = ioTask;
			this.driver = driver;
		}
		
		@Override
		public final void operationComplete(final Future<Channel> future)
		throws Exception {
			
			final Channel channel = future.getNow();
			final LoadType ioType = ioTask.getLoadType();
			final I item = ioTask.getItem();
			final String nodeAddr = ioTask.getNodeAddr();
			
			if(channel == null) {
				LOG.error(Markers.ERR, "Invalid behavior: no connection leased from the pool");
			} else {
				channel.attr(ATTR_KEY_IOTASK).set(ioTask);
				
				try {
					ioTask.startRequest();
					final HttpRequest httpRequest = driver.getHttpRequest(ioTask, nodeAddr);
					channel.write(httpRequest);
					if(LoadType.CREATE.equals(ioType)) {
						if(item instanceof DataItem) {
							final DataItem dataItem = (DataItem) item;
							channel
								.write(new DataItemFileRegion<>(dataItem))
								.addListener(new RequestSentCallback(ioTask));
							((DataIoTask) ioTask).setCountBytesDone(dataItem.size());
						}
					} else if(LoadType.UPDATE.equals(ioType)) {
						if(item instanceof MutableDataItem) {
							final MutableDataItem mdi = (MutableDataItem) item;
							final MutableDataIoTask mdIoTask = (MutableDataIoTask) ioTask;
							// TODO
						}
					}
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to write the data");
				} catch(final URISyntaxException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to build the request URI");
				}
				
				channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
			}
		}
	}
	
	private final static class RequestSentCallback
	implements FutureListener<Void> {
		
		private final IoTask ioTask;
		
		public RequestSentCallback(final IoTask ioTask) {
			this.ioTask = ioTask;
		}
		
		@Override
		public final void operationComplete(final Future<Void> future)
		throws Exception {
			ioTask.finishRequest();
		}
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
		if(secretKey != null) {
			try {
				secretKey.destroy();
			} catch(final DestroyFailedException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Failed to clear the secret key");
			}
		}
		nodeSelector.close();
		workerGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS);
	}
}
