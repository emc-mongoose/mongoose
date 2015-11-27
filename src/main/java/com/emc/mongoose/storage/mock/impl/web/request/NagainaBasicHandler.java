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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
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
import java.util.concurrent.atomic.AtomicInteger;

import static com.emc.mongoose.core.api.io.req.WSRequestConfig.VALUE_RANGE_CONCAT;
import static com.emc.mongoose.core.api.io.req.WSRequestConfig.VALUE_RANGE_PREFIX;
import static io.netty.channel.ChannelHandler.*;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

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


	private AttributeKey<HttpRequest> requestKey = AttributeKey.valueOf("requestKey");
	private AttributeKey<HttpResponseStatus> responseStatusKey = AttributeKey.valueOf("responseStatusKey");
	private AttributeKey<Long> contentLengthKey = AttributeKey.valueOf("contentLengthKey");
	private AttributeKey<Boolean> ctxWriteFlagKey = AttributeKey.valueOf("ctxWriteFlagKey");

	private final static DocumentBuilder DOM_BUILDER;
	private final static TransformerFactory TF = TransformerFactory.newInstance();

	static {
		try {
			DOM_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	private String[] getContainerNameAndObjectId(String uri) {
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
		String[] pathChunks = queryStringDecoder.path().split("/");
		String[] result = new String[2];
		if (pathChunks.length == 2) {
			result[0] = pathChunks[1];
			result[1] = null;
		} else if (pathChunks.length >= 3) {
			result[0] = pathChunks[1];
			result[1] = pathChunks[2];
		}
		return result;
	}

	private void processHttpRequest(ChannelHandlerContext ctx, HttpRequest request) {
		ctx.attr(requestKey).set(request);
		if (request.headers().contains(CONTENT_LENGTH)) {
			ctx.attr(contentLengthKey).set(Long.parseLong(request.headers().get(CONTENT_LENGTH)));
		}
	}

	private void processHttpContent(ChannelHandlerContext ctx, HttpContent httpContent) {
		ByteBuf content = httpContent.content();
		if (ctx.attr(contentLengthKey) == null) {
			Long currentContentSize = ctx.attr(contentLengthKey).get();
			ctx.attr(contentLengthKey).set(currentContentSize + content.readableBytes());
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
				}
			} else if (lastMilliDelay.get() > 0) {
				lastMilliDelay.decrementAndGet();
			}
		}
		handleActually(ctx);
	}

	public void handleActually(ChannelHandlerContext ctx) {
		String method = ctx.attr(requestKey).get().getMethod().toString().toUpperCase();
		String[] namesArr = getContainerNameAndObjectId(ctx.attr(requestKey).get().getUri());
		String containerName = namesArr[0];
		String objId = namesArr[1];
		Long size = ctx.attr(contentLengthKey).get();
		ctx.attr(ctxWriteFlagKey).set(true);
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
			setHttpResponseStatusInContext(ctx, BAD_REQUEST);
		}
		if (ctx.attr(ctxWriteFlagKey).get()) {
			HttpResponseStatus status = ctx.attr(responseStatusKey).get();
			FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status != null ? status : OK);
			HttpHeaders.setContentLength(response, 0);
			ctx.write(response);
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

	private void handleContainerDelete(String containerName) {
		sharedStorage.deleteContainer(containerName); // todo check is it necessary here to send OK response (send by default)
	}

	private void handleContainerExists(String containerName, ChannelHandlerContext ctx) {
		if (sharedStorage.getContainer(containerName) == null) {
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
		}
	}

	private void handleContainerCreate(String containerName) {
		sharedStorage.createContainer(containerName);
	}

	protected void handleContainerList(String containerName, ChannelHandlerContext ctx) {
		int maxCount = DataItemContainer.DEFAULT_PAGE_SIZE;
		String marker = null;
		String uri = ctx.attr(requestKey).get().getUri();
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
		if (queryStringDecoder.parameters().containsKey("max-keys")) {
			maxCount = Integer.parseInt(queryStringDecoder.parameters().get("max-keys").get(0));
		} else {
			LOG.warn(Markers.ERR, "Failed to parse max keys argument value in the URI: " + uri);
		}
		if (queryStringDecoder.parameters().containsKey("marker")) {
			marker = queryStringDecoder.parameters().get("marker").get(0);
		}
		List<T> buff = new ArrayList<>(maxCount);
		T lastObj;
		try {
			lastObj = sharedStorage.listObjects(containerName, marker, buff, maxCount);
			if (LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
						Markers.MSG, "Bucket \"{}\": generated list of {} objects, last one is \"{}\"",
						containerName, buff.size(), lastObj
				);
			}
		} catch (ContainerMockNotFoundException e) {
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
			return;
		} catch (ContainerMockException e) {
			setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			return;
		}
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
		for (T dataObject : buff) {
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
		} catch (TransformerException ex) {
			setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			LogUtil.exception(LOG, Level.ERROR, ex, "Failed to build bucket XML listing");
			return;
		}
		byte[] content = bos.toByteArray();
		ctx.attr(ctxWriteFlagKey).set(false);
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content));
		response.headers().set(CONTENT_TYPE, ContentType.APPLICATION_XML.getMimeType());
		HttpHeaders.setContentLength(response, content.length);
		ctx.write(response);
	}

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

	private void handleWrite(String containerName, String objId,
	                         Long offset, Long size, ChannelHandlerContext ctx) {
		List<String> rangeHeadersValues = ctx.attr(requestKey).get().headers().getAll(RANGE);
		try {
			if (rangeHeadersValues.size() == 0 || rangeHeadersValues == null) {
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
				ctx.attr(ctxWriteFlagKey).set(false);
				response = new DefaultHttpResponse(HTTP_1_1, OK);
				HttpHeaders.setContentLength(response, objSize);
				ctx.write(response);
				ctx.write(new DataItemFileRegion(obj));
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

	private void setHttpResponseStatusInContext(ChannelHandlerContext ctx, HttpResponseStatus status) {
		ctx.attr(responseStatusKey).set(status);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
