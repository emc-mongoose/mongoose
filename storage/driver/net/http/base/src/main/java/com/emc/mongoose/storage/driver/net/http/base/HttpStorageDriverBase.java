package com.emc.mongoose.storage.driver.net.http.base;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.common.io.AsyncCurrentDateInput;
import com.emc.mongoose.common.io.pattern.AsyncPatternDefinedInput;
import com.emc.mongoose.model.io.IoType;
import static com.emc.mongoose.common.io.pattern.PatternDefinedInput.PATTERN_CHAR;
import static com.emc.mongoose.model.io.task.IoTask.SLASH;
import static com.emc.mongoose.model.item.DataItem.getRangeCount;
import static com.emc.mongoose.model.item.DataItem.getRangeOffset;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.NetConfig.HttpConfig;
import com.emc.mongoose.model.item.PathItem;
import com.emc.mongoose.model.item.TokenItem;
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
	
	protected final String namespace;
	protected final boolean fsAccess;
	protected final boolean versioning;
	protected final HttpHeaders sharedHeaders = new DefaultHttpHeaders();
	protected final HttpHeaders dynamicHeaders = new DefaultHttpHeaders();
	
	protected HttpStorageDriverBase(
		final String jobName, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final boolean verifyFlag
	) throws IllegalStateException {
		super(jobName, loadConfig, storageConfig, verifyFlag);
		
		final HttpConfig httpConfig = storageConfig.getNetConfig().getHttpConfig();
		
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
		try {
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
		} finally {
			channel.close();
		}
	}

	@Override
	protected void appendHandlers(final ChannelPipeline pipeline) {
		super.appendHandlers(pipeline);
		pipeline.addLast(new HttpClientCodec(REQ_LINE_LEN, HEADERS_LEN, CHUNK_SIZE, true));
		pipeline.addLast(new ChunkedWriteHandler());
	}

	protected HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
	throws URISyntaxException {

		final I item = ioTask.getItem();
		final IoType ioType = ioTask.getIoType();
		final String srcPath = ioTask.getSrcPath();

		final HttpMethod httpMethod;
		final String uriPath;
		if(item instanceof DataItem) {
			httpMethod = getDataHttpMethod(ioType);
			uriPath = getDataUriPath(item, srcPath, ioTask.getDstPath(), ioType);
		} else if(item instanceof TokenItem) {
			httpMethod = getTokenHttpMethod(ioType);
			uriPath = getTokenUriPath(item, srcPath, ioTask.getDstPath(), ioType);
		} else if(item instanceof PathItem) {
			httpMethod = getPathHttpMethod(ioType);
			uriPath = getPathUriPath(item, srcPath, ioTask.getDstPath(), ioType);
		} else {
			throw new AssertionError("Unsupported item class: " + item.getClass().getName());
		}

		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		if(nodeAddr != null) {
			httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		}
		httpHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateInput.INSTANCE.get());
		final HttpRequest httpRequest = new DefaultHttpRequest(
			HTTP_1_1, httpMethod, uriPath, httpHeaders
		);

		switch(ioType) {
			case CREATE:
				if(srcPath == null || srcPath.isEmpty()) {
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
					applyCopyHeaders(httpHeaders, getDataUriPath(item, srcPath, null, ioType));
					httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				}
				break;
			case READ:
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				if(ioTask instanceof DataIoTask) {
					applyByteRangesHeaders(httpHeaders, (DataIoTask) ioTask);
				}
				break;
			case UPDATE:
				final DataIoTask dataIoTask = (DataIoTask) ioTask;
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, dataIoTask.getMarkedRangesSize());
				applyByteRangesHeaders(httpHeaders, dataIoTask);
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
	
	protected HttpMethod getDataHttpMethod(final IoType ioType) {
		switch(ioType) {
			case READ:
				return HttpMethod.GET;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				return HttpMethod.PUT;
		}
	}

	protected abstract HttpMethod getTokenHttpMethod(final IoType ioType);

	protected abstract HttpMethod getPathHttpMethod(final IoType ioType);

	protected String getDataUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	) {
		final String itemName = item.getName();
		if(dstPath == null) {
			if(srcPath == null) {
				return SLASH + itemName;
			} else if(srcPath.endsWith(SLASH)) {
				return srcPath + itemName;
			} else {
				return srcPath + SLASH + itemName;
			}
		} else if(itemName.startsWith(dstPath)) {
			return itemName;
		} else {
			return (dstPath.endsWith(SLASH) ? dstPath : (dstPath + SLASH)) + itemName;
		}
	}

	protected abstract String getTokenUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	);

	protected abstract String getPathUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	);
	
	private final static ThreadLocal<StringBuilder>
		THR_LOC_RANGES_BUILDER = new ThreadLocal<StringBuilder>() {
			@Override
			protected StringBuilder initialValue() {
				return new StringBuilder();
			}
		};
	
	protected void applyByteRangesHeaders(
		final HttpHeaders httpHeaders, final DataIoTask dataIoTask
	) {
		final long baseItemSize;
		try {
			baseItemSize = dataIoTask.getItem().size();
		} catch(final IOException e) {
			throw new AssertionError(e);
		}
		final List<ByteRange> fixedByteRanges = dataIoTask.getFixedRanges();
		final StringBuilder strb = THR_LOC_RANGES_BUILDER.get();
		strb.setLength(0);

		if(fixedByteRanges == null || fixedByteRanges.isEmpty()) {
			final BitSet rangesMaskPair[] = dataIoTask.getMarkedRangesMaskPair();
			if(rangesMaskPair[0].isEmpty() && rangesMaskPair[1].isEmpty()) {
				return; // do not set the ranges header
			}
			// current layer first
			for(int i = 0; i < getRangeCount(baseItemSize); i++) {
				if(rangesMaskPair[0].get(i)) {
					if(strb.length() > 0) {
						strb.append(',');
					}
					strb
						.append(getRangeOffset(i))
						.append('-')
						.append(Math.min(getRangeOffset(i + 1), baseItemSize) - 1);
				}
			}
			// then next layer ranges if any
			for(int i = 0; i < getRangeCount(baseItemSize); i++) {
				if(rangesMaskPair[1].get(i)) {
					if(strb.length() > 0) {
						strb.append(',');
					}
					strb
						.append(getRangeOffset(i))
						.append('-')
						.append(Math.min(getRangeOffset(i + 1), baseItemSize) - 1);
				}
			}

		} else { // fixed byte ranges
			ByteRange nextFixedByteRange;
			long nextRangeSize;
			for(int i = 0; i < fixedByteRanges.size(); i ++) {
				nextFixedByteRange = fixedByteRanges.get(i);
				nextRangeSize = nextFixedByteRange.getSize();
				if(i > 0) {
					strb.append(',');
				}
				if(nextRangeSize == -1) {
					strb.append(nextFixedByteRange.toString());
				} else {
					strb.append(baseItemSize).append("-");
				}
			}
		}
		httpHeaders.set(HttpHeaderNames.RANGE, "bytes=" + strb.toString());
	}

	protected void applySharedHeaders(final HttpHeaders httpHeaders) {
		for(final Map.Entry<String, String> sharedHeader : sharedHeaders) {
			httpHeaders.add(sharedHeader.getKey(), sharedHeader.getValue());
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
			httpHeaders.set(headerName, headerValue);
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
				if(item instanceof DataItem) {
					
					final DataItem dataItem = (DataItem) item;
					final DataIoTask dataIoTask = (DataIoTask) ioTask;

					final List<ByteRange> fixedByteRanges = dataIoTask.getFixedRanges();
					if(fixedByteRanges == null || fixedByteRanges.isEmpty()) {
						// random range update case
						final BitSet updRangesMaskPair[] = dataIoTask.getMarkedRangesMaskPair();
						final int rangeCount = getRangeCount(dataItem.size());
						DataItem updatedRange;
						// current layer updates first
						for(int i = 0; i < rangeCount; i ++) {
							if(updRangesMaskPair[0].get(i)) {
								dataIoTask.setCurrRangeIdx(i);
								updatedRange = dataIoTask.getCurrRangeUpdate();
								assert updatedRange != null;
								channel.write(new DataItemFileRegion<>(updatedRange));
							}
						}
						// then next layer updates if any
						for(int i = 0; i < rangeCount; i ++) {
							if(updRangesMaskPair[1].get(i)) {
								dataIoTask.setCurrRangeIdx(i);
								updatedRange = dataIoTask.getCurrRangeUpdate();
								assert updatedRange != null;
								channel.write(new DataItemFileRegion<>(updatedRange));
							}
						}
						dataItem.commitUpdatedRanges(dataIoTask.getMarkedRangesMaskPair());
					} else { // append case
						final long baseItemSize = dataItem.size();
						long beg;
						long end;
						long size;
						for(final ByteRange fixedByteRange : fixedByteRanges) {
							beg = fixedByteRange.getBeg();
							end = fixedByteRange.getEnd();
							size = fixedByteRange.getSize();
							if(size == -1) {
								if(beg == -1) {
									beg = baseItemSize - end;
									size = end;
								} else if(end == -1) {
									size = baseItemSize - beg;
								} else {
									size = end - beg + 1;
								}
							} else {
								// append
								beg = baseItemSize;
							}
							channel.write(new DataItemFileRegion<>(dataItem.slice(beg, size)));
						}
						dataItem.size(dataItem.size() + dataIoTask.getMarkedRangesSize());
					}
					dataIoTask.setCountBytesDone(dataIoTask.getMarkedRangesSize());
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
	protected void doClose()
	throws IOException {
		super.doClose();
		sharedHeaders.clear();
		dynamicHeaders.clear();
		headerNameInputs.clear();
		headerValueInputs.clear();
	}
}
