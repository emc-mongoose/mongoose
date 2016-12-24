package com.emc.mongoose.storage.mock.impl.http.request;

import com.emc.mongoose.storage.driver.net.base.data.DataItemFileRegion;
import com.emc.mongoose.storage.driver.net.base.data.UpdatedFullDataFileRegion;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageIoStats;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.exception.ObjectMockNotFoundException;
import com.emc.mongoose.storage.mock.api.exception.StorageMockCapacityLimitReachedException;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.emc.mongoose.ui.config.Config.ItemConfig.NamingConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.RANGE;
import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.HEAD;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INSUFFICIENT_STORAGE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 Created on 12.07.16.
 */
@SuppressWarnings("WeakerAccess")
@Sharable
public abstract class RequestHandlerBase<T extends MutableDataItemMock>
extends ChannelInboundHandlerAdapter {

	private static final Logger LOG = LogManager.getLogger();

	private final double rateLimit;
	private final AtomicInteger lastMilliDelay = new AtomicInteger(1);

	private final StorageMockClient<T> remoteStorage;
	protected final StorageMock<T> localStorage;
	private final StorageIoStats ioStats;
	private final String apiClsName;

	private final int prefixLength, idRadix;

	protected static final AttributeKey<HttpRequest> ATTR_KEY_REQUEST = AttributeKey
		.newInstance("requestKey");
	protected static final AttributeKey<HttpResponseStatus> ATTR_KEY_RESPONSE_STATUS = AttributeKey
		.newInstance("responseStatusKey");
	protected static final AttributeKey<Long> ATTR_KEY_CONTENT_LENGTH = AttributeKey
		.newInstance("contentLengthKey");
	protected static final AttributeKey<Boolean> ATTR_KEY_CTX_WRITE_FLAG = AttributeKey
		.newInstance("ctxWriteFlagKey");
	protected static final AttributeKey<String> ATTR_KEY_HANDLER = AttributeKey
		.newInstance("handlerKey");

	protected static final int DEFAULT_PAGE_SIZE = 0x1000;
	protected static final String MARKER_KEY = "marker";

	protected RequestHandlerBase(
		final LimitConfig limitConfig, final NamingConfig namingConfig,
		final StorageMock<T> localStorage, final StorageMockClient<T> remoteStorage
	) throws RemoteException {
		this.rateLimit = limitConfig.getRate();
		final String t = namingConfig.getPrefix();
		this.prefixLength = t == null ? 0 : t.length();
		this.idRadix = namingConfig.getRadix();
		this.remoteStorage = remoteStorage;
		this.localStorage = localStorage;
		this.ioStats = localStorage.getStats();
		this.apiClsName = getClass().getSimpleName();
	}

	protected boolean checkApiMatch(final HttpRequest request) {
		return true;
	}

	@Override
	public void channelReadComplete(final ChannelHandlerContext ctx)
	throws Exception {
		final String ctxName = ctx.channel().attr(ATTR_KEY_HANDLER).get();
		if(!apiClsName.equals(ctxName)) {
			ctx.fireChannelReadComplete();
			return;
		}
		ctx.flush();
	}

	private void processHttpRequest(final ChannelHandlerContext ctx, final HttpRequest request) {
		final Channel channel = ctx.channel();
		final HttpHeaders headers = request.headers();
		channel.attr(ATTR_KEY_REQUEST).set(request);
		if(headers.contains(CONTENT_LENGTH)) {
			channel
				.attr(ATTR_KEY_CONTENT_LENGTH)
				.set(Long.parseLong(headers.get(CONTENT_LENGTH)));
		}
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
		final Channel channel = ctx.channel();
		if(msg instanceof HttpRequest) {
			if(!checkApiMatch((HttpRequest) msg)) {
				channel.attr(ATTR_KEY_HANDLER).set("");
				ctx.fireChannelRead(msg);
				return;
			}
			channel.attr(ATTR_KEY_HANDLER).set(apiClsName);
			processHttpRequest(ctx, (HttpRequest) msg);
			ReferenceCountUtil.release(msg);
			return;
		}
		if(!channel.attr(ATTR_KEY_HANDLER).get().equals(apiClsName)) {
			ctx.fireChannelRead(msg);
			return;
		}
		if(msg instanceof LastHttpContent) {
			handle(ctx);
		}
		ReferenceCountUtil.release(msg);
	}

	private void handle(final ChannelHandlerContext ctx) {

		if(rateLimit > 0) {
			if(ioStats.getWriteRate() + ioStats.getReadRate() + ioStats.getDeleteRate() > rateLimit) {
				try {
					Thread.sleep(lastMilliDelay.incrementAndGet());
				} catch (InterruptedException e) {
					return;
				}
			} else if(lastMilliDelay.get() > 0) {
				lastMilliDelay.decrementAndGet();
			}
		}
		final Channel channel = ctx.channel();

		if(localStorage.dropConnection()) {
			LOG.warn(Markers.MSG, "Dropped the connection \"{}\"", channel);
			channel.close();
			return;
		}

		final String uri = channel.attr(ATTR_KEY_REQUEST).get().uri();
		final HttpMethod method = channel.attr(ATTR_KEY_REQUEST).get().method();
		final long size;
		if(channel.hasAttr(ATTR_KEY_CONTENT_LENGTH)) {
			size = channel.attr(ATTR_KEY_CONTENT_LENGTH).get();
		} else {
			size = 0;
		}
		setHttpResponseStatusInContext(ctx, OK); // OK response assumption
		final int uriPathEnd = uri.indexOf('?', 0);
		final String uriPath;
		final Map<String, String> queryParams;
		if(uriPathEnd > 0) {
			uriPath = uri.substring(0, uriPathEnd);
			final String queryParamsStr = uri.substring(uriPathEnd + 1);
			final String queryParamPairs[] = queryParamsStr.split("&");
			queryParams = new HashMap<>();
			String nextKeyValuePair[];
			for(final String queryParamPair : queryParamPairs) {
				nextKeyValuePair = queryParamPair.split("=");
				if(nextKeyValuePair.length == 2) {
					queryParams.put(nextKeyValuePair[0], nextKeyValuePair[1]);
				} else if(nextKeyValuePair.length == 1){
					queryParams.put(nextKeyValuePair[0], "");
				}
			}
		} else {
			uriPath = uri;
			queryParams = null;
		}

		doHandle(uriPath, queryParams, method, size, ctx);
	}

	protected final void handleItemRequest(
		final HttpMethod method, final Map<String, String> queryParams,
		final String containerName, final String objectId, final long size,
		final ChannelHandlerContext ctx
	) {
		if(objectId != null) {
			final long offset;
			if(method.equals(POST) || method.equals(PUT)) {
				if(prefixLength > 0) {
					offset = Long.parseLong(objectId.substring(prefixLength + 1), idRadix);
				} else {
					offset = Long.parseLong(objectId, idRadix);
				}
			} else {
				offset = -1;
			}
			handleObjectRequest(method, containerName, objectId, offset, size, ctx);
		} else {
			handleContainerRequest(method, containerName, queryParams, ctx);
		}
	}

	protected abstract void doHandle(
		final String uriPath, final Map<String, String> queryParams, final HttpMethod method,
		final long size, final ChannelHandlerContext ctx
	);

	protected FullHttpResponse newEmptyResponse(final HttpResponseStatus status) {
		final DefaultFullHttpResponse
			response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.EMPTY_BUFFER, false);
		HttpUtil.setContentLength(response, 0);
		return response;
	}

	protected FullHttpResponse newEmptyResponse() {
		return newEmptyResponse(OK);
	}

	protected final void writeResponse(
		final ChannelHandlerContext ctx, final FullHttpResponse response
	) {
		final HttpResponseStatus status = ctx.channel().attr(ATTR_KEY_RESPONSE_STATUS).get();
		response.setStatus(status);
		ctx.write(response);
	}

	protected final void writeEmptyResponse(final ChannelHandlerContext ctx) {
		final HttpResponseStatus status = ctx.channel().attr(ATTR_KEY_RESPONSE_STATUS).get();
		final FullHttpResponse response = newEmptyResponse(status);
		ctx.write(response);
	}

	protected final void handleObjectRequest(
		final HttpMethod httpMethod, final String containerName, final String id, final long offset,
		final long size, final ChannelHandlerContext ctx
	) {
		if(containerName != null) {
			if(httpMethod.equals(POST) || httpMethod.equals(PUT)) {
				handleObjectWrite(containerName, id, offset, size, ctx);
			} else if(httpMethod.equals(GET)) {
				handleObjectRead(containerName, id, offset, ctx);
			} else if(httpMethod.equals(HEAD)) {
//				setHttpResponseStatusInContext(ctx, OK); by default
			} else if(httpMethod.equals(DELETE)) {
				handleObjectDelete(containerName, id, offset, ctx);
			}
		} else {
			setHttpResponseStatusInContext(ctx, BAD_REQUEST);
		}
	}

	private void handleObjectWrite(
		final String containerName, final String id, final long offset, final long size,
		final ChannelHandlerContext ctx
	) {
		final List<String> rangeHeadersValues = ctx
			.channel()
			.attr(ATTR_KEY_REQUEST)
			.get()
			.headers()
			.getAll(RANGE);
		try {
			if(rangeHeadersValues.size() == 0) {
				localStorage.createObject(containerName, id, offset, size);
				ioStats.markWrite(true, size);
			} else {
				final boolean success = handlePartialWrite(
					containerName, id, rangeHeadersValues, size
				);
				ioStats.markWrite(success, size);
			}
		} catch (final StorageMockCapacityLimitReachedException e) {
			setHttpResponseStatusInContext(ctx, INSUFFICIENT_STORAGE);
			ioStats.markWrite(false, size);
		} catch (final ContainerMockNotFoundException e) {
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
			ioStats.markWrite(false, size);
		} catch (final ObjectMockNotFoundException e) {
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
			ioStats.markWrite(false, 0);
		} catch (final ContainerMockException | NumberFormatException | IllegalStateException e) {
			setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			ioStats.markWrite(false, 0);
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to perform a range update/append for \"{}\"", id
			);
		} catch(final IllegalArgumentException e) {
			setHttpResponseStatusInContext(ctx, BAD_REQUEST);
			ioStats.markWrite(false, 0);
		}
	}

	private static final String VALUE_RANGE_PREFIX = "bytes=";
	private static final String VALUE_RANGE_CONCAT = "-";

	private boolean handlePartialWrite(
		final String containerName, final String id, final List<String> rangeHeadersValues,
		final long size
	) throws ContainerMockException, ObjectMockNotFoundException, NumberFormatException {
		String ranges[];
		String rangeBorders[];
		int rangeBordersNum;
		long start;
		long end;
		for(final String rangeValues: rangeHeadersValues) {
			if(rangeValues.startsWith(VALUE_RANGE_PREFIX)) {
				final String rangeValueWithoutPrefix = rangeValues.substring(
					VALUE_RANGE_PREFIX.length(), rangeValues.length()
				);
				ranges = rangeValueWithoutPrefix.split(",");
				for(final String range : ranges) {
					rangeBorders = range.split(VALUE_RANGE_CONCAT);
					rangeBordersNum = rangeBorders.length;
					if(rangeBordersNum == 2) {
						if(rangeBorders[0].isEmpty()) {
							end = Long.parseLong(rangeBorders[1]);
							localStorage.appendObject(containerName, id, end);
						} else if(rangeBorders[1].isEmpty()) {
							start = Long.parseLong(rangeBorders[0]);
							localStorage.updateObject(containerName, id, start, size - start);
						} else {
							start = Long.parseLong(rangeBorders[0]);
							end = Long.parseLong(rangeBorders[1]);
							localStorage.updateObject(containerName, id, start, end - start + 1);
						}
					} else {
						LOG.warn(Markers.ERR, "Invalid range header value: \"{}\"", rangeValues);
						return false;
					}
				}
			} else {
				LOG.warn(Markers.ERR, "Invalid range header value: \"{}\"", rangeValues);
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private void handleObjectRead(
		final String containerName, final String id, final long offset,
		final ChannelHandlerContext ctx
	) {
		try {
			T object = localStorage.getObject(containerName, id, offset, 0);
			if(object != null) {
				handleObjectReadSuccess(object, ctx);
			} else {
				if(remoteStorage != null) {
					object = remoteStorage.getObject(containerName, id, offset, 0);
				}
				if(object == null) {
					setHttpResponseStatusInContext(ctx, NOT_FOUND);
					if(LOG.isTraceEnabled(Markers.ERR)) {
						LOG.trace(Markers.ERR, "No such container: {}", id);
					}
					ioStats.markRead(false, 0);
				} else {
					handleObjectReadSuccess(object, ctx);
				}
			}
		} catch(final ContainerMockNotFoundException e) {
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
			ioStats.markRead(false, 0);
		} catch(final IOException | ContainerMockException  e) {
			setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			LogUtil.exception(LOG, Level.WARN, e, "Container \"{}\" failure", containerName);
			ioStats.markRead(false, 0);
		} catch(final InterruptedException | ExecutionException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Remote call failure", containerName);
		}
	}

	private void handleObjectReadSuccess(final T object, final ChannelHandlerContext ctx)
	throws IOException {
		final long size = object.size();
		ioStats.markRead(true, size);
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Send data object with ID {}", object.getName());
		}
		if(localStorage.missResponse()) {
			return;
		}
		ctx.channel().attr(ATTR_KEY_CTX_WRITE_FLAG).set(false);
		final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, OK);
		HttpUtil.setContentLength(response, size);
		ctx.write(response);
		if(object.isUpdated()) {
			ctx.write(new UpdatedFullDataFileRegion<>(object));
		} else {
			ctx.write(new DataItemFileRegion<>(object));
		}
		ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
	}

	private void handleObjectDelete(
		final String containerName, final String id, final long offset,
		final ChannelHandlerContext ctx
	) {
		try {
			localStorage.deleteObject(containerName, id, offset, -1);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Delete data object with ID: {}", id);
			}
			ioStats.markDelete(true);
		} catch (final ContainerMockNotFoundException e) {
			ioStats.markDelete(false);
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.ERR, "No such container: {}", id);
			}
		}
	}

	protected void setHttpResponseStatusInContext(
		final ChannelHandlerContext ctx, final HttpResponseStatus status
	) {
		ctx.channel().attr(ATTR_KEY_RESPONSE_STATUS).set(status);
	}

	protected void handleContainerRequest(
		final HttpMethod method, final String name, final Map<String, String> queryParams,
		final ChannelHandlerContext ctx
	) {
		if(method.equals(PUT)) {
			handleContainerCreate(name);
		} else if(method.equals(GET)) {
			handleContainerList(name, queryParams, ctx);
		} else if(method.equals(HEAD)) {
			handleContainerExist(name, ctx);
		} else if(method.equals(DELETE)) {
			handleContainerDelete(name);
		}
	}

	protected void handleContainerCreate(final String name) {
		localStorage.createContainer(name);
	}

	protected abstract void handleContainerList(
		final String name, final Map<String, String> queryParams, final ChannelHandlerContext ctx
	);

	protected final T listContainer(
		final String name, final String marker, final List<T> buffer, final int maxCount
	)
	throws ContainerMockException {
		final T lastObject = localStorage.listObjects(name, marker, buffer, maxCount);
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "Container \"{}\": generated list of {} objects, last one is \"{}\"",
				name, buffer.size(), lastObject
			);
		}
		return lastObject;
	}

	private void handleContainerExist(final String name, final ChannelHandlerContext ctx) {
		if(localStorage.getContainer(name) == null) {
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
		}
	}

	private void handleContainerDelete(final String name) {
		localStorage.deleteContainer(name);
	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
	throws Exception {
		LogUtil.exception(LOG, Level.DEBUG, cause, "Handler was interrupted");
		ctx.close();
	}
	
	protected static String generateHexId(final int byteCount) {
		final byte buff[] = new byte[byteCount];
		ThreadLocalRandom.current().nextBytes(buff);
		return Hex.encodeHexString(buff);
	}
	
	protected static String generateBase64Id(final int byteCount) {
		final byte buff[] = new byte[byteCount];
		ThreadLocalRandom.current().nextBytes(buff);
		return Base64.encodeBase64URLSafeString(buff);
	}
}
