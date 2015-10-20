package com.emc.mongoose.storage.mock.impl.web.request;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
// mongoose-storage-mock.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.data.MutableDataItem;
import com.emc.mongoose.core.api.data.model.GenericContainer;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.storage.adapter.s3.Bucket;
//
import com.emc.mongoose.storage.mock.api.ContainerMockException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.WSMock;
//
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
//
import org.apache.http.nio.entity.NByteArrayEntity;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by andrey on 13.05.15.
 */
public final class S3RequestHandler<T extends WSObjectMock>
extends WSRequestHandlerBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String
		MAX_KEYS = "maxKeys", MARKER = "marker",
		BUCKET = "bucket", OBJ_ID = "objId",
		AUTH_PREFIX = RunTimeConfig.getContext().getApiS3AuthPrefix() + " ";
	private final static Pattern
		PATTERN_URI = Pattern.compile("/(?<" + BUCKET + ">[^/^\\?]+)/?(?<" + OBJ_ID + ">[^\\?]+)?"),
		PATTERN_MAX_KEYS = Pattern.compile(
			Bucket.URL_ARG_MAX_KEYS + "=(?<" + MAX_KEYS +  ">[\\d]+)&?"
		),
		PATTERN_MARKER = Pattern.compile(Bucket.URL_ARG_MARKER + "=(?<" + MARKER + ">[a-z\\d]+)&?");
	//
	public S3RequestHandler(final RunTimeConfig runTimeConfig, final WSMock<T> sharedStorage) {
		super(runTimeConfig, sharedStorage);
	}
	//
	@Override
	public boolean matches(final HttpRequest httpRequest) {
		final Header authHeader = httpRequest.getFirstHeader(HttpHeaders.AUTHORIZATION);
		if(authHeader == null) {
			return false;
		}
		return authHeader.getValue().startsWith(AUTH_PREFIX);
	}
	//
	@Override
	public final void handleActually(
		final HttpRequest httpRequest, final HttpResponse httpResponse,
		final String method, final String requestURI
	) {
		final Matcher m = PATTERN_URI.matcher(requestURI);
		try {
			if(m.find()) {
				final String
					bucket = m.group(BUCKET),
					objId = m.group(OBJ_ID);
				if(bucket != null) {
					if(objId == null) {
						handleGenericContainerReq(httpRequest, httpResponse, method, bucket, null);
					} else {
						final long offset;
						if(
							WSRequestConfig.METHOD_PUT.equalsIgnoreCase(method) ||
							WSRequestConfig.METHOD_POST.equalsIgnoreCase(method)
						) {
							offset = Long.parseLong(objId, MutableDataItem.ID_RADIX);
						} else {
							offset = -1;
						}
						handleGenericDataReq(
							httpRequest, httpResponse, method, bucket, objId, offset
						);
					}
				} else {
					httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				}
			} else {
				httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			}
		} catch(final IllegalArgumentException | IllegalStateException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to parse the request URI: {}", requestURI);
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
		final HttpRequest req, final HttpResponse resp, final String name, final String dataId
	) {
		final String uri = req.getRequestLine().getUri();
		int maxCount = GenericContainer.DEFAULT_PAGE_SIZE;
		String marker = null;
		final Matcher maxKeysMatcher = PATTERN_MAX_KEYS.matcher(uri);
		if(maxKeysMatcher.find()) {
			try {
				maxCount = Integer.parseInt(maxKeysMatcher.group(MAX_KEYS));
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
		final List<T> buff = new ArrayList<>(maxCount);
		final T lastObj;
		try {
			lastObj = sharedStorage.listObjects(name, marker, buff, maxCount);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Bucket \"{}\": generated list of {} objects, last one is \"{}\"",
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
		resp.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_XML.getMimeType());
		//
		final Document doc = DOM_BUILDER.newDocument();
		final Element eRoot = doc.createElementNS(
			"http://s3.amazonaws.com/doc/2006-03-01/", "ListBucketResult"
		);
		doc.appendChild(eRoot);
		//
		Element e = doc.createElement("Name"), ee;
		e.appendChild(doc.createTextNode(name));
		eRoot.appendChild(e);
		e = doc.createElement("IsTruncated");
		e.appendChild(doc.createTextNode(Boolean.toString(lastObj != null)));
		eRoot.appendChild(e);
		e = doc.createElement("Prefix"); // TODO prefix support
		eRoot.appendChild(e);
		e = doc.createElement("MaxKeys");
		e.appendChild(doc.createTextNode(Integer.toString(buff.size())));
		eRoot.appendChild(e);
		//
		for(final T dataObject : buff) {
			e = doc.createElement("Contents");
			ee = doc.createElement("Key");
			ee.appendChild(doc.createTextNode(dataObject.getName()));
			e.appendChild(ee);
			ee = doc.createElement("Size");
			ee.appendChild(doc.createTextNode(Long.toString(dataObject.getSize())));
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
			LogUtil.exception(LOG, Level.ERROR, ex, "Failed to build bucket XML listing");
			return;
		}
		//
		resp.setEntity(new NByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_XML));
	}
}
