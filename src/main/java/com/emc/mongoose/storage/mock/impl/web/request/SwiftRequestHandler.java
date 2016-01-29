package com.emc.mongoose.storage.mock.impl.web.request;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-storage-adapter-swift.jar
import com.emc.mongoose.core.api.item.data.MutableDataItem;
import com.emc.mongoose.core.api.item.data.ContainerHelper;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.storage.adapter.swift.HttpRequestConfigImpl;
//
import com.emc.mongoose.storage.mock.api.ContainerMockException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.HttpStorageMock;
//
import com.emc.mongoose.storage.mock.api.HttpDataItemMock;
import com.fasterxml.jackson.core.JsonProcessingException;
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
//
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by andrey on 13.05.15.
 */
public final class SwiftRequestHandler<T extends HttpDataItemMock>
extends WSRequestHandlerBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String
		AUTH = "auth", LIMIT = "limit", MARKER = "marker",
		KEY_VERSION = "version", KEY_ACCOUNT = "account", KEY_CONTAINER = "container",
		KEY_OID = "oid";
	private final static Pattern
		PATTERN_URI = Pattern.compile(
			"/v(?<" + KEY_VERSION + ">[0-9\\.]+)/(?<" + KEY_ACCOUNT + ">[^/]+)/?(?<" +
			KEY_CONTAINER + ">[^\\?^/]+)?/?(?<" + KEY_OID + ">[^\\?]+)?"
		),
		PATTERN_LIMIT = Pattern.compile(LIMIT + "=(?<" + LIMIT + ">[\\d]+)&?"),
		PATTERN_MARKER = Pattern.compile(MARKER + "=(?<" + MARKER + ">[a-z\\d]+)&?");
	//
	private final static String API_BASE_PATH = "v1";
	//
	public SwiftRequestHandler(
		final AppConfig appConfig, final HttpStorageMock<T> sharedStorage
	) {
		super(appConfig, sharedStorage);
	}
	//
	public boolean matches(final HttpRequest httpRequest) {
		final String requestURI = httpRequest.getRequestLine().getUri();
		return requestURI.startsWith(AUTH, 1) || requestURI.startsWith(API_BASE_PATH, 1);
	}
	//
	@Override
	public final void handleActually(
		final HttpRequest httpRequest, final HttpResponse httpResponse,
		final String method, final String requestURI
	) {
		if(requestURI.startsWith(AUTH, 1)) { // auth token
			if(HttpRequestConfig.METHOD_GET.equalsIgnoreCase(method)) { // create
				final String authToken = randomString(0x10);
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, "Created auth token: {}", authToken);
				}
				httpResponse.setHeader(HttpRequestConfigImpl.KEY_X_AUTH_TOKEN, authToken);
				httpResponse.setStatusCode(HttpStatus.SC_CREATED);
			} else {
				httpResponse.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
			}
		} else {
			final Matcher m = PATTERN_URI.matcher(requestURI);
			if(m.find()) {
				try {
					final String
						//ver = m.group(KEY_VERSION),
						acc = m.group(KEY_ACCOUNT),
						container = m.group(KEY_CONTAINER),
						oid = m.group(KEY_OID);
					if(oid != null) {
						final long offset;
						if(
							HttpRequestConfig.METHOD_PUT.equalsIgnoreCase(method) ||
							HttpRequestConfig.METHOD_POST.equalsIgnoreCase(method)
						) {
							offset = Long.parseLong(oid, MutableDataItem.ID_RADIX);
						} else {
							offset = -1;
						}
						handleGenericDataReq(
							httpRequest, httpResponse, method, container, oid, offset
						);
					} else if(container != null) {
						handleGenericContainerReq(
							httpRequest, httpResponse, method, container, null
						);
					} else if(acc != null) {
						httpResponse.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
					} else {
						httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
					}
				} catch(final IllegalStateException | IllegalArgumentException e) {
					httpResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				}
			} else {
				httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			}
		}
	}
	//
	private final static ObjectMapper OBJ_MAPPER = new ObjectMapper();
	//
	@Override
	protected final void handleContainerList(
		final HttpRequest req, final HttpResponse resp, final String name, final String oid
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
			maxCount = ContainerHelper.DEFAULT_PAGE_SIZE;
		}
		//
		final List<T> buff = new ArrayList<>(maxCount);
		final T lastObj;
		try {
			lastObj = sharedStorage.listObjects(name, marker, buff, maxCount);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG,
					"Container \"{}\": generated list of {} objects, last one is \"{}\"",
					name, buff.size(), lastObj
				);
			}
		} catch(final ContainerMockNotFoundException e) {
			resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
			return;
		} catch(final ContainerMockException e) {
			resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		//
		if(buff.size() > 0) {
			final JsonNode nodeRoot = OBJ_MAPPER.createArrayNode();
			ObjectNode node;
			for(final T dataObject : buff) {
				node = OBJ_MAPPER.createObjectNode();
				node.put("name", dataObject.getName());
				node.put("bytes", dataObject.getSize());
				((ArrayNode) nodeRoot).add(node);
			}
			//
			resp.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
			try {
				final byte bbuff[] = OBJ_MAPPER.writeValueAsBytes(nodeRoot);
				resp.setEntity(new NByteArrayEntity(bbuff));
			} catch(final JsonProcessingException e) {
				resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				LogUtil.exception(LOG, Level.WARN, e, "Failed to write the json response content");
			}
		} else {
			resp.setStatusCode(HttpStatus.SC_NO_CONTENT);
		}
	}
}
