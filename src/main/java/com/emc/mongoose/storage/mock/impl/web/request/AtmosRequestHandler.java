package com.emc.mongoose.storage.mock.impl.web.request;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
//
// mongoose-storage-adapter-atmos.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.storage.adapter.atmos.SubTenant;
//
import com.emc.mongoose.storage.mock.api.ContainerMockException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
//
import org.apache.commons.codec.binary.Hex;
//
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
//
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NByteArrayEntity;
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
		KEY_OID = "oid",
		KEY_VERSIONING = "versioning",
		KEY_DIR = "dir",
		KEY_FNAME = "fName";
	private final static Pattern
		PATTERN_REQ_URI_OBJ = Pattern.compile(
		OBJ_PATH + "/?(?<" + KEY_OID + ">[a-f\\d]{44})?\\??(?<" + KEY_VERSIONING +
			">versions)?"
	),
		PATTERN_REQ_URI_NS = Pattern.compile(
			NS_PATH + "/?(?<" + KEY_DIR + ">[\\w]+/)*(?<" + KEY_FNAME + ">[^\\?])\\??(?<" +
				KEY_VERSIONING + ">versions)?"
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
		final String metaDataList[] = httpRequest.containsHeader(WSRequestConfig.KEY_EMC_TAGS) ?
			httpRequest.getFirstHeader(WSRequestConfig.KEY_EMC_TAGS).getValue().split(",") :
			null;
		if(requestURI.startsWith(OBJ_PATH)) {
			final Matcher m = PATTERN_REQ_URI_OBJ.matcher(requestURI);
			if(m.find()) {
				String oid = m.group(KEY_OID);
				long offset = -1;
				if(oid == null) {
					if(WSRequestConfig.METHOD_POST.equalsIgnoreCase(method)) {
						oid = generateId();
						if(metaDataList != null) {
							String keyValuePair[];
							for(final String metaData : metaDataList) {
								keyValuePair = metaData.split("=");
								if(keyValuePair.length == 2 && "offset".equals(keyValuePair[0])) {
									try {
										offset = Long.parseLong(keyValuePair[1]);
									} catch(final NumberFormatException e) {
										LogUtil.exception(
											LOG, Level.WARN, e,
											"Failed to parse offset meta tag value: \"{}\"",
											keyValuePair[1]
										);
									}
								}
							}
						}
						handleGenericDataReq(
							httpRequest, httpResponse, method, getSubtenant(httpRequest),
							oid, offset
						);
						if(300 > httpResponse.getStatusLine().getStatusCode()) {
							httpResponse.setHeader(HttpHeaders.LOCATION, OBJ_PATH + '/' + oid);
						}
					} else if(WSRequestConfig.METHOD_GET.equalsIgnoreCase(method)) {
						String subtenant = null;
						if(metaDataList != null) {
							String keyValuePair[];
							for(final String metaData : metaDataList) {
								keyValuePair = metaData.split("=");
								if(keyValuePair.length == 2 && "subtenant".equals(keyValuePair[0])) {
									subtenant = keyValuePair[1];
								}
							}
						}
						if(httpRequest.containsHeader(WSRequestConfig.KEY_EMC_TOKEN)) {
							oid = httpRequest
								.getFirstHeader(WSRequestConfig.KEY_EMC_TOKEN)
								.getValue();
						}
						handleContainerList(httpRequest, httpResponse, subtenant, oid);
					}
				} else {
					handleGenericDataReq(
						httpRequest, httpResponse, method, getSubtenant(httpRequest), oid, offset
					);
				}
			} else {
				httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			}
		} else if(requestURI.startsWith(NS_PATH)) {
			httpResponse.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
		} else if(requestURI.startsWith(AT_PATH)) {
			httpResponse.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
		} else if(requestURI.startsWith(ST_PATH)) {
			final String subtenant = WSRequestConfig.METHOD_PUT.equalsIgnoreCase(method) ?
				generateSubtenant() : getSubtenant(httpRequest);
			handleGenericContainerReq(
				httpRequest, httpResponse, method,
				WSRequestConfig.METHOD_PUT.equalsIgnoreCase(method) ?
					generateSubtenant() : subtenant,
				null
			);
		} else {
			httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
		}
	}
	//
	private final static DocumentBuilder DOM_BUILDER;
	private final static TransformerFactory TF = TransformerFactory.newInstance();

	static {
		try {
			DOM_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch(final ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	//
	@Override
	protected final void handleContainerList(
		final HttpRequest req, final HttpResponse resp, final String subtenant, final String oid
	) {
		int maxCount = batchSize;
		if(req.containsHeader(WSRequestConfig.KEY_EMC_LIMIT)) {
			try {
				maxCount = Integer.parseInt(
					req.getFirstHeader(WSRequestConfig.KEY_EMC_LIMIT).getValue()
				);
			} catch(final NumberFormatException e) {
				LOG.warn(
					Markers.ERR, "Limit header value is not a valid integer: {}",
					req.getFirstHeader(WSRequestConfig.KEY_EMC_LIMIT).getValue()
				);
			}
		}
		//
		final List<T> buff = new ArrayList<>(maxCount);
		final T lastObj;
		try {
			lastObj = sharedStorage.listObjects(subtenant, oid, buff, maxCount);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Subtenant \"{}\": generated list of {} objects, last one is \"{}\"",
					subtenant, buff.size(), lastObj
				);
			}
		} catch(final ContainerMockNotFoundException e) {
			resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
			return;
		} catch(final ContainerMockException e) {
			resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			LogUtil.exception(LOG, Level.WARN, e, "Subtenant \"{}\" failure", subtenant);
			return;
		}
		//
		if(lastObj != null) {
			resp.setHeader(WSRequestConfig.KEY_EMC_TOKEN, lastObj.getId());
		}
		//
		final Document doc = DOM_BUILDER.newDocument();
		final Element eRoot = doc.createElement("ListObjectsResponse");
		doc.appendChild(eRoot);
		//
		Element e, ee;
		for(final T dataObject : buff) {
			e = doc.createElement("Object");
			ee = doc.createElement("ObjectID");
			ee.appendChild(doc.createTextNode(dataObject.getId()));
			e.appendChild(ee);
			eRoot.appendChild(e);
		}
		//
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final StreamResult r = new StreamResult(bos);
		try {
			TF.newTransformer().transform(new DOMSource(doc), r);
		} catch(final TransformerException ex) {
			resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			LogUtil.exception(LOG, Level.ERROR, ex, "Failed to build subtenant XML listing");
			return;
		}
		//
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "Responding the subtenant \"{}\" listing content:\n{}",
				subtenant, new String(bos.toByteArray())
			);
		}
		resp.setEntity(new NByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_XML));
	}
	//
	private String getSubtenant(final HttpRequest httpRequest) {
		if(httpRequest.containsHeader(WSRequestConfig.KEY_EMC_UID)) {
			final String uid[] = httpRequest
				.getLastHeader(WSRequestConfig.KEY_EMC_UID)
				.getValue()
				.split("/");
			return uid.length < 1 ? null : uid[0];
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
	public static String generateSubtenant() {
		final byte buff[] = new byte[0x10];
		ThreadLocalRandom.current().nextBytes(buff);
		return Hex.encodeHexString(buff);
	}
	//
	@Override
	protected final boolean handleContainerCreate(
		final HttpRequest req, final HttpResponse resp, final String name
	) {
		final boolean created = super.handleContainerCreate(req, resp, name);
		if(created) {
			resp.setHeader(SubTenant.KEY_SUBTENANT_ID, name);
		}
		return created;
	}
}
