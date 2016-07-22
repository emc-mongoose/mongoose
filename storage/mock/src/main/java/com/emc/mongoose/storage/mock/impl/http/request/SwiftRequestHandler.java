package com.emc.mongoose.storage.mock.impl.http.request;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpMethod.GET;
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
@Sharable
public class SwiftRequestHandler<T extends MutableDataItemMock>
extends RequestHandlerBase<T> {

	private static final Logger LOG = LogManager.getLogger();
	private static final ObjectMapper OBJ_MAPPER = new ObjectMapper();
	private static final String AUTH = "auth", API_BASE_PATH_SWIFT = "v1";


	SwiftRequestHandler(final Config.ItemConfig.NamingConfig namingConfig,
                        final Config.LoadConfig.LimitConfig limitConfig,
						final StorageMock<T> sharedStorage,
						final ContentSource contentSource) {
		super(limitConfig, sharedStorage, contentSource);
		final String prefix = namingConfig.getPrefix();
		if (prefix != null) {
			setPrefixLength(prefix.length());
		} else {
			setPrefixLength(0);
		}
		setIdRadix(namingConfig.getRadix());
	}

	@Override
	protected boolean checkApiMatch(final HttpRequest request) {
		final String uri = request.uri();
		return uri.startsWith(AUTH, 1) || uri.startsWith(API_BASE_PATH_SWIFT, 1);
	}

    private static final String KEY_X_AUTH_TOKEN = "X-Auth-Token";

    @Override
	protected void doHandle(
			final String uri,
			final HttpMethod method,
			final Long size,
			final ChannelHandlerContext ctx) {
        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.EMPTY_BUFFER, false);
        final Channel channel = ctx.channel();
        channel.attr(AttributeKey.<Boolean>valueOf(CTX_WRITE_FLAG_KEY)).set(true);
        if (uri.startsWith(AUTH, 1)) {
            if (method.equals(GET)) {
                final String authToken = randomString(0x10);
                if (LOG.isTraceEnabled(Markers.MSG)) {
                    LOG.trace(Markers.MSG, "Created auth token: {}", authToken);
                }
                response.headers().set(KEY_X_AUTH_TOKEN, authToken);
                setHttpResponseStatusInContext(ctx, CREATED);
            } else {
                setHttpResponseStatusInContext(ctx, NOT_IMPLEMENTED);
            }
        } else {
            final String[] uriParams = getUriParameters(uri, 4);
            final String account = uriParams[1];
            final String containerName = uriParams[2];
            final String objectId = uriParams[3];
            if (containerName != null) {
                handleItemRequest(uri, method, containerName, objectId, size, ctx);
            } else if (account != null) {
                setHttpResponseStatusInContext(ctx, NOT_IMPLEMENTED);
            } else {
                setHttpResponseStatusInContext(ctx, BAD_REQUEST);
            }
        }
        if (channel.attr(AttributeKey.<Boolean>valueOf(CTX_WRITE_FLAG_KEY)).get()) {
            writeResponse(ctx);
        }
    }

    private static String randomString(int len) {
        final byte buff[] = new byte[len];
        ThreadLocalRandom.current().nextBytes(buff);
        return Hex.encodeHexString(buff);
    }

    private static final String LIMIT_KEY = "limit";

	@Override
	protected void handleContainerList(
            final String name,
            final QueryStringDecoder queryStringDecoder,
            final ChannelHandlerContext ctx) {
        int maxCount = DEFAULT_PAGE_SIZE;
        String marker = null;
        final Map<String, List<String>> parameters = queryStringDecoder.parameters();
        if (parameters.containsKey(LIMIT_KEY)) {
            maxCount = Integer.parseInt(parameters.get("limit").get(0));
        } else {
            LOG.warn(Markers.ERR, "Failed to parse max keys argument value in the URI: " +
                    queryStringDecoder.uri());
        }
        if (parameters.containsKey(MARKER_KEY)) {
            marker = parameters.get(MARKER_KEY).get(0);
        }
        final List<T> buffer = new ArrayList<>(maxCount);
        listContainer(name, marker, buffer, maxCount);
        if (buffer.size() > 0) {
            final JsonNode nodeRoot = OBJ_MAPPER.createArrayNode();
            ObjectNode node;
            for(final T object : buffer) {
                node = OBJ_MAPPER.createObjectNode();
                node.put("name", object.getName());
                node.put("bytes", object.getSize());
                ((ArrayNode) nodeRoot).add(node);
            }
            try {
                final byte[] content = OBJ_MAPPER.writeValueAsBytes(nodeRoot);
                ctx.channel().attr(AttributeKey.<Boolean>valueOf(CTX_WRITE_FLAG_KEY)).set(false);
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content));
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
