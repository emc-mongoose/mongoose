package com.emc.mongoose.storage.driver.net.http.base;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.data.mutable.MutableDataIoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.MutableDataItem;
import com.emc.mongoose.common.io.AsyncCurrentDateInput;
import com.emc.mongoose.common.io.pattern.AsyncPatternDefinedInput;
import com.emc.mongoose.model.io.IoType;
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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedWriteHandler;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.util.AsciiString;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

/**
 Created by kurila on 29.07.16.
 Netty-based concurrent HTTP client executing the submitted I/O tasks.
 */
public abstract class HttpStorageDriverBase<I extends Item, O extends IoTask<I, R>, R extends IoResult>
extends NetStorageDriverBase<I, O, R>
implements HttpStorageDriver<I, O, R> {
	
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

	protected final FullHttpResponse executeHttpRequest(final FullHttpRequest request)
	throws InterruptedException, ConnectException {
		final Channel channel = getUnpooledConnection();
		final ChannelPipeline pipeline = channel.pipeline();
		pipeline.removeLast(); // remove the API specific handler
		final SynchronousQueue<FullHttpResponse> fullRespSync = new SynchronousQueue<>();
		pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
		pipeline.addLast(
			new SimpleChannelInboundHandler<HttpObject>() {
				@Override
				protected final void channelRead0(
					final ChannelHandlerContext ctx, final HttpObject msg
				) throws Exception {
					if(msg instanceof FullHttpResponse) {
						fullRespSync.put(((FullHttpResponse) msg).retain());
					}
				}
			}
		);
		channel.writeAndFlush(request).sync();
		return fullRespSync.take();
	}

	@Override
	protected void appendHandlers(final ChannelPipeline pipeline) {
		super.appendHandlers(pipeline);
		pipeline.addLast(new HttpClientCodec(REQ_LINE_LEN, HEADERS_LEN, CHUNK_SIZE, true));
		pipeline.addLast(new ChunkedWriteHandler());
	}

	private final static ThreadLocal<StringBuilder>
		THR_LOC_RANGES_BUILDER = new ThreadLocal<StringBuilder>() {
			@Override
			protected StringBuilder initialValue() {
				return new StringBuilder();
			}
		};

	protected HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
	throws URISyntaxException {

		final I item = ioTask.getItem();
		final IoType ioType = ioTask.getIoType();
		final HttpMethod httpMethod = getHttpMethod(ioType);

		final String srcPath = ioTask.getSrcPath();
		final String uriPath = getUriPath(item, srcPath, ioTask.getDstPath(), ioType);

		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		httpHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateInput.INSTANCE.get());
		final HttpRequest httpRequest = new DefaultHttpRequest(
			HTTP_1_1, httpMethod, uriPath, httpHeaders
		);

		switch(ioType) {
			case CREATE:
				if(srcPath == null) {
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
					applyCopyHeaders(httpHeaders, getUriPath(item, srcPath, null, ioType));
					httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				}
				break;
			case READ:
				// TODO partial read support
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				break;
			case UPDATE:
				final MutableDataItem mdi = (MutableDataItem) item;
				final MutableDataIoTask mdIoTask = (MutableDataIoTask) ioTask;
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, mdIoTask.getUpdatingRangesSize());
				final long baseItemSize;
				try {
					baseItemSize = mdi.size();
				} catch(final IOException e) {
					throw new IllegalStateException(e);
				}
				final List<ByteRange> fixedByteRanges = mdIoTask.getFixedRanges();
				final StringBuilder strb = THR_LOC_RANGES_BUILDER.get();
				strb.setLength(0);
				if(fixedByteRanges == null || fixedByteRanges.isEmpty()) { // random range update
					final BitSet updRangesMaskPair[] = mdIoTask.getUpdRangesMaskPair();
					// current layer updates first
					for(int i = 0; i < getRangeCount(baseItemSize); i++) {
						if(updRangesMaskPair[0].get(i)) {
							if(strb.length() > 0) {
								strb.append(',');
							}
							strb
								.append(getRangeOffset(i))
								.append('-')
								.append(Math.min(getRangeOffset(i + 1), baseItemSize) - 1);
						}
					}
					// then next layer updates if any
					for(int i = 0; i < getRangeCount(baseItemSize); i++) {
						if(updRangesMaskPair[1].get(i)) {
							if(strb.length() > 0) {
								strb.append(',');
							}
							strb
								.append(getRangeOffset(i))
								.append('-')
								.append(Math.min(getRangeOffset(i + 1), baseItemSize) - 1);
						}
					}
				} else { // append
					for(final ByteRange nextFixedByteRange : fixedByteRanges) {
						strb.append(nextFixedByteRange.toString());
					}
				}
				httpHeaders.set(HttpHeaderNames.RANGE, "bytes=" + strb.toString());
				break;
			case DELETE:
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				break;
		}

		applyMetaDataHeaders(httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);
		applyAuthHeaders(httpMethod, uriPath, httpHeaders);

		return httpRequest;
	}
	
	protected HttpMethod getHttpMethod(final IoType ioType) {
		switch(ioType) {
			case READ:
				return HttpMethod.GET;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				return HttpMethod.PUT;
		}
	}

	protected String getUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	) {
		if(dstPath == null) {
			if(srcPath == null) {
				return SLASH + item.getName();
			} else if(srcPath.endsWith(SLASH)) {
				return srcPath + item.getName();
			} else {
				return srcPath + SLASH + item.getName();
			}
		} else if(dstPath.endsWith(SLASH)) {
			return dstPath + item.getName();
		} else {
			return dstPath + SLASH + item.getName();
		}
	}

	protected void applySharedHeaders(final HttpHeaders httpHeaders) {
		String sharedHeaderName;
		for(final Map.Entry<String, String> sharedHeader : sharedHeaders) {
			sharedHeaderName = sharedHeader.getKey();
			if(!httpHeaders.contains(sharedHeaderName)) {
				httpHeaders.add(new AsciiString(sharedHeaderName), sharedHeader.getValue());
			}
		}
	}

	protected void applyDynamicHeaders(final HttpHeaders httpHeaders) {

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
			httpHeaders.set(new AsciiString(headerName), headerValue);
		}
	}

	protected abstract void applyMetaDataHeaders(final HttpHeaders httpHeaders);

	protected abstract void applyAuthHeaders(
		final HttpMethod httpMethod, final String dstUriPath, final HttpHeaders httpHeaders
	);

	protected abstract void applyCopyHeaders(final HttpHeaders httpHeaders, final String srcPath)
	throws URISyntaxException;

	@Override
	protected final ChannelFuture sendRequest(final Channel channel, final O ioTask) {

		final String nodeAddr = ioTask.getNodeAddr();
		final IoType ioType = ioTask.getIoType();
		final I item = ioTask.getItem();
		
		try {

			final HttpRequest httpRequest = getHttpRequest(ioTask, nodeAddr);
			if(channel == null) {
				return null;
			} else {
				channel.write(httpRequest);
			}

			if(IoType.CREATE.equals(ioType)) {
				if(item instanceof DataItem) {
					final DataIoTask dataIoTask = (DataIoTask) ioTask;
					if(!(dataIoTask instanceof CompositeDataIoTask)) {
						final DataItem dataItem = (DataItem) item;
						final String srcPath = dataIoTask.getSrcPath();
						if(null == srcPath || srcPath.isEmpty()) {
							channel.write(new DataItemFileRegion<>(dataItem));
						}
						dataIoTask.setCountBytesDone(dataItem.size());
					}
				}
			} else if(IoType.UPDATE.equals(ioType)) {
				if(item instanceof MutableDataItem) {
					
					final MutableDataItem mdi = (MutableDataItem) item;
					final MutableDataIoTask mdIoTask = (MutableDataIoTask) ioTask;

					final List<ByteRange> fixedByteRanges = mdIoTask.getFixedRanges();
					if(fixedByteRanges == null || fixedByteRanges.isEmpty()) {
						// random range update case
						final BitSet updRangesMaskPair[] = mdIoTask.getUpdRangesMaskPair();
						final int rangeCount = getRangeCount(mdi.size());
						DataItem updatedRange;
						// current layer updates first
						for(int i = 0; i < rangeCount; i ++) {
							if(updRangesMaskPair[0].get(i)) {
								mdIoTask.setCurrRangeIdx(i);
								updatedRange = mdIoTask.getCurrRangeUpdate();
								assert updatedRange != null;
								channel.write(new DataItemFileRegion<>(updatedRange));
							}
						}
						// then next layer updates if any
						for(int i = 0; i < rangeCount; i ++) {
							if(updRangesMaskPair[1].get(i)) {
								mdIoTask.setCurrRangeIdx(i);
								updatedRange = mdIoTask.getCurrRangeUpdate();
								assert updatedRange != null;
								channel.write(new DataItemFileRegion<>(updatedRange));
							}
						}
						mdi.commitUpdatedRanges(mdIoTask.getUpdRangesMaskPair());
					} else { // append case
						final long baseItemSize = mdi.size();
						long beg;
						long end;
						for(final ByteRange fixedByteRange : fixedByteRanges) {
							beg = fixedByteRange.getBeg();
							end = fixedByteRange.getEnd();
							if(beg == -1) {
								channel.write(
									new DataItemFileRegion<>(mdi.slice(baseItemSize, end))
								);
							} else if(end == -1) {
								channel.write(
									new DataItemFileRegion<>(mdi.slice(beg, baseItemSize - beg))
								);
							} else {
								channel.write(
									new DataItemFileRegion<>(mdi.slice(beg, end - beg + 1))
								);
							}
						}
						mdi.size(mdi.size() + mdIoTask.getUpdatingRangesSize());
					}
					mdIoTask.setCountBytesDone(mdIoTask.getUpdatingRangesSize());
				}
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to write the data");
		} catch(final URISyntaxException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to build the request URI");
		} catch(final Exception e) {
			if(!isInterrupted() && !isClosed()) {
				LogUtil.exception(LOG, Level.WARN, e, "Send HTTP request failure");
			}
		} catch(final Throwable e) {
			e.printStackTrace(System.err);
		}

		return channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
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
