package com.emc.mongoose.storage.mock.impl.web.request;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.storage.mock.api.*;
import io.netty.buffer.ByteBuf;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.channel.ChannelHandler.Sharable;
import static com.emc.mongoose.core.api.io.req.WSRequestConfig.VALUE_RANGE_CONCAT;
import static com.emc.mongoose.core.api.io.req.WSRequestConfig.VALUE_RANGE_PREFIX;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.RANGE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.INSUFFICIENT_STORAGE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public abstract class NagainaRequestHandlerBase<T extends WSObjectMock> extends SimpleChannelInboundHandler<Object> {

	private final static Logger LOG = LogManager.getLogger();

	protected final int batchSize;
	private final float rateLimit;
	private final AtomicInteger lastMilliDelay = new AtomicInteger(1);

	protected final WSMock<T> sharedStorage;
	private final StorageIOStats ioStats;

	protected final String requestKey = "requestKey";
	protected final String responseStatusKey = "responseStatusKey";
	protected final String contentLengthKey = "contentLengthKey";
	protected final String ctxWriteFlagKey = "ctxWriteFlagKey";

	public NagainaRequestHandlerBase(RunTimeConfig rtConfig, WSMock<T> sharedStorage) {
		this.rateLimit = rtConfig.getLoadLimitRate();
		this.batchSize = rtConfig.getBatchSize();
		this.sharedStorage = sharedStorage;
		this.ioStats = sharedStorage.getStats();
		AttributeKey.<HttpRequest>valueOf(requestKey);
		AttributeKey.<HttpResponseStatus>valueOf(responseStatusKey);
		AttributeKey.<Long>valueOf(contentLengthKey);
		AttributeKey.<Boolean>valueOf(ctxWriteFlagKey);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	private void processHttpRequest(ChannelHandlerContext ctx, HttpRequest request) {
		ctx.attr(AttributeKey.<HttpRequest>valueOf(requestKey)).set(request);
		if (request.headers().contains(CONTENT_LENGTH)) {
			ctx.attr(AttributeKey.<Long>valueOf(contentLengthKey)).set(Long.parseLong(request.headers().get(CONTENT_LENGTH)));
		}
	}

	private void processHttpContent(ChannelHandlerContext ctx, HttpContent httpContent) {
		ByteBuf content = httpContent.content();
		if (ctx.attr(AttributeKey.<Long>valueOf(contentLengthKey)) == null) {
			Long currentContentSize = ctx.attr(AttributeKey.<Long>valueOf(contentLengthKey)).get();
			ctx.attr(AttributeKey.<Long>valueOf(contentLengthKey)).set(currentContentSize + content.readableBytes());
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof HttpRequest) {
			processHttpRequest(ctx, (HttpRequest) msg);
		}

		if (msg instanceof HttpContent) {
			processHttpContent(ctx, (HttpContent) msg);
		}

		if (msg instanceof LastHttpContent) {
			handle(ctx);
		}
	}

	public final void handle(ChannelHandlerContext ctx) {
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
		handleActually(ctx);
	}

	protected String[] getUriParams(String uri, int maxNumberOfParams) {
		String[] result = new String[maxNumberOfParams];
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
		String[] pathChunks = queryStringDecoder.path().split("/");
		System.arraycopy(pathChunks, 1, result, 0, pathChunks.length - 1);
		return result;
	}

	protected abstract void handleActually(ChannelHandlerContext ctx);

	protected void handleGenericDataReq(String method, String containerName, String objId,
	                                    Long offset, Long size, ChannelHandlerContext ctx) {
		switch (method) {
			case WSRequestConfig.METHOD_POST:
			case WSRequestConfig.METHOD_PUT:
				handleWrite(containerName, objId, offset, size, ctx);
				break;
			case WSRequestConfig.METHOD_GET:
				handleRead(containerName, objId, offset, ctx);
				break;
			case WSRequestConfig.METHOD_HEAD:
				setHttpResponseStatusInContext(ctx, OK);
				break;
			case WSRequestConfig.METHOD_DELETE:
				handleDelete(containerName, objId, offset, ctx);
				break;
		}
	}

	private void handleWrite(String containerName, String objId,
	                         Long offset, Long size, ChannelHandlerContext ctx) {
		List<String> rangeHeadersValues =
				ctx.attr(AttributeKey.<HttpRequest>valueOf(requestKey)).get().headers().getAll(RANGE);
		try {
			if (rangeHeadersValues.size() == 0) {
				sharedStorage.createObject(containerName, objId,
						offset, size);
				ioStats.markWrite(true, size);
			} else {
				ioStats.markWrite(
						handlePartialWrite(containerName, objId, rangeHeadersValues, size),
						size
				);
			}
		} catch (ContainerMockNotFoundException | ObjectMockNotFoundException e) {
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
			ioStats.markWrite(false, size);
		} catch (StorageMockCapacityLimitReachedException | ContainerMockException e) {
			setHttpResponseStatusInContext(ctx, INSUFFICIENT_STORAGE);
			ioStats.markWrite(false, size);
		}
	}

	private boolean handlePartialWrite(String containerName, String objId,
	                                   List<String> rangeHeadersValues, Long size) throws ContainerMockException, ObjectMockNotFoundException {
		for (String rangeValues : rangeHeadersValues) {
			if (rangeValues.startsWith(VALUE_RANGE_PREFIX)) {
				rangeValues = rangeValues.substring(
						VALUE_RANGE_PREFIX.length(), rangeValues.length()
				);
				String[] ranges = rangeValues.split(RunTimeConfig.LIST_SEP);
				for (String range : ranges) {
					String[] rangeBorders = range.split(VALUE_RANGE_CONCAT);
					if (rangeBorders.length == 1) {
						sharedStorage.appendObject(containerName, objId, Long.parseLong(rangeBorders[0]), size);
					} else if (rangeBorders.length == 2) {
						long offset = Long.parseLong(rangeBorders[0]);
						sharedStorage.updateObject(
								containerName, objId, offset, Long.parseLong(rangeBorders[1]) - offset + 1
						);
					} else {
						LOG.warn(
								Markers.ERR, "Invalid range header value: \"{}\"", rangeValues
						);
						return false;
					}

				}
			}
			else {
				LOG.warn(Markers.ERR, "Invalid range header value: \"{}\"", rangeValues);
				return false;
			}
		}
		return true;
	}

	private void handleRead(String containerName, String objId,
	                        Long offset, ChannelHandlerContext ctx) {
		HttpResponse response;
		try {
			T obj = sharedStorage.getObject(containerName, objId, offset, 0);
			if (obj != null) {
				final long objSize = obj.getSize();
				ioStats.markRead(true, objSize);
				if (LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, "Send data object with ID: {}", obj);
				}
				ctx.attr(AttributeKey.<Boolean>valueOf(ctxWriteFlagKey)).set(false);
				response = new DefaultHttpResponse(HTTP_1_1, OK);
				HttpHeaders.setContentLength(response, objSize);
				ctx.write(response);
				if(obj.hasBeenUpdated()) {
					ctx.write(new UpdatedDataItemFileRegion<>(obj));
				} else {
					ctx.write(new DataItemFileRegion<>(obj));
				}
				ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
			} else {
				setHttpResponseStatusInContext(ctx, NOT_FOUND);
				ioStats.markRead(false, 0);
			}
		} catch (ContainerMockException e) {
			setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			LogUtil.exception(LOG, Level.WARN, e, "Container \"{}\" failure", containerName);
			ioStats.markRead(false, 0);
		}
	}

	private void handleDelete(String containerName, String objId,
	                          Long offset, ChannelHandlerContext ctx) {
		try {
			sharedStorage.deleteObject(containerName, objId, offset, -1);
			if (LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Delete data object with ID: {}", objId);
			}
			ioStats.markDelete(true);
		} catch (ContainerMockNotFoundException e) {
			ioStats.markDelete(false);
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
			if (LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.ERR, "No such container: {}", objId);
			}
		}
	}

	protected void handleGenericContainerReq(String method, String containerName, ChannelHandlerContext ctx) {
		switch (method) {
			case WSRequestConfig.METHOD_HEAD:
				handleContainerExists(containerName, ctx);
				break;
			case WSRequestConfig.METHOD_PUT:
				handleContainerCreate(containerName);
				break;
			case WSRequestConfig.METHOD_GET:
				handleContainerList(containerName, ctx);
				break;
			case WSRequestConfig.METHOD_DELETE:
				handleContainerDelete(containerName);
				break;
		}
	}

	protected abstract void handleContainerList(String containerName, ChannelHandlerContext ctx);

	private void handleContainerCreate(String containerName) {
		sharedStorage.createContainer(containerName);
	}

	private void handleContainerExists(String containerName, ChannelHandlerContext ctx) {
		if (sharedStorage.getContainer(containerName) == null) {
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
		}
	}

	private void handleContainerDelete(String containerName) {
		sharedStorage.deleteContainer(containerName);
	}

	protected void setHttpResponseStatusInContext(ChannelHandlerContext ctx, HttpResponseStatus status) {
		ctx.attr(AttributeKey.<HttpResponseStatus>valueOf(responseStatusKey)).set(status);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}
