package com.emc.mongoose.storage.mock.impl.web.request;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
// mongoose-storage-adapter-swift.jar
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
//
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
//
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
//
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by andrey on 13.05.15.
 */
public final class SwiftRequestHandler<T extends WSObjectMock>
extends WSRequestHandlerBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String AUTH = "auth", LIMIT = "limit", MARKER = "marker";
	private final static Pattern
		PATTERN_LIMIT = Pattern.compile(LIMIT + "=(?<" + LIMIT + ">[\\d]+)&?"),
		PATTERN_MARKER = Pattern.compile(MARKER + "=(?<" + MARKER + ">[a-z\\d]+)&?");
	//
	private final String apiBasePathSwift;
	//
	public SwiftRequestHandler(
		final RunTimeConfig runTimeConfig, final WSMock<T> sharedStorage
	) {
		super(runTimeConfig, sharedStorage);
		apiBasePathSwift = runTimeConfig.getString(WSRequestConfigImpl.KEY_CONF_SVC_BASEPATH);
	}
	//
	public boolean matches(final HttpRequest httpRequest) {
		return requestURI != null &&
			(requestURI.startsWith(AUTH, 1) || requestURI.startsWith(apiBasePathSwift, 1));
	}
	//
	@Override
	public final void handleActually(
		final HttpRequest httpRequest, final HttpResponse httpResponse, final String method,
		final String requestURI[], final String dataId
	) {
		final String container;
		if(requestURI.length > 1) {
			if(requestURI[1].equals(AUTH)) { // create an auth token
				final String authToken = randomString(0x10);
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, "Created auth token: {}", authToken);
				}
				httpResponse.setHeader(WSRequestConfigImpl.KEY_X_AUTH_TOKEN, authToken);
				httpResponse.setStatusCode(HttpStatus.SC_OK);
			} else if(
				requestURI[1].equals(apiBasePathSwift) && requestURI.length == 4
			) {
				container = requestURI[requestURI.length - 1];
				handleGenericContainerReq(httpRequest, httpResponse, method, container, dataId);
			} else {
				container = requestURI[requestURI.length - 2];
				handleGenericDataReq(httpRequest, httpResponse, method, container, dataId);
			}
		} else {
			httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
		}
	}
	//
	private final static ObjectMapper OBJ_MAPPER = new ObjectMapper();
	//
	@Override
	protected final void handleContainerList(
		final HttpRequest req, final HttpResponse resp, final String name, final String dataId
	) {
		final String uri = req.getRequestLine().getUri();
		int maxCount = -1;
		String marker = null;
		final Matcher maxKeysMatcher = PATTERN_LIMIT.matcher(uri);
		if(maxKeysMatcher.find()) {
			try {
				maxCount = Integer.parseInt(maxKeysMatcher.group(LIMIT));
			} catch(final NumberFormatException e) {
				LOG.warn(Markers.ERR, "Failed to parse max keys argument value in the URI: " + uri);
			}
		}
		final Matcher markerMatcher = PATTERN_MARKER.matcher(uri);
		if(markerMatcher.find()) {
			try {
				marker = markerMatcher.group(MARKER);
			} catch(final IllegalArgumentException ignored) {
			}
		}
		//
		if(maxCount <= 0) {
			maxCount = batchSize;
		}
		//
		final List<T> buff = new ArrayList<>(maxCount);
		try {
			marker = sharedStorage.list(name, marker, buff, maxCount);
			LOG.info(
				Markers.MSG, "Generated list of {} objects, last one is \"{}\"",
				buff.size(), marker
			);
		} catch(final ContainerMockNotFoundException e) {
			resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
			return;
		}
		//
		final JsonNode nodeRoot = OBJ_MAPPER.createArrayNode();
		ObjectNode node;
		for(final T dataObject : buff) {
			node = OBJ_MAPPER.createObjectNode();
			node.put("name", dataObject.getId());
			node.put("bytes", dataObject.getSize());
			((ArrayNode) nodeRoot).add(node);
		}
		//
		resp.setEntity(new StringEntity(nodeRoot.toString(), StandardCharsets.UTF_8));
		resp.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
	}
}
