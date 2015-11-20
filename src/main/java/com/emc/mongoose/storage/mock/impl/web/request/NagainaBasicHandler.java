package com.emc.mongoose.storage.mock.impl.web.request;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.data.MutableDataItem;
import com.emc.mongoose.core.api.data.content.ContentSource;
import com.emc.mongoose.core.api.data.model.DataItemContainer;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;
import com.emc.mongoose.storage.mock.api.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.channel.ChannelHandler.*;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * Created by Ilia on 30.10.2015.
 */
@Sharable
public class NagainaBasicHandler<T extends WSObjectMock> extends SimpleChannelInboundHandler<Object> {

	private final static Logger LOG = LogManager.getLogger();

	protected final int batchSize;
	private final float rateLimit;
	private final AtomicInteger lastMilliDelay = new AtomicInteger(1);
	private final ContentSource contentSrc = ContentSourceBase.getDefault();

	protected final WSMock<T> sharedStorage;
	private final StorageIOStats ioStats;

	public NagainaBasicHandler(RunTimeConfig rtConfig, WSMock<T> sharedStorage) {
		this.rateLimit = rtConfig.getLoadLimitRate();
		this.batchSize = rtConfig.getBatchSize();
		this.sharedStorage = sharedStorage;
		this.ioStats = sharedStorage.getStats();
	}

	private AttributeKey<HttpRequest> currentHttpRequestKey = AttributeKey.valueOf("currentHttpRequestKey");
	private AttributeKey<FullHttpResponse> currentHttpResponseKey = AttributeKey.valueOf("currentHttpResponseKey");
	private AttributeKey<String> containerNameKey = AttributeKey.valueOf("containerNameKey");
	private AttributeKey<String> objIdKey = AttributeKey.valueOf("objIdKey");
	private AttributeKey<Long> contentLengthKey = AttributeKey.valueOf("contentLengthKey");

	private final static DocumentBuilder DOM_BUILDER;
	private final static TransformerFactory TF = TransformerFactory.newInstance();
	static {
		try {
			DOM_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch(final ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof HttpRequest) {
			HttpRequest request = (HttpRequest) msg;
			ctx.attr(currentHttpRequestKey).set(request);
			if (request.headers().contains(CONTENT_LENGTH)) {
				ctx.attr(contentLengthKey).set(Long.parseLong(request.headers().get(CONTENT_LENGTH)));
			}
			QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
			String[] pathChunks = queryStringDecoder.path().split("/");
			if (pathChunks.length == 2) {
				ctx.attr(containerNameKey).set(pathChunks[1]);
			} else if (pathChunks.length >= 3) {
				ctx.attr(containerNameKey).set(pathChunks[1]);
				ctx.attr(objIdKey).set(pathChunks[2]);
			}
		}

		if (msg instanceof HttpContent) {
			HttpContent httpContent = (HttpContent) msg;
			ByteBuf content = httpContent.content();
			if (ctx.attr(contentLengthKey) == null) {
				Long currentContentSize = ctx.attr(contentLengthKey).get();
				ctx.attr(contentLengthKey).set(currentContentSize + content.readableBytes());
			}
		}

		if (msg instanceof LastHttpContent) {
			LastHttpContent trailer = (LastHttpContent) msg;
			if (!handle(trailer, ctx)) {
				ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
			}
		}
	}

	public final boolean handle(HttpObject currentObj, ChannelHandlerContext ctx) {
		if (rateLimit > 0) {
			if (ioStats.getWriteRate() + ioStats.getReadRate() + ioStats.getDeleteRate() > rateLimit) {
				try {
					Thread.sleep(lastMilliDelay.incrementAndGet());
				} catch (InterruptedException e) {
					return false;
				}
			} else if (lastMilliDelay.get() > 0) {
				lastMilliDelay.decrementAndGet();
			}
		}
		handleActually(ctx);
		return true;
	}

	public void handleActually(ChannelHandlerContext ctx) {
		HttpRequest request = ctx.attr(currentHttpRequestKey).get();
		String method = request.getMethod().toString().toUpperCase();
		String containerName = ctx.attr(containerNameKey).get();
		String objId = ctx.attr(objIdKey).get();
		Long size = ctx.attr(contentLengthKey).get();
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
		response.headers().set(CONTENT_LENGTH, 0);
		ctx.attr(currentHttpResponseKey).set(response);
		if (containerName != null) {
			if (objId != null) {
				long offset;
				switch (method) {
					case WSRequestConfig.METHOD_POST:
					case WSRequestConfig.METHOD_PUT:
						offset = Long.parseLong(objId, MutableDataItem.ID_RADIX);
						break;
					default:
						offset = -1;
				}
				handleGenericDataReq(method, containerName, objId, offset, size, ctx);
			} else {
				handleGenericContainerReq(method, containerName, ctx);
			}
		} else {
			changeHttpResponseStatusInContext(ctx, BAD_REQUEST);
		}
		ctx.write(ctx.attr(currentHttpResponseKey).get());
	}

	protected void handleGenericContainerReq(String method, String containerName, ChannelHandlerContext ctx) {
		switch (method) {
			case WSRequestConfig.METHOD_POST:
				break;
			case WSRequestConfig.METHOD_HEAD:
				handleContainerExists(containerName, ctx);
			case WSRequestConfig.METHOD_PUT:
				handleContainerCreate(containerName);
				break;
			case WSRequestConfig.METHOD_GET:
				handleContainerList(containerName, ctx);
				break;
		}
	}

	private void handleContainerExists(String containerName, ChannelHandlerContext ctx) {
		if (sharedStorage.getContainer(containerName) == null) {
			changeHttpResponseStatusInContext(ctx, NOT_FOUND);
		}
	}

	private void handleContainerCreate(String containerName) {
		sharedStorage.createContainer(containerName);
	}

	protected void handleContainerList(String containerName, ChannelHandlerContext ctx) {
		FullHttpResponse response;
		int maxCount = DataItemContainer.DEFAULT_PAGE_SIZE;
		String marker = null;
		String uri = ctx.attr(currentHttpRequestKey).get().getUri();
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
		if (queryStringDecoder.parameters().containsKey("max-keys")) {
			maxCount = Integer.parseInt(queryStringDecoder.parameters().get("max-keys").get(0));
		}
		else {
			LOG.warn(Markers.ERR, "Failed to parse max keys argument value in the URI: " + uri);
		}
		if (queryStringDecoder.parameters().containsKey("marker")) {
			marker = queryStringDecoder.parameters().get("marker").get(0);
		}
		List<T> buff = new ArrayList<>(maxCount);
		T lastObj;
		try {
			lastObj = sharedStorage.listObjects(containerName, marker, buff, maxCount);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
						Markers.MSG, "Bucket \"{}\": generated list of {} objects, last one is \"{}\"",
						containerName, buff.size(), lastObj
				);
			}
		} catch (ContainerMockNotFoundException e) {
			changeHttpResponseStatusInContext(ctx, NOT_FOUND);
			return;
		} catch (ContainerMockException e) {
			changeHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			return;
		}
		// todo check this line
		response = ctx.attr(currentHttpResponseKey).get();
		response.headers().set(CONTENT_TYPE, ContentType.APPLICATION_XML.getMimeType());
		Document doc = DOM_BUILDER.newDocument();
		Element eRoot = doc.createElementNS(
				"http://s3.amazonaws.com/doc/2006-03-01/", "ListBucketResult"
		);
		doc.appendChild(eRoot);
		//
		Element e = doc.createElement("Name"), ee;
		e.appendChild(doc.createTextNode(containerName));
		eRoot.appendChild(e);
		e = doc.createElement("IsTruncated");
		e.appendChild(doc.createTextNode(Boolean.toString(lastObj != null)));
		eRoot.appendChild(e);
		e = doc.createElement("Prefix"); // TODO prefix support
		eRoot.appendChild(e);
		e = doc.createElement("MaxKeys");
		e.appendChild(doc.createTextNode(Integer.toString(buff.size())));
		eRoot.appendChild(e);
		for(T dataObject : buff) {
			e = doc.createElement("Contents");
			ee = doc.createElement("Key");
			ee.appendChild(doc.createTextNode(dataObject.getName()));
			e.appendChild(ee);
			ee = doc.createElement("Size");
			ee.appendChild(doc.createTextNode(Long.toString(dataObject.getSize())));
			e.appendChild(ee);
			eRoot.appendChild(e);
		}
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final StreamResult r = new StreamResult(bos);
		try {
			TF.newTransformer().transform(new DOMSource(doc), r);
		} catch(TransformerException ex) {
			changeHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			LogUtil.exception(LOG, Level.ERROR, ex, "Failed to build bucket XML listing");
			return;
		}
		// todo know what does it mean
		response.content().setBytes(0, bos.toByteArray());
		ctx.attr(currentHttpResponseKey).set(response);
	}

	protected void handleGenericDataReq(String method, String containerName, String objId,
	                                    Long offset, Long size, ChannelHandlerContext ctx) {
		switch (method) {
			case WSRequestConfig.METHOD_POST:
				handleWrite(containerName, objId, offset, size, ctx);
				break;
			case WSRequestConfig.METHOD_PUT:
				handleWrite(containerName, objId, offset, size, ctx);
				break;
			case WSRequestConfig.METHOD_GET:
				handleRead(containerName, objId, offset, ctx);
				break;
			case WSRequestConfig.METHOD_HEAD:
				changeHttpResponseStatusInContext(ctx, OK);
				break;
		}
	}

	private void handleWrite(String containerName, String objId,
	                                 Long offset, Long size, ChannelHandlerContext ctx) {
		// TODO check usage of RANGE header
		List<String> rangeHeaders = ctx.attr(currentHttpRequestKey).get().headers().getAll(RANGE);
		try {
			if (rangeHeaders.size() != 0) {
				sharedStorage.createObject(containerName, objId,
						offset, size);
				ioStats.markWrite(true, size);
			}
			else {
				ioStats.markWrite(
						handlePartialWrite(containerName, objId, rangeHeaders, size),
						size
				);
			}
		} catch (ContainerMockNotFoundException e) {
			changeHttpResponseStatusInContext(ctx, NOT_FOUND);
			ioStats.markWrite(false, size);
		} catch (StorageMockCapacityLimitReachedException e) {
			changeHttpResponseStatusInContext(ctx, INSUFFICIENT_STORAGE);
			ioStats.markWrite(false, size);
		}
	}

	private boolean handlePartialWrite(String containerName, String objId,
	                                   List<String> rangeHeaders, Long size) {
		String rangeHeaderValue, rangeValuePairs[], rangeValue[];
		long offset;
		for (String rangeHeader: rangeHeaders) {

		}
		return true;
	}

	private void handleRead(String containerName, String objId,
	                   Long offset, ChannelHandlerContext ctx) {
		FullHttpResponse response;
		try {
			T objData = sharedStorage.getObject(containerName, objId, offset, 0);
			if (objData != null) {
				ioStats.markRead(true, objData.getSize());
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, "Send data object with ID: {}", objData);
				}
				//todo solve a problem with a type of objData (T extends WSObjMock)
				response = new DefaultFullHttpResponse(HTTP_1_1, OK,
						Unpooled.copiedBuffer(objData.toString(), CharsetUtil.UTF_8));
				response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
				ctx.attr(currentHttpResponseKey).set(response);

			} else {
				changeHttpResponseStatusInContext(ctx, NOT_FOUND);
				ioStats.markRead(false, 0);
			}
		} catch (ContainerMockException e) {
			e.printStackTrace();
		}
	}

	private void changeHttpResponseStatusInContext(ChannelHandlerContext ctx, HttpResponseStatus status) {
		ctx.attr(currentHttpResponseKey).set(ctx.attr(currentHttpResponseKey).get().setStatus(status));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
