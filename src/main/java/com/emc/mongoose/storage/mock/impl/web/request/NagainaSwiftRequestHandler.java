package com.emc.mongoose.storage.mock.impl.web.request;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.data.MutableDataItem;
import com.emc.mongoose.core.api.data.model.ContainerHelper;
import com.emc.mongoose.core.api.io.conf.WSRequestConfig;
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
import com.emc.mongoose.storage.mock.api.ContainerMockException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static io.netty.channel.ChannelHandler.Sharable;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_IMPLEMENTED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class NagainaSwiftRequestHandler<T extends WSObjectMock> extends NagainaRequestHandlerBase<T> {

	private final static Logger LOG = LogManager.getLogger();
	private final static ObjectMapper OBJ_MAPPER = new ObjectMapper();
	private final static String
			AUTH = "auth";

	public NagainaSwiftRequestHandler(RunTimeConfig rtConfig, WSMock<T> sharedStorage) {
		super(rtConfig, sharedStorage);
	}

	@Override
	protected void handleActually(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.EMPTY_BUFFER, false);
		String uri = ctx.attr(AttributeKey.<HttpRequest>valueOf(requestKey))
				.get().getUri();
		String method = ctx.attr(AttributeKey.<HttpRequest>valueOf(requestKey))
				.get().getMethod().toString().toUpperCase();
		Long size = ctx.attr(AttributeKey.<Long>valueOf(contentLengthKey)).get();
		ctx.attr(AttributeKey.<Boolean>valueOf(ctxWriteFlagKey)).set(true);
		if (uri.startsWith(AUTH, 1)) {
			if (method.equals(WSRequestConfig.METHOD_GET)) {
				String authToken = randomString(0x10);
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, "Created auth token: {}", authToken);
				}
				response.headers().set(WSRequestConfigImpl.KEY_X_AUTH_TOKEN, authToken);
				setHttpResponseStatusInContext(ctx, CREATED);
			}
			else {
				setHttpResponseStatusInContext(ctx, NOT_IMPLEMENTED);
			}
		} else {
			String[] uriParams =
					getUriParams(ctx.attr(AttributeKey.<HttpRequest>valueOf(requestKey))
							.get().getUri(), 4);
			try {
				String account = uriParams[1], containerName = uriParams[2], objId = uriParams[3];
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
				} else if (account != null) {
					setHttpResponseStatusInContext(ctx, NOT_IMPLEMENTED);
				} else {
					setHttpResponseStatusInContext(ctx, BAD_REQUEST);
				}
			}
			catch(IllegalStateException | IllegalArgumentException e) {
				setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			}
		}
		if (ctx.attr(AttributeKey.<Boolean>valueOf(ctxWriteFlagKey)).get()) {
			HttpResponseStatus status = ctx.attr(AttributeKey.<HttpResponseStatus>valueOf(responseStatusKey)).get();
			response.setStatus(status != null ? status : OK);
			HttpHeaders.setContentLength(response, 0);
			ctx.write(response);
		}
	}

	@Override
	protected void handleContainerList(String containerName, ChannelHandlerContext ctx) {
		int maxCount = ContainerHelper.DEFAULT_PAGE_SIZE;
		String marker = null;
		String uri = ctx.attr(AttributeKey.<HttpRequest>valueOf(requestKey)).get().getUri();
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
		if (queryStringDecoder.parameters().containsKey("limit")) {
			maxCount = Integer.parseInt(queryStringDecoder.parameters().get("limit").get(0));
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
		if(buff.size() > 0) {
			JsonNode nodeRoot = OBJ_MAPPER.createArrayNode();
			ObjectNode node;
			for(final T dataObject : buff) {
				node = OBJ_MAPPER.createObjectNode();
				node.put("name", dataObject.getName());
				node.put("bytes", dataObject.getSize());
				((ArrayNode) nodeRoot).add(node);
			}
			try {
				byte[] content = OBJ_MAPPER.writeValueAsBytes(nodeRoot);
				ctx.attr(AttributeKey.<Boolean>valueOf(ctxWriteFlagKey)).set(false);
				FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content));
				response.headers().set(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
				HttpHeaders.setContentLength(response, content.length);
				ctx.write(response);
			} catch (JsonProcessingException e) {
				setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
				LogUtil.exception(LOG, Level.WARN, e, "Failed to write the json response content");
			}

		} else {
			setHttpResponseStatusInContext(ctx, NO_CONTENT);
		}

	}

	protected static String randomString(int len) {
		final byte buff[] = new byte[len];
		ThreadLocalRandom.current().nextBytes(buff);
		return Hex.encodeHexString(buff);
	}
}
