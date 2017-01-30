package com.emc.mongoose.storage.mock.impl.http.request;

import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockNotFoundException;
import static com.emc.mongoose.ui.config.Config.ItemConfig.NamingConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_IMPLEMENTED;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 Created on 12.07.16.
 */
public class SwiftRequestHandler<T extends MutableDataItemMock>
extends RequestHandlerBase<T> {

	private static final Logger LOG = LogManager.getLogger();
	private static final ObjectMapper OBJ_MAPPER = new ObjectMapper();
	private static final String AUTH = "auth", API_BASE_PATH_SWIFT = "v1";
	
	public SwiftRequestHandler(
		final LimitConfig limitConfig, final NamingConfig namingConfig,
		final StorageMock<T> localStorage, final StorageMockClient<T> remoteStorage
	) throws RemoteException {
		super(limitConfig, namingConfig, localStorage, remoteStorage);
	}

	@Override
	protected boolean checkApiMatch(final HttpRequest request) {
		final String uri = request.uri();
		return uri.startsWith(AUTH, 1) || uri.startsWith(API_BASE_PATH_SWIFT, 1);
	}

    private static final String KEY_X_AUTH_TOKEN = "X-Auth-Token";

    @Override
	protected void doHandle(
		final String uriPath, final Map<String, String> queryParams, final HttpMethod method,
		final long size, final ChannelHandlerContext ctx
	) {
		if(localStorage.missResponse()) {
			return;
		}
		FullHttpResponse response = null;
		final Channel channel = ctx.channel();
		channel.attr(ATTR_KEY_CTX_WRITE_FLAG).set(true);
		if(uriPath.startsWith(AUTH, 1)) {
			if(method.equals(GET)) {
				final String authToken = randomString(0x10);
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, "Created auth token: {}", authToken);
				}
				response = newEmptyResponse();
				response.headers().set(KEY_X_AUTH_TOKEN, authToken);
				setHttpResponseStatusInContext(ctx, CREATED);
			} else {
				setHttpResponseStatusInContext(ctx, NOT_IMPLEMENTED);
			}
		} else {
			final String uriPathParts[] = uriPath.split("/");
			final String account = uriPathParts.length > 2 ? uriPathParts[2] : null;
			final String containerName = uriPathParts.length > 3 ? uriPathParts[3] : null;
			final String objectId = uriPathParts.length > 4 ? uriPathParts[4] : null;
			if(containerName != null) {
				handleItemRequest(method, queryParams, containerName, objectId, size, ctx);
			} else if(account != null) {
				setHttpResponseStatusInContext(ctx, NOT_IMPLEMENTED);
			} else {
				setHttpResponseStatusInContext(ctx, BAD_REQUEST);
			}
		}
	    if(channel.attr(ATTR_KEY_CTX_WRITE_FLAG).get()) {
			if (response == null) {
				writeEmptyResponse(ctx);
			} else {
				writeResponse(ctx, response);
			}
	    }
	}

	private static String randomString(final int len) {
		final byte buff[] = new byte[len];
		ThreadLocalRandom.current().nextBytes(buff);
		return Hex.encodeHexString(buff);
	}

	private static final String LIMIT_KEY = "limit";

	@Override
	protected void handleContainerList(
		final String name, final Map<String, String> queryParams, final ChannelHandlerContext ctx
	) {
		if(localStorage.missResponse()) {
			return;
		}
		int maxCount = DEFAULT_PAGE_SIZE;
		String marker = null;
		if(queryParams != null) {
			if(queryParams.containsKey(LIMIT_KEY)) {
				maxCount = Integer.parseInt(queryParams.get(LIMIT_KEY));
			}
			if(queryParams.containsKey(MARKER_KEY)) {
				marker = queryParams.get(MARKER_KEY);
			}
		}
		final List<T> buffer = new ArrayList<>(maxCount);
		final T lastObject;
		try {
			lastObject = listContainer(name, marker, buffer, maxCount);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG,
					"Container \"{}\": generated list of {} objects, last one is \"{}\"",
					name, buffer.size(), lastObject
				);
			}
		} catch(final ContainerMockNotFoundException e) {
			setHttpResponseStatusInContext(ctx, NOT_FOUND);
			return;
		} catch(final ContainerMockException e) {
			setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
			return;
		}
		if(buffer.size() > 0) {
			final JsonNode nodeRoot = OBJ_MAPPER.createArrayNode();
			ObjectNode node;
			for(final T object : buffer) {
				node = OBJ_MAPPER.createObjectNode();
				node.put("name", object.getName());
				try {
					node.put("bytes", object.size());
				} catch(final IOException ignored) {
				}
				((ArrayNode) nodeRoot).add(node);
			}
			try {
				final byte[] content = OBJ_MAPPER.writeValueAsBytes(nodeRoot);
				ctx.channel().attr(ATTR_KEY_CTX_WRITE_FLAG).set(false);
				final FullHttpResponse response = new DefaultFullHttpResponse(
					HTTP_1_1, OK, Unpooled.copiedBuffer(content)
				);
				response.headers().set(CONTENT_TYPE, "application/json");
				HttpUtil.setContentLength(response, content.length);
				ctx.write(response);
			} catch (final JsonProcessingException e) {
				setHttpResponseStatusInContext(ctx, INTERNAL_SERVER_ERROR);
				LogUtil.exception(LOG, Level.WARN, e, "Failed to write the json response content");
			}
		} else {
			setHttpResponseStatusInContext(ctx, NO_CONTENT);
		}
	}
}
