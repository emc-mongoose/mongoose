package com.emc.mongoose.storage.driver.coop.netty.http;

import static com.emc.mongoose.base.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.base.Constants.KEY_STEP_ID;
import static com.emc.mongoose.base.item.DataItem.rangeCount;
import static com.emc.mongoose.base.item.DataItem.rangeOffset;
import static com.emc.mongoose.base.item.op.Operation.SLASH;
import static com.github.akurilov.commons.io.el.ExpressionInput.ASYNC_MARKER;
import static com.github.akurilov.commons.io.el.ExpressionInput.INIT_MARKER;
import static com.github.akurilov.commons.io.el.ExpressionInput.SYNC_MARKER;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.emc.mongoose.base.config.ConstantValueInputImpl;
import com.emc.mongoose.base.config.el.CompositeExpressionInputBuilder;
import com.emc.mongoose.base.data.DataInput;
import com.emc.mongoose.base.exception.InterruptRunException;
import com.emc.mongoose.base.exception.OmgShootMyFootException;
import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.PathItem;
import com.emc.mongoose.base.item.TokenItem;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.Operation.Status;
import com.emc.mongoose.base.item.op.data.DataOperation;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.storage.Credential;
import com.emc.mongoose.storage.driver.coop.netty.NettyStorageDriverBase;
import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.confuse.Config;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
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
import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.nio.channels.ClosedChannelException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

/**
* Created by kurila on 29.07.16. Netty-based concurrent HTTP client executing the submitted load
* operations.
*/
public abstract class HttpStorageDriverBase<I extends Item, O extends Operation<I>>
				extends NettyStorageDriverBase<I, O> implements HttpStorageDriver<I, O> {

	private static final String CLS_NAME = HttpStorageDriverBase.class.getSimpleName();
	private static final Function<String, Input<String>> EXPR_INPUT_FUNC = expr -> CompositeExpressionInputBuilder.newInstance()
					.expression(expr)
					.build();
	private final Map<String, Input<String>> headerNameInputs = new ConcurrentHashMap<>();
	private final Map<String, Input<String>> headerValueInputs = new ConcurrentHashMap<>();
	protected final HttpHeaders sharedHeaders = new DefaultHttpHeaders();
	protected final Map<String, String> dynamicHeaders = new HashMap<>();
	private final Input<String> uriQueryInput;
	protected final ChannelFutureListener httpReqSentCallback = this::sendHttpRequestComplete;

	protected HttpStorageDriverBase(
					final String testStepId,
					final DataInput itemDataInput,
					final Config storageConfig,
					final boolean verifyFlag,
					final int batchSize)
					throws OmgShootMyFootException, InterruptedException {
		super(testStepId, itemDataInput, storageConfig, verifyFlag, batchSize);
		final var httpConfig = storageConfig.configVal("net-http");
		final var headersMap = httpConfig.<String> mapVal("headers");
		for (final var header : headersMap.entrySet()) {
			final var headerKey = header.getKey();
			final var headerValue = header.getValue();
			if (headerKey.contains(ASYNC_MARKER)
							|| headerKey.contains(SYNC_MARKER)
							|| headerKey.contains(INIT_MARKER)
							|| headerValue.contains(ASYNC_MARKER)
							|| headerValue.contains(SYNC_MARKER)
							|| headerValue.contains(INIT_MARKER)) {
				dynamicHeaders.put(headerKey, headerValue);
			} else {
				sharedHeaders.add(headerKey, headerValue);
			}
		}
		final var uriArgs = httpConfig.<String> mapVal("uri-args");
		final var uriQueryExpr = uriArgs.entrySet().stream()
						.map(entry -> entry.getKey() + '=' + entry.getValue())
						.collect(Collectors.joining("&"));
		if (uriQueryExpr.length() > 0) {
			uriQueryInput = EXPR_INPUT_FUNC.apply('?' + uriQueryExpr);
		} else {
			uriQueryInput = new ConstantValueInputImpl("");
		}
	}

	protected FullHttpResponse executeHttpRequest(final FullHttpRequest request)
					throws InterruptedException, ConnectException {
		ThreadContext.put(KEY_STEP_ID, stepId);
		ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);
		final FullHttpResponse resp;
		final var channel = getUnpooledConnection(storageNodeAddrs[0], storageNodePort);
		try {
			final var pipeline = channel.pipeline();
			Loggers.MSG.debug(
							"{}: execute the HTTP request using the channel {} w/ pipeline: {}",
							stepId,
							channel.hashCode(),
							pipeline);
			pipeline.removeLast(); // remove the API specific handler
			final var fullRespSync = new SynchronousQueue<FullHttpResponse>();
			pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
			pipeline.addLast(
							new SimpleChannelInboundHandler<HttpObject>() {
								@Override
								protected final void channelRead0(final ChannelHandlerContext ctx, final HttpObject msg)
												throws Exception {
									if (msg instanceof FullHttpResponse) {
										fullRespSync.put(((FullHttpResponse) msg).retain());
									}
								}
							});
			channel.writeAndFlush(request).sync();
			if (null == (resp = fullRespSync.poll(netTimeoutMilliSec, TimeUnit.MILLISECONDS))) {
				Loggers.MSG.warn("{}: Response timeout \n Request: {}", stepId, request);
			}
		} catch (final NoSuchElementException e) {
			throw new ConnectException("Channel pipeline is empty: connectivity related failure");
		} catch (final Exception e) {
			if (e instanceof ClosedChannelException) {
				throw new ConnectException("Connection is closed: " + e.toString());
			} else {
				throw new ConnectException("Connection failure: " + e.toString());
			}
		} finally {
			channel.close();
		}
		return resp;
	}

	@Override
	protected void appendHandlers(final Channel channel) {
		super.appendHandlers(channel);
		channel
						.pipeline()
						.addLast(new HttpClientCodec(REQ_LINE_LEN, HEADERS_LEN, CHUNK_SIZE, true))
						.addLast(new ChunkedWriteHandler());
	}

	protected HttpRequest httpRequest(final O op, final String nodeAddr) throws URISyntaxException {
		final var item = op.item();
		final var opType = op.type();
		final var srcPath = op.srcPath();
		final HttpMethod httpMethod;
		final String uriPath;
		if (item instanceof DataItem) {
			httpMethod = dataHttpMethod(opType);
			uriPath = dataUriPath(item, srcPath, op.dstPath(), opType);
		} else if (item instanceof TokenItem) {
			httpMethod = tokenHttpMethod(opType);
			uriPath = tokenUriPath(item, srcPath, op.dstPath(), opType);
		} else if (item instanceof PathItem) {
			httpMethod = pathHttpMethod(opType);
			uriPath = pathUriPath(item, srcPath, op.dstPath(), opType);
		} else {
			throw new AssertionError("Unsupported item class: " + item.getClass().getName());
		}
		final var uriQuery = uriQuery();
		final var uri = uriQuery == null || uriQuery.isEmpty() ? uriPath : uriPath + uriQuery;
		final var httpHeaders = (HttpHeaders) new DefaultHttpHeaders();
		if (nodeAddr != null) {
			httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		}
		final var httpRequest = (HttpRequest) new DefaultHttpRequest(HTTP_1_1, httpMethod, uri, httpHeaders);
		switch (opType) {
		case CREATE:
			if (srcPath == null || srcPath.isEmpty()) {
				if (item instanceof DataItem) {
					try {
						httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, ((DataItem) item).size());
					} catch (final IOException ignored) {}
				} else {
					httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
				}
			} else {
				applyCopyHeaders(httpHeaders, dataUriPath(item, srcPath, null, opType));
				httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			}
			break;
		case READ:
			httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			if (op instanceof DataOperation) {
				applyRangesHeaders(httpHeaders, (DataOperation) op);
			}
			break;
		case UPDATE:
			final var dataOp = (DataOperation) op;
			httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, dataOp.markedRangesSize());
			applyRangesHeaders(httpHeaders, dataOp);
			break;
		case DELETE:
			httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			break;
		}
		applyMetaDataHeaders(httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);
		applyAuthHeaders(httpHeaders, httpMethod, uriPath, op.credential());
		return httpRequest;
	}

	protected HttpMethod dataHttpMethod(final OpType opType) {
		switch (opType) {
		case READ:
			return HttpMethod.GET;
		case DELETE:
			return HttpMethod.DELETE;
		default:
			return HttpMethod.PUT;
		}
	}

	protected abstract HttpMethod tokenHttpMethod(final OpType opType);

	protected abstract HttpMethod pathHttpMethod(final OpType opType);

	protected String dataUriPath(
					final I item, final String srcPath, final String dstPath, final OpType opType) {
		final String itemPath;
		if (dstPath != null) {
			itemPath = dstPath.startsWith(SLASH) ? dstPath : SLASH + dstPath;
		} else if (srcPath != null) {
			itemPath = srcPath.startsWith(SLASH) ? srcPath : SLASH + srcPath;
		} else {
			itemPath = null;
		}
		final String itemNameRaw = item.name();
		final String itemName = itemNameRaw.startsWith(SLASH) ? itemNameRaw : SLASH + itemNameRaw;
		return (itemPath == null || itemName.startsWith(itemPath)) ? itemName : itemPath + itemName;
	}

	protected abstract String tokenUriPath(
					final I item, final String srcPath, final String dstPath, final OpType opType);

	protected abstract String pathUriPath(
					final I item, final String srcPath, final String dstPath, final OpType opType);

	private static final ThreadLocal<StringBuilder> THR_LOC_RANGES_BUILDER = ThreadLocal.withInitial(StringBuilder::new);

	protected void applyRangesHeaders(final HttpHeaders httpHeaders, final DataOperation dataOp) {
		final long baseItemSize;
		try {
			baseItemSize = dataOp.item().size();
		} catch (final IOException e) {
			throw new AssertionError(e);
		}
		final List<Range> fixedRanges = dataOp.fixedRanges();
		final StringBuilder strb = THR_LOC_RANGES_BUILDER.get();
		strb.setLength(0);
		if (fixedRanges == null || fixedRanges.isEmpty()) {
			final BitSet rangesMaskPair[] = dataOp.markedRangesMaskPair();
			if (rangesMaskPair[0].isEmpty() && rangesMaskPair[1].isEmpty()) {
				return; // do not set the ranges header
			}
			// current layer first
			for (int i = 0; i < rangeCount(baseItemSize); i++) {
				if (rangesMaskPair[0].get(i)) {
					if (strb.length() > 0) {
						strb.append(',');
					}
					strb.append(rangeOffset(i))
									.append('-')
									.append(Math.min(rangeOffset(i + 1), baseItemSize) - 1);
				}
			}
			// then next layer ranges if any
			for (int i = 0; i < rangeCount(baseItemSize); i++) {
				if (rangesMaskPair[1].get(i)) {
					if (strb.length() > 0) {
						strb.append(',');
					}
					strb.append(rangeOffset(i))
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
					final List<Range> ranges, final long baseLength, final StringBuilder dstBuff) {
		Range nextFixedRange;
		long nextRangeSize;
		for (int i = 0; i < ranges.size(); i++) {
			nextFixedRange = ranges.get(i);
			nextRangeSize = nextFixedRange.getSize();
			if (i > 0) {
				dstBuff.append(',');
			}
			if (nextRangeSize == -1) {
				dstBuff.append(nextFixedRange.toString());
			} else {
				dstBuff.append(baseLength).append("-");
			}
		}
	}

	protected void applySharedHeaders(final HttpHeaders httpHeaders) {
		for (final var sharedHeader : sharedHeaders) {
			httpHeaders.add(sharedHeader.getKey(), sharedHeader.getValue());
		}
	}

	protected void applyDynamicHeaders(final HttpHeaders httpHeaders) {
		String headerName;
		String headerValue;
		Input<String> headerNameInput;
		Input<String> headerValueInput;
		for (final var nextHeader : dynamicHeaders.entrySet()) {
			headerName = nextHeader.getKey();
			// header name is a generator pattern
			headerNameInput = headerNameInputs.computeIfAbsent(headerName, EXPR_INPUT_FUNC);
			if (headerNameInput == null) {
				continue;
			}
			// spin while header name generator is not ready
			headerName = headerNameInput.get();
			headerValue = nextHeader.getValue();
			// header value is a generator pattern
			headerValueInput = headerValueInputs.computeIfAbsent(headerValue, EXPR_INPUT_FUNC);
			if (headerValueInput == null) {
				continue;
			}
			// spin while header value generator is not ready
			headerValue = headerValueInput.get();
			// put the generated header value into the request
			httpHeaders.set(headerName, headerValue);
		}
	}

	protected final String uriQuery() {
		return uriQueryInput.get();
	}

	protected abstract void applyMetaDataHeaders(final HttpHeaders httpHeaders);

	protected abstract void applyAuthHeaders(
					final HttpHeaders httpHeaders,
					final HttpMethod httpMethod,
					final String dstUriPath,
					final Credential credential);

	protected abstract void applyCopyHeaders(final HttpHeaders httpHeaders, final String srcPath)
					throws URISyntaxException;

	@Override
	protected final void sendRequest(final Channel channel, final O op) {
		final String nodeAddr = op.nodeAddr();
		try {
			final HttpRequest httpRequest = httpRequest(op, nodeAddr);
			if (channel == null) {
				return;
			} else {
				channel.write(httpRequest).addListener(httpReqSentCallback);
				if (Loggers.MSG.isTraceEnabled()) {
					Loggers.MSG.trace(
									"{} >>>> {} {}", op.hashCode(), httpRequest.method(), httpRequest.uri());
				}
			}
			if (!(httpRequest instanceof FullHttpRequest)) {
				sendRequestData(channel, op);
			}
		} catch (final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to write the data");
		} catch (final URISyntaxException e) {
			LogUtil.exception(Level.WARN, e, "Failed to build the request URI");
		} catch (final Throwable e) {
			if (!isStopped() && !isClosed()) {
				LogUtil.trace(Loggers.ERR, Level.ERROR, e, "Send HTTP request failure");
			}
		}
		channel.write(LastHttpContent.EMPTY_LAST_CONTENT).addListener(reqSentCallback);
		channel.flush();
	}

	void sendHttpRequestComplete(final ChannelFuture future) {
		try {
			future.get(1, TimeUnit.NANOSECONDS);
		} catch (final ExecutionException | TimeoutException e) {
			final var cause = e.getCause();
			LogUtil.trace(Loggers.ERR, Level.WARN, cause, "Failed to send the request");
			final var op = future.channel().attr(ATTR_KEY_OPERATION).get();
			op.status(Status.FAIL_IO);
			complete(future.channel(), (O) op);
		} catch (final InterruptedException e) {
			throw new InterruptRunException(e);
		}
	};

	@Override
	protected void doClose() throws IOException {
		super.doClose();
		try {
			uriQueryInput.close();
		} catch (final Exception ignored) {}
		sharedHeaders.clear();
		dynamicHeaders.clear();
		headerNameInputs
						.values()
						.forEach(
										headerNameInput -> {
											try {
												headerNameInput.close();
											} catch (final Exception ignored) {}
										});
		headerNameInputs.clear();
		headerValueInputs
						.values()
						.forEach(
										headerValueInput -> {
											try {
												headerValueInput.close();
											} catch (final Exception ignored) {}
										});
		headerValueInputs.clear();
	}
}
