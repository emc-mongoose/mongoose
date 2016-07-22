package com.emc.mongoose.storage.mock.impl.http.request;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import org.apache.commons.codec.binary.Hex;
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.emc.mongoose.storage.mock.impl.http.request.XmlShortcuts.appendElement;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_IMPLEMENTED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 Created on 12.07.16.
 */
public class AtmosRequestHandler<T extends MutableDataItemMock>
extends RequestHandlerBase<T> {

	private static final Logger LOG = LogManager.getLogger();
	private static final String
			URI_BASE_PATH = "/rest",
			OBJ_PATH = URI_BASE_PATH + "/objects",
			NS_PATH = URI_BASE_PATH + "/namespace",
			AT_PATH = URI_BASE_PATH + "/accesstokens",
			ST_PATH = URI_BASE_PATH + "/subtenant",
			STS_PATH = ST_PATH + "s/";
	private static final DocumentBuilder DOM_BUILDER;
	private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

	static {
		try {
			DOM_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public AtmosRequestHandler(
			final Config.LoadConfig.LimitConfig limitConfig,
			final StorageMock<T> sharedStorage,
			final ContentSource contentSource) {
		super(limitConfig, sharedStorage, contentSource);
	}

	@Override
	protected boolean checkApiMatch(final HttpRequest request) {
		return request.uri().startsWith(URI_BASE_PATH);
	}

	private String processMetaDataList(final String[] metaDataList, final String key) {
		if (metaDataList != null) {
			String entry[];
			for (final String metaData : metaDataList) {
				entry = metaData.split("=");
				if (entry.length == 2 && entry[0].equals(key)) {
					return entry[1];
				}
			}
		}
		return null;
	}

	private static final String KEY_EMC_TAGS = "x-emc-tags";
	private static final String KEY_EMC_TOKEN = "x-emc-token";

	@Override
	protected void doHandle(
			final String uri,
			final HttpMethod method,
			final Long size,
			final ChannelHandlerContext ctx) {
		final Channel channel = ctx.channel();
		final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
		final HttpRequest request = channel.attr(AttributeKey.<HttpRequest>valueOf
				(REQUEST_KEY)).get();
		String[] metaDataList = null;
		final HttpHeaders headers = request.headers();
		if (headers.contains(KEY_EMC_TAGS)) {
			metaDataList = headers.get(KEY_EMC_TAGS).split(",");
		}
		channel.attr(AttributeKey.<Boolean>valueOf(CTX_WRITE_FLAG_KEY)).set(true);
		if (uri.startsWith(OBJ_PATH)) {
			final String[] uriParams = getUriParameters(uri, 3);
			String objectId = uriParams[2];
			long offset = -1;
			String subtenantName = getSubtenant(headers, uri);
			if (objectId == null) {
				if (method.equals(POST)) {
					objectId = generateId();
					final String processResult =
							processMetaDataList(metaDataList, "offset");
					try {
						if (processResult != null) {
							offset = Long.parseLong(processResult);
						}
					} catch (final NumberFormatException e) {
						LogUtil.exception(
								LOG, Level.WARN, e,
								"Failed to parse offset meta tag value: \"{}\"",
								processResult
						);
					}
					handleObjectRequest(method, subtenantName, objectId, offset, size, ctx);
					final int statusCode = response.status().code();
					if (statusCode < 300 && 200 <= statusCode) {
						headers.set(LOCATION, OBJ_PATH + "/" + objectId);
					}
				} else if (method.equals(GET)) {
					subtenantName = processMetaDataList(metaDataList, "subtenant");
					if (headers.contains(KEY_EMC_TOKEN)) {
						objectId = headers.get(KEY_EMC_TOKEN);
					}
					handleContainerList(subtenantName, objectId, ctx);
				}
			} else {
				handleObjectRequest(method, subtenantName, objectId, offset, size, ctx);
			}
		} else if (uri.startsWith(NS_PATH) || (uri.startsWith(AT_PATH))) {
			setHttpResponseStatusInContext(ctx, NOT_IMPLEMENTED);
		} else if (uri.startsWith(ST_PATH)) {
			final String subtenantName;
			if (method.equals(PUT)) {
				subtenantName = generateSubtenant();
			} else {
				subtenantName = getSubtenant(headers, uri);
			}
			handleContainerRequest(uri, method, subtenantName, ctx);
		} else {
			setHttpResponseStatusInContext(ctx, BAD_REQUEST);
		}
		if (channel.attr(AttributeKey.<Boolean>valueOf(CTX_WRITE_FLAG_KEY)).get()) {
			writeResponse(ctx);
		}
	}



	private static String generateId() {
		final byte buff[] = new byte[22];
		ThreadLocalRandom.current().nextBytes(buff);
		return Hex.encodeHexString(buff);
	}

	private static final String KEY_EMC_UID = "x-emc-uid";
	private static final String UID_DELIMITER = "/";
	private static final String KEY_SUBTENANT_ID = "subtenantID";

	private static String getSubtenant(final HttpHeaders headers, final String uri) {
		if (uri.startsWith(STS_PATH) && uri.length() > STS_PATH.length()) {
			return uri.substring(STS_PATH.length());
		}
		if (headers.contains(KEY_EMC_UID)) {
			String uid = headers.get(KEY_EMC_UID);
			if (uid.contains(UID_DELIMITER)) {
				return uid.split(UID_DELIMITER)[0];
			}
		}
		if (headers.contains(KEY_SUBTENANT_ID)) {
			return headers.get(KEY_SUBTENANT_ID);
		}
		return null;
	}

	private static String generateSubtenant() {
		final byte buff[] = new byte[0x10];
		ThreadLocalRandom.current().nextBytes(buff);
		return Hex.encodeHexString(buff);
	}

	@Override
	protected void handleContainerList(
			final String name,
			final QueryStringDecoder queryStringDecoder,
			final ChannelHandlerContext ctx) {
		// there is a distinguish behavior for Atmos Protocol
	}

	private static final String KEY_EMC_LIMIT = "x-emc-limit";

	private void handleContainerList(
			final String name,
			final String objectId,
			final ChannelHandlerContext ctx
	) {
		int maxCount = DEFAULT_PAGE_SIZE;
		final HttpHeaders headers = ctx.channel()
				.attr(AttributeKey.<HttpRequest>valueOf(REQUEST_KEY)).get().headers();
		if (headers.contains(KEY_EMC_LIMIT)) {
			try {
				maxCount = Integer.parseInt(headers.get(KEY_EMC_LIMIT));
			} catch (final NumberFormatException e) {
				LOG.warn(
						Markers.ERR, "Limit header value is not a valid integer: {}",
						headers.get(KEY_EMC_LIMIT)
				);
			}
		}

		final List<T> buffer = new ArrayList<>(maxCount);
		final T lastObj = listContainer(name, objectId, buffer, maxCount);
		Map.Entry<String, String> header = null;
		if (lastObj != null) {
			header = new AbstractMap.SimpleEntry<>(KEY_EMC_TOKEN, lastObj.getName());
		}
		final Document xml = DOM_BUILDER.newDocument();
		final Element rootElem = xml.createElement("ListObjectsResponse");
		xml.appendChild(rootElem);
		for (final T object: buffer) {
			final Element elem = xml.createElement("Object");
			appendElement(xml, elem, "ObjectID", object.getName());
			appendElement(rootElem, elem);
		}
		final ByteArrayOutputStream stream = new ByteArrayOutputStream();
		final StreamResult streamResult = new StreamResult(stream);
		try {
			TRANSFORMER_FACTORY.newTransformer().transform(new DOMSource(xml), streamResult);
		} catch (final TransformerException e) {
			setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to build subtenant XML listing");
			return;
		}
		final byte[] content = stream.toByteArray();
		ctx.channel().attr(AttributeKey.<Boolean>valueOf(CTX_WRITE_FLAG_KEY)).set(false);
		final FullHttpResponse
				response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content));
		response.headers().set(CONTENT_TYPE, "application/xml");
		if (header != null) {
			response.headers().set(header.getKey(), header.getValue());
		}
		HttpUtil.setContentLength(response, content.length);
		ctx.write(response);
	}
}
