package com.emc.mongoose.storage.driver.coop.net.http;

import com.github.akurilov.commons.collection.Range;

import com.emc.mongoose.exception.OmgDoesNotPerformException;
import com.emc.mongoose.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.supply.BatchSupplier;
import com.emc.mongoose.supply.async.AsyncPatternDefinedSupplier;
import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.item.io.task.data.DataIoTask;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.item.DataItem;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.io.IoType;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;
import static com.emc.mongoose.supply.PatternDefinedSupplier.PATTERN_CHAR;
import static com.emc.mongoose.item.io.task.IoTask.SLASH;
import static com.emc.mongoose.item.DataItem.rangeCount;
import static com.emc.mongoose.item.DataItem.rangeOffset;
import com.emc.mongoose.item.PathItem;
import com.emc.mongoose.item.TokenItem;
import com.emc.mongoose.storage.Credential;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import com.emc.mongoose.storage.driver.coop.net.NetStorageDriverBase;

import com.github.akurilov.confuse.Config;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

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

	public static final AsyncCurrentDateSupplier DATE_SUPPLIER;
	static {
		try {
			DATE_SUPPLIER = new AsyncCurrentDateSupplier(ServiceTaskExecutor.INSTANCE);
		} catch(final OmgDoesNotPerformException e) {
			throw new RuntimeException(e);
		}
	}

	private final static String CLS_NAME = HttpStorageDriverBase.class.getSimpleName();

	private final Map<String, BatchSupplier<String>> headerNameInputs = new ConcurrentHashMap<>();
	private final Map<String, BatchSupplier<String>> headerValueInputs = new ConcurrentHashMap<>();
	private static final Function<String, BatchSupplier<String>>
		ASYNC_PATTERN_SUPPLIER_FUNC = pattern -> {
			try {
				return new AsyncPatternDefinedSupplier(ServiceTaskExecutor.INSTANCE, pattern);
			} catch(final OmgShootMyFootException e) {
				LogUtil.exception(Level.ERROR, e, "Failed to create the pattern defined input");
				return null;
			}
		};
	
	protected final String namespace;
	protected final boolean fsAccess;
	protected final boolean versioning;
	protected final HttpHeaders sharedHeaders = new DefaultHttpHeaders();
	protected final HttpHeaders dynamicHeaders = new DefaultHttpHeaders();
	
	protected HttpStorageDriverBase(
		final String testStepId, final DataInput itemDataInput, final Config loadConfig,
		final Config storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException, InterruptedException {

		super(testStepId, itemDataInput, loadConfig, storageConfig, verifyFlag);
		
		final Config httpConfig = storageConfig.configVal("net-http");
		
		namespace = httpConfig.stringVal("namespace");
		fsAccess = httpConfig.boolVal("fsAccess");
		versioning = httpConfig.boolVal("versioning");
		
		final Map<String, String> headersMap = httpConfig.mapVal("headers");
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

	protected FullHttpResponse executeHttpRequest(final FullHttpRequest request)
	throws InterruptedException, ConnectException {

		ThreadContext.put(KEY_STEP_ID, stepId);
		ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);

		final Channel channel = getUnpooledConnection();
		try {
			final ChannelPipeline pipeline = channel.pipeline();
			Loggers.MSG.debug(
				"{}: execute the HTTP request using the channel {} w/ pipeline: {}", stepId,
				channel.hashCode(), pipeline
			);
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

		final I item = ioTask.item();
		final IoType ioType = ioTask.ioType();
		final String srcPath = ioTask.srcPath();

		final HttpMethod httpMethod;
		final String uriPath;
		if(item instanceof DataItem) {
			httpMethod = getDataHttpMethod(ioType);
			uriPath = getDataUriPath(item, srcPath, ioTask.dstPath(), ioType);
		} else if(item instanceof TokenItem) {
			httpMethod = getTokenHttpMethod(ioType);
			uriPath = getTokenUriPath(item, srcPath, ioTask.dstPath(), ioType);
		} else if(item instanceof PathItem) {
			httpMethod = getPathHttpMethod(ioType);
			uriPath = getPathUriPath(item, srcPath, ioTask.dstPath(), ioType);
		} else {
			throw new AssertionError("Unsupported item class: " + item.getClass().getName());
		}

		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		if(nodeAddr != null) {
			httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		}
		httpHeaders.set(HttpHeaderNames.DATE, DATE_SUPPLIER.get());
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
					applyRangesHeaders(httpHeaders, (DataIoTask) ioTask);
				}
				break;
			case UPDATE:
				final DataIoTask dataIoTask = (DataIoTask) ioTask;
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, dataIoTask.markedRangesSize());
				applyRangesHeaders(httpHeaders, dataIoTask);
				break;
			case DELETE:
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				break;
		}

		applyMetaDataHeaders(httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);
		applyAuthHeaders(httpHeaders, httpMethod, uriPath, ioTask.credential());

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
				if(itemName.startsWith(SLASH)) {
					return itemName;
				} else {
					return SLASH + itemName;
				}
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
	
	protected void applyRangesHeaders(
		final HttpHeaders httpHeaders, final DataIoTask dataIoTask
	) {
		final long baseItemSize;
		try {
			baseItemSize = dataIoTask.item().size();
		} catch(final IOException e) {
			throw new AssertionError(e);
		}
		final List<Range> fixedRanges = dataIoTask.fixedRanges();
		final StringBuilder strb = THR_LOC_RANGES_BUILDER.get();
		strb.setLength(0);

		if(fixedRanges == null || fixedRanges.isEmpty()) {
			final BitSet rangesMaskPair[] = dataIoTask.markedRangesMaskPair();
			if(rangesMaskPair[0].isEmpty() && rangesMaskPair[1].isEmpty()) {
				return; // do not set the ranges header
			}
			// current layer first
			for(int i = 0; i < rangeCount(baseItemSize); i++) {
				if(rangesMaskPair[0].get(i)) {
					if(strb.length() > 0) {
						strb.append(',');
					}
					strb
						.append(rangeOffset(i))
						.append('-')
						.append(Math.min(rangeOffset(i + 1), baseItemSize) - 1);
				}
			}
			// then next layer ranges if any
			for(int i = 0; i < rangeCount(baseItemSize); i++) {
				if(rangesMaskPair[1].get(i)) {
					if(strb.length() > 0) {
						strb.append(',');
					}
					strb
						.append(rangeOffset(i))
						.append('-')
						.append(Math.min(rangeOffset(i + 1), baseItemSize) - 1);
				}
			}

		} else { // fixed byte ranges
			rangeListToStringBuff(fixedRanges, baseItemSize, strb);
		}
		httpHeaders.set(HttpHeaderNames.RANGE, "bytes=" + strb.toString());
	}

	protected static void rangeListToStringBuff(
		final List<Range> ranges, final long baseLength, final StringBuilder dstBuff
	) {
		Range nextFixedRange;
		long nextRangeSize;
		for(int i = 0; i < ranges.size(); i ++) {
			nextFixedRange = ranges.get(i);
			nextRangeSize = nextFixedRange.getSize();
			if(i > 0) {
				dstBuff.append(',');
			}
			if(nextRangeSize == -1) {
				dstBuff.append(nextFixedRange.toString());
			} else {
				dstBuff.append(baseLength).append("-");
			}
		}
	}

	protected void applySharedHeaders(final HttpHeaders httpHeaders) {
		for(final Map.Entry<String, String> sharedHeader : sharedHeaders) {
			httpHeaders.add(sharedHeader.getKey(), sharedHeader.getValue());
		}
	}

	protected void applyDynamicHeaders(final HttpHeaders httpHeaders) {

		String headerName;
		String headerValue;
		BatchSupplier<String> headerNameSupplier;
		BatchSupplier<String> headerValueSupplier;

		for(final Map.Entry<String, String> nextHeader : dynamicHeaders) {

			headerName = nextHeader.getKey();
			// header name is a generator pattern
			headerNameSupplier = headerNameInputs.computeIfAbsent(
				headerName, ASYNC_PATTERN_SUPPLIER_FUNC
			);
			if(headerNameSupplier == null) {
				continue;
			}
			// spin while header name generator is not ready
			while(null == (headerName = headerNameSupplier.get())) {
				LockSupport.parkNanos(1_000_000);
			}

			headerValue = nextHeader.getValue();
			// header value is a generator pattern
			headerValueSupplier = headerValueInputs.computeIfAbsent(headerValue,
				ASYNC_PATTERN_SUPPLIER_FUNC
			);
			if(headerValueSupplier == null) {
				continue;
			}
			// spin while header value generator is not ready
			while(null == (headerValue = headerValueSupplier.get())) {
				LockSupport.parkNanos(1_000_000);
			}
			// put the generated header value into the request
			httpHeaders.set(headerName, headerValue);
		}
	}

	protected abstract void applyMetaDataHeaders(final HttpHeaders httpHeaders);

	protected abstract void applyAuthHeaders(
		final HttpHeaders httpHeaders, final HttpMethod httpMethod, final String dstUriPath,
		final Credential credential
	);

	protected abstract void applyCopyHeaders(final HttpHeaders httpHeaders, final String srcPath)
	throws URISyntaxException;

	@Override
	protected final void sendRequest(
		final Channel channel, final ChannelPromise channelPromise, final O ioTask
	) {

		final String nodeAddr = ioTask.nodeAddr();
		try {
			final HttpRequest httpRequest = getHttpRequest(ioTask, nodeAddr);
			if(channel == null) {
				return;
			} else {
				channel.write(httpRequest);
				if(Loggers.MSG.isTraceEnabled()) {
					Loggers.MSG.trace(
						"{} >>>> {} {}", ioTask.hashCode(), httpRequest.method(), httpRequest.uri()
					);
				}
			}
			if(!(httpRequest instanceof FullHttpRequest)) {
				sendRequestData(channel, ioTask);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to write the data");
		} catch(final URISyntaxException e) {
			LogUtil.exception(Level.WARN, e, "Failed to build the request URI");
		} catch(final Exception e) {
			if(!isStopped() && !isClosed()) {
				LogUtil.exception(Level.WARN, e, "Send HTTP request failure");
			}
		} catch(final Throwable e) {
			e.printStackTrace(System.err);
		}

		channel.write(LastHttpContent.EMPTY_LAST_CONTENT, channelPromise);
		channel.flush();
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
