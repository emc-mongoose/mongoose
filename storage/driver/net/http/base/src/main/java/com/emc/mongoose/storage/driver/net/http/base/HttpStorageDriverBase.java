package com.emc.mongoose.storage.driver.net.http.base;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.model.io.task.DataIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.MutableDataIoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.MutableDataItem;
import com.emc.mongoose.common.io.AsyncCurrentDateInput;
import com.emc.mongoose.common.io.pattern.AsyncPatternDefinedInput;
import com.emc.mongoose.model.load.LoadType;
import static com.emc.mongoose.common.io.pattern.PatternDefinedInput.PATTERN_CHAR;
import static com.emc.mongoose.model.io.task.IoTask.SLASH;
import static com.emc.mongoose.model.item.MutableDataItem.getRangeCount;
import static com.emc.mongoose.model.item.MutableDataItem.getRangeOffset;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.HttpConfig;
import com.emc.mongoose.storage.driver.net.base.NetStorageDriverBase;
import com.emc.mongoose.storage.driver.net.base.data.DataItemFileRegion;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
	
	private final Map<String, Input<String>> headerNameInputs = new ConcurrentHashMap<>();
	private final Map<String, Input<String>> headerValueInputs = new ConcurrentHashMap<>();
	private static final Function<String, Input<String>> PATTERN_INPUT_FUNC = headerName -> {
		try {
			return new AsyncPatternDefinedInput(headerName);
		} catch(final UserShootHisFootException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to create the pattern defined input");
			return null;
		}
	};
	
	protected volatile String authToken = null;
	protected final String namespace;
	protected final boolean fsAccess;
	protected final boolean versioning;
	protected final HttpHeaders sharedHeaders = new DefaultHttpHeaders();
	protected final HttpHeaders dynamicHeaders = new DefaultHttpHeaders();
	
	protected HttpStorageDriverBase(
		final String jobName, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final boolean verifyFlag, final SocketConfig socketConfig
	) throws IllegalStateException {
		super(jobName, loadConfig, storageConfig, socketConfig, verifyFlag);
		
		final HttpConfig httpConfig = storageConfig.getHttpConfig();
		
		authToken = storageConfig.getAuthConfig().getToken();
		namespace = httpConfig.getNamespace();
		fsAccess = httpConfig.getFsAccess();
		versioning = httpConfig.getVersioning();
		
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
	public void channelCreated(final Channel channel)
	throws Exception {
		super.channelCreated(channel);
		final ChannelPipeline pipeline = channel.pipeline();
		pipeline.addLast(new HttpClientCodec(REQ_LINE_LEN, HEADERS_LEN, CHUNK_SIZE, true));
		pipeline.addLast(new ChunkedWriteHandler());
	}
	
	@Override
	public final HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
	throws URISyntaxException {

		final I item = ioTask.getItem();
		final LoadType ioType = ioTask.getLoadType();
		final HttpMethod httpMethod = getHttpMethod(ioType);

		String srcUriPath = ioTask.getSrcPath();
		if(srcUriPath != null) {
			if(srcUriPath.endsWith(SLASH)) {
				srcUriPath = srcUriPath + item.getName();
			} else {
				srcUriPath = srcUriPath + SLASH + item.getName();
			}
		}

		String dstUriPath = ioTask.getDstPath();
		if(dstUriPath != null) {
			if(dstUriPath.endsWith(SLASH)) {
				dstUriPath = dstUriPath + item.getName();
			} else {
				dstUriPath = dstUriPath + SLASH + item.getName();
			}
		}

		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		httpHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateInput.INSTANCE.get());
		final HttpRequest httpRequest = new DefaultHttpRequest(
			HTTP_1_1, httpMethod, dstUriPath == null ? srcUriPath : dstUriPath, httpHeaders
		);

		switch(ioType) {
			case CREATE:
				if(srcUriPath == null) {
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
					applyCopyHeaders(httpHeaders, srcUriPath);
					httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				}
				break;
			case READ:
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				break;
			case UPDATE:
				final MutableDataItem mdi = (MutableDataItem) item;
				final MutableDataIoTask mdIoTask = (MutableDataIoTask) ioTask;
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, mdIoTask.getUpdatingRangesSize());
				final BitSet updRangesMaskPair[] = mdIoTask.getUpdRangesMaskPair();
				final StringBuilder strb = new StringBuilder();
				try {
					final long baseItemSize = mdi.size();
					for(int i = 0; i < getRangeCount(baseItemSize); i++) {
						if(updRangesMaskPair[0].get(i) || updRangesMaskPair[1].get(i)) {
							if(strb.length() > 0) {
								strb.append(',');
							}
							strb
								.append(getRangeOffset(i))
								.append('-')
								.append(Math.min(getRangeOffset(i + 1), baseItemSize) - 1);
						}
					}
				} catch(final IOException ignored) {
				}
				httpHeaders.set(HttpHeaderNames.RANGE, "bytes=" + strb.toString());
				break;
			case DELETE:
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				break;
		}

		applyMetaDataHeaders(httpHeaders);
		applyAuthHeaders(httpMethod, dstUriPath, httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);
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

	private void applySharedHeaders(final HttpHeaders httpHeaders) {
		String sharedHeaderName;
		for(final Map.Entry<String, String> sharedHeader : sharedHeaders) {
			sharedHeaderName = sharedHeader.getKey();
			if(!httpHeaders.contains(sharedHeaderName)) {
				httpHeaders.add(sharedHeaderName, sharedHeader.getValue());
			}
		}
	}

	private void applyDynamicHeaders(final HttpHeaders httpHeaders) {

		String headerName;
		String headerValue;
		Input<String> headerNameInput;
		Input<String> headerValueInput;

		for(final Map.Entry<String, String> nextHeader : dynamicHeaders) {

			headerName = nextHeader.getKey();
			// header name is a generator pattern
			headerNameInput = headerNameInputs.computeIfAbsent(headerName, PATTERN_INPUT_FUNC);
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
			headerValueInput = headerValueInputs.computeIfAbsent(headerValue, PATTERN_INPUT_FUNC);
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

	protected abstract void applyMetaDataHeaders(final HttpHeaders httpHeaders);

	protected abstract void applyAuthHeaders(
		final HttpMethod httpMethod, final String dstUriPath, final HttpHeaders httpHeaders
	);

	protected abstract void applyCopyHeaders(final HttpHeaders httpHeaders, final String srcPath)
	throws URISyntaxException;
	
	protected final FutureListener<Channel> getConnectionLeaseCallback(final O ioTask) {
		return new ConnectionLeaseCallback<>(ioTask, this);
	}
	
	private static final class ConnectionLeaseCallback<I extends Item, O extends IoTask<I>>
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
				if(!driver.isClosed() && !driver.isInterrupted()) {
					LOG.warn(Markers.ERR, "Failed to obtain the connection to {}", nodeAddr);
				} // else ignore
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
							DataItem updatedRange;
							ChannelFuture reqSentFuture = null;
							for(int i = 0; i < getRangeCount(mdi.size()); i ++) {
								mdIoTask.setCurrRangeIdx(i);
								updatedRange = mdIoTask.getCurrRangeUpdate();
								if(updatedRange != null) {
									reqSentFuture = channel.write(
										new DataItemFileRegion<>(updatedRange)
									);
								}
							}
							if(reqSentFuture == null) {
								LOG.error(Markers.ERR, "No ranges scheduled for the update");
								ioTask.setStatus(IoTask.Status.RESP_FAIL_CLIENT);
							} else {
								reqSentFuture.addListener(new RequestSentCallback(mdIoTask));
							}
							mdIoTask.setCountBytesDone(mdIoTask.getUpdatingRangesSize());
							mdi.commitUpdatedRanges(mdIoTask.getUpdRangesMaskPair());
						}
					}
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to write the data");
				} catch(final URISyntaxException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to build the request URI");
				} catch(final Exception e) {
					LogUtil.exception(LOG, Level.WARN, e, "Send HTTP request failure");
				}
				
				channel
					.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
					.addListener(new RequestSentCallback(ioTask));
			}
		}
	}
	
	private static final class RequestSentCallback
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
	protected void doClose()
	throws IOException {
		super.doClose();
		sharedHeaders.clear();
		dynamicHeaders.clear();
		headerNameInputs.clear();
		headerValueInputs.clear();
	}
}
