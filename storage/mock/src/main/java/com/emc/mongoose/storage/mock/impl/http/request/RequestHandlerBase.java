package com.emc.mongoose.storage.mock.impl.http.request;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageIoStats;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.exception.ObjectMockNotFoundException;
import com.emc.mongoose.storage.mock.api.exception.StorageMockCapacityLimitReachedException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.RANGE;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 Created on 12.07.16.
 */
@Sharable
public abstract class RequestHandlerBase<T extends MutableDataItemMock>
extends ChannelInboundHandlerAdapter {

	private final static Logger LOG = LogManager.getLogger();

	private final double rateLimit;
	private final AtomicInteger lastMilliDelay = new AtomicInteger(1);

	protected final StorageMock<T> sharedStorage;
	private final StorageIoStats ioStats;
	private final ContentSource contentSource;

	protected final String requestKey = "requestKey";
	protected final String responseStatusKey = "responseStatusKey";
	protected final String contentLengthKey = "contentLengthKey";
	protected final String ctxWriteFlagKey = "ctxWriteFlagKey";
	protected final String handlerStatus = "handlerStatus";

	public RequestHandlerBase(
			final Config.LoadConfig.LimitConfig limitConfig,
			final StorageMock<T> sharedStorage,
			final ContentSource contentSource
			) {
		this.rateLimit = limitConfig.getRate();
		this.sharedStorage = sharedStorage;
		this.contentSource = contentSource;
		this.ioStats = sharedStorage.getStats();
		AttributeKey.<HttpRequest>valueOf(requestKey);
		AttributeKey.<HttpResponseStatus>valueOf(responseStatusKey);
		AttributeKey.<Long>valueOf(contentLengthKey);
		AttributeKey.<Boolean>valueOf(ctxWriteFlagKey);
		AttributeKey.<Boolean>valueOf(handlerStatus);
	}

	protected abstract boolean checkApiMatch(final HttpRequest request);

	@Override
	public void channelReadComplete(final ChannelHandlerContext ctx)
	throws Exception {
		if (!ctx.channel().attr(AttributeKey.<Boolean>valueOf(handlerStatus)).get()) {
			ctx.fireChannelReadComplete();
			return;
		}
		ctx.flush();
	}

	private void processHttpRequest(final ChannelHandlerContext ctx, final HttpRequest request) {
		final Channel channel = ctx.channel();
		final HttpHeaders headers = request.headers();
		channel.attr(AttributeKey.<HttpRequest>valueOf(requestKey)).set(request);
		if (headers.contains(CONTENT_LENGTH)) {
			channel.attr(AttributeKey.<Long>valueOf(contentLengthKey)).set(
				Long.parseLong(headers.get(CONTENT_LENGTH)));
		}
	}

	private void processHttpContent(final ChannelHandlerContext ctx, final Object msg) {
		final Channel channel = ctx.channel();
		if (msg instanceof HttpRequest) {
			if (!checkApiMatch((HttpRequest) msg)) {
				channel.attr(AttributeKey.<Boolean>valueOf(handlerStatus)).set(false);
				ctx.fireChannelRead(msg);
				return;
			}
			channel.attr(AttributeKey.<Boolean>valueOf(handlerStatus)).set(true);
			processHttpRequest(ctx, (HttpRequest) msg);
			ReferenceCountUtil.release(msg);
			return;
		}
		if (!channel.attr(AttributeKey.<Boolean>valueOf(handlerStatus)).get()) {
			ctx.fireChannelRead(msg);
			return;
		}
		if (msg instanceof LastHttpContent) {
			handle(ctx);
		}
		ReferenceCountUtil.release(msg);
	}

	public final void handle(final ChannelHandlerContext ctx) {
		if (rateLimit > 0) {
			if (ioStats.getWriteRate() + ioStats.getReadRate() + ioStats.getDeleteRate() > rateLimit) {
				try {
					Thread.sleep(lastMilliDelay.incrementAndGet());
				} catch (InterruptedException e) {
					return;
				}
			} else if (lastMilliDelay.get() > 0) {
				lastMilliDelay.decrementAndGet();
			}
		}
		doHandle(ctx);
	}

	protected abstract void doHandle(final ChannelHandlerContext ctx);

	protected final String[] getUriParameters(final String uri, final int maxNumOfParams) {
		final String[] result = new String[maxNumOfParams];
		final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
		final String[] queryStringChunks = queryStringDecoder.path().split("/");
		System.arraycopy(queryStringChunks, 1, result, 0, queryStringChunks.length - 1);
		return result;
	}

	protected final void writeResponse(final ChannelHandlerContext ctx) {
		final HttpResponseStatus status = ctx.channel()
				.attr(AttributeKey.<HttpResponseStatus>valueOf(responseStatusKey)).get();
		final DefaultFullHttpResponse response =
				new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
		HttpUtil.setContentLength(response, 0);
		ctx.write(response);
	}

	protected final void handleObjectRequest(
			final HttpMethod httpMethod, final String containerName, final String id,
			final Long offset, final Long size, final ChannelHandlerContext ctx) {
		if (containerName != null) {
			if (httpMethod.equals(POST) || httpMethod.equals(PUT)) {
				handleObjectCreate(containerName, id, offset, size, ctx);
			} else if (httpMethod.equals(GET)) {
				handleObjectRead(containerName, id, offset, ctx);
			} else if (httpMethod.equals(HEAD)) {
				setHttpResponseStatusInContext(ctx, OK);
			} else if (httpMethod.equals(DELETE)) {
				handleObjectDelete(containerName, id, offset, ctx);
			}
		} else {
			setHttpResponseStatusInContext(ctx, BAD_REQUEST);
		}
	}

	private void handleObjectCreate(
			final String containerName, final String id, final Long offset,
			final Long size, final ChannelHandlerContext ctx) {
		final List<String> rangeHeadersValues = ctx.channel()
				.attr(AttributeKey.<HttpRequest>valueOf(requestKey)).get()
				.headers().getAll(RANGE);
		try {
			if (rangeHeadersValues.size() == 0) {
				sharedStorage.createObject(containerName, id, offset, size);
				ioStats.markWrite(true, size);
			} else {
				final boolean success = handlePartialCreate(containerName, id,
						rangeHeadersValues,
						size);
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
		} catch (final ContainerMockException | NumberFormatException e) {
			setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			ioStats.markWrite(false, 0);
			LogUtil.exception(
					LOG, Level.ERROR, e,
					"Failed to perform a range update/append for \"{}\"", id
			);
		}
	}

	private static final String VALUE_RANGE_PREFIX = "bytes=";
	private static final String VALUE_RANGE_CONCAT = "-";

	private boolean handlePartialCreate(
			final String containerName, final String id,
			final List<String> rangeHeadersValues, final Long size)
	throws ContainerMockException, ObjectMockNotFoundException {
		for (final String rangeValues: rangeHeadersValues) {
			if (rangeValues.startsWith(VALUE_RANGE_PREFIX)) {
				final String rangeValueWithoutPrefix =
						rangeValues.substring(VALUE_RANGE_PREFIX.length(), rangeValues.length());
				final String[] ranges = rangeValueWithoutPrefix.split(",");
				for (final String range: ranges) {
					final String[] rangeBorders = range.split(VALUE_RANGE_CONCAT);
					final int rangeBordersNum = rangeBorders.length;
					final long offset = Long.parseLong(rangeBorders[0]);
					if (rangeBordersNum == 1) {
						sharedStorage.appendObject(containerName, id,
								offset, size);
					} else if (rangeBordersNum == 2) {
						final long dynSize = Long.parseLong(rangeBorders[1]) - offset + 1;
						sharedStorage.updateObject(containerName, id, offset, dynSize);
					} else {
						LOG.warn(
								Markers.ERR, "Invalid range header value: \"{}\"", rangeValues
						);
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

	private void handleObjectRead(
			final String containerName, final String id,
			final Long offset, final ChannelHandlerContext ctx) {
		final HttpResponse response;
		try {
			final T object = sharedStorage.getObject(containerName, id, offset, 0);
			if (object != null) {
				final long size = object.getSize();
				ioStats.markRead(true, size);
				if (LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, "Send data object with ID {}", id);
					ctx.channel().attr(AttributeKey.<Boolean>valueOf(ctxWriteFlagKey)).set(false);
					response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, OK);
					HttpUtil.setContentLength(response, size);
					ctx.write(response);
					if (object.hasBeenUpdated()) {
						ctx.write(new UpdatedDataItemFileRegion<>(object, contentSource));
					} else {
						ctx.write(new DataItemFileRegion<>(object));
					}
					ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
				}
			}
		} catch (final ContainerMockNotFoundException e) {

			setHttpResponseStatusInContext(ctx, NOT_FOUND);
			if(LOG.isTraceEnabled(Markers.ERR)) {
				LOG.trace(Markers.ERR, "No such container: {}", id);
			}
			ioStats.markRead(false, 0);
		} catch (final ContainerMockException e) {
			setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			LogUtil.exception(LOG, Level.WARN, e, "Container \"{}\" failure", containerName);
			ioStats.markRead(false, 0);
		}
	}

	private void handleObjectDelete(
			final String containerName, final String id,
			final Long offset, final ChannelHandlerContext ctx) {
		try {
			sharedStorage.deleteObject(containerName, id, offset, -1);
			if (LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Delete data object with ID: {}", id);
			}
			ioStats.markDelete(true);
		} catch (final ContainerMockNotFoundException e) {
			ioStats.markDelete(false);
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
			if (LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.ERR, "No such container: {}", id);
			}
		}
	}

	private void setHttpResponseStatusInContext(
			final ChannelHandlerContext ctx,
			final HttpResponseStatus status) {
		ctx.channel()
				.attr(AttributeKey.<HttpResponseStatus>valueOf(responseStatusKey))
				.set(status);
	}

	protected void handleContainerRequest(
			final HttpMethod httpMethod, final String name,
			final ChannelHandlerContext ctx) {
		if (httpMethod.equals(PUT)) {
			handleContainerCreate(name);
		} else if (httpMethod.equals(GET)) {
			handleContainerList(name, ctx);
		} else if (httpMethod.equals(HEAD)) {
			handleContainerExist(name, ctx);
		} else if (httpMethod.equals(DELETE)) {
			handleContainerDelete(name);
		}
	}

	private void handleContainerCreate(final String name) {
		sharedStorage.createContainer(name);
	}

	protected abstract void handleContainerList(final String name, final ChannelHandlerContext ctx);

	protected final T listContainer(
			final String name, final String marker,
			final List<T> buffer, final int maxCount) {
		try {
			final T lastObject = sharedStorage.listObjects(name, marker, buffer, maxCount);
			if (LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
						Markers.MSG, "Container \"{}\": generated list of {} objects, last one is \"{}\"",
						name, buffer.size(), lastObject
				);
			}
			return lastObject;
		} catch (final ContainerMockException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Container \"{}\" failure", name);
		}
		return null;
	}

	private void handleContainerExist(final String name, final ChannelHandlerContext ctx) {
		if (sharedStorage.getContainer(name) == null) {
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
		}
	}

	private void handleContainerDelete(final String name) {
		sharedStorage.deleteContainer(name);
	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
	throws Exception {
		LogUtil.exception(LOG, Level.DEBUG, cause, "Handler was interrupted");
		ctx.close();
	}
}
