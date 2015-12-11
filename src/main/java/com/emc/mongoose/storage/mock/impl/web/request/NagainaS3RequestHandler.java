package com.emc.mongoose.storage.mock.impl.web.request;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.data.MutableDataItem;
import com.emc.mongoose.core.api.data.model.ContainerHelper;
import com.emc.mongoose.core.api.io.conf.WSRequestConfig;
import com.emc.mongoose.storage.mock.api.ContainerMockException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
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

import static io.netty.channel.ChannelHandler.Sharable;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class NagainaS3RequestHandler<T extends WSObjectMock> extends NagainaRequestHandlerBase<T> {

	private final static Logger LOG = LogManager.getLogger();

	private final static DocumentBuilder DOM_BUILDER;
	private final static TransformerFactory TF = TransformerFactory.newInstance();

	static {
		try {
			DOM_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public NagainaS3RequestHandler(RunTimeConfig rtConfig, WSMock<T> sharedStorage) {
		super(rtConfig, sharedStorage);
	}

	@Override
	public void handleActually(ChannelHandlerContext ctx) {
		String method = ctx.attr(AttributeKey.<HttpRequest>valueOf(requestKey))
				.get().getMethod().toString().toUpperCase();
		String[] uriParams =
				getUriParams(ctx.attr(AttributeKey.<HttpRequest>valueOf(requestKey))
						.get().getUri(), 2);
		String containerName = uriParams[0];
		String objId = uriParams[1];
		Long size = ctx.attr(AttributeKey.<Long>valueOf(contentLengthKey)).get();
		ctx.attr(AttributeKey.<Boolean>valueOf(ctxWriteFlagKey)).set(true);
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
		if (ctx.attr(AttributeKey.<Boolean>valueOf(ctxWriteFlagKey)).get()) {
			HttpResponseStatus status = ctx.attr(AttributeKey.<HttpResponseStatus>valueOf(responseStatusKey)).get();
			FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status != null ? status : OK);
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
		ctx.attr(AttributeKey.<Boolean>valueOf(ctxWriteFlagKey)).set(false);
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content));
		response.headers().set(CONTENT_TYPE, ContentType.APPLICATION_XML.getMimeType());
		HttpHeaders.setContentLength(response, content.length);
		ctx.write(response);
	}

}
