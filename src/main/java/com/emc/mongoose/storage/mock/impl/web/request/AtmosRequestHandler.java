package com.emc.mongoose.storage.mock.impl.web.request;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
//
// mongoose-storage-adapter-atmos.jar
import com.emc.mongoose.storage.adapter.atmos.SubTenant;
//
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
//
import org.apache.commons.codec.binary.Hex;
//
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by andrey on 13.05.15.
 */
public final class AtmosRequestHandler<T extends WSObjectMock>
extends WSRequestHandlerBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	/*
	Variants:
	POST /rest/objects
	PUT|GET|DELETE|HEAD /rest/objects[/idLen44[?versions]]
	POST|PUT|GET|DELETE|HEAD /rest/namespace/[path]
	POST|DELETE /rest/accesstokens {x-emc-uid, x-emc-signature, x-emc-date} + XML payload -> 201
	PUT|DELETE /rest/subtenant
	 */
	private final static String
		URI_BASE_PATH = "/rest",
		OBJ_PATH = URI_BASE_PATH + "/objects",
		NS_PATH = URI_BASE_PATH + "/namespace",
		AT_PATH = URI_BASE_PATH + "/accesstokens",
		ST_PATH = URI_BASE_PATH + "/subtenant",
		GROUP_OBJ_ID = "objId",
		GROUP_VERSIONING = "versioning",
		GROUP_DIR = "dir",
		GROUP_FILE_NAME = "fileName";
	private final static Pattern
		PATTERN_REQ_URI_OBJ = Pattern.compile(
			OBJ_PATH + "/?(?<" + GROUP_OBJ_ID + ">[a-f\\d]{44})?\\??(?<" + GROUP_VERSIONING +
			">versions)?"
		),
		PATTERN_REQ_URI_NS = Pattern.compile(
			NS_PATH + "/?(?<" + GROUP_DIR + ">[\\w]+/)*(?<" + GROUP_FILE_NAME + ">[^\\?])\\??(?<" +
			GROUP_VERSIONING + ">versions)?"
		);
	//
	public AtmosRequestHandler(final RunTimeConfig runTimeConfig, final WSMock<T> sharedStorage) {
		super(runTimeConfig, sharedStorage);
	}
	//
	@Override
	public final boolean matches(final HttpRequest httpRequest) {
		return httpRequest.getRequestLine().getUri().startsWith(URI_BASE_PATH);
	}
	//
	@Override
	public final void handleActually(
		final HttpRequest httpRequest, final HttpResponse httpResponse,
		final String method, final String requestURI
	) {
		if(requestURI.startsWith(OBJ_PATH)) {
			final Matcher m = PATTERN_REQ_URI_OBJ.matcher(requestURI);
			if(m.find()) {
				String objId = m.group(GROUP_OBJ_ID);
				if(objId == null) {
					if(METHOD_POST.equals(method)) {
						objId = generateId();
					} else {
						httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
						return;
					}
				}
				handleGenericDataReq(
					httpRequest, httpResponse, method, getSubtenant(httpRequest), objId
				);
			} else {
				httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				return;
			}
		} else if(requestURI.startsWith(NS_PATH)) {
			httpResponse.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
			return;
		} else if(requestURI.startsWith(AT_PATH)) {
			httpResponse.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
			return;
		} else if(requestURI.startsWith(ST_PATH)) {
			final String subtenant = METHOD_PUT.equals(method) ?
				generateSubtenant() : getSubtenant(httpRequest);
			handleGenericContainerReq(
				httpRequest, httpResponse,
				METHOD_PUT.equals(method) ? generateSubtenant() : subtenant,
				method, null
			);
		} else {
			httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			return;
		}
	}
	//
	@Override
	protected final void handleContainerList(
		final HttpRequest req, final HttpResponse resp, final String name, final String dataId
	) {
		resp.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
	}
	//
	private String getSubtenant(final HttpRequest httpRequest) {
		if(httpRequest.containsHeader(SubTenant.KEY_SUBTENANT_ID)) {
			return httpRequest.getLastHeader(SubTenant.KEY_SUBTENANT_ID).getValue();
		} else {
			return null;
		}
	}
	//
	private String generateId() {
		final byte buff[] = new byte[22];
		ThreadLocalRandom.current().nextBytes(buff);
		return Hex.encodeHexString(buff);
	}
	//
	private String generateSubtenant() {
		final byte buff[] = new byte[0x10];
		ThreadLocalRandom.current().nextBytes(buff);
		return Hex.encodeHexString(buff);
	}
}
