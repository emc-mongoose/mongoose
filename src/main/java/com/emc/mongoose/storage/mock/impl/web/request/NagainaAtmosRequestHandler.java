package com.emc.mongoose.storage.mock.impl.web.request;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.data.MutableDataItem;
import com.emc.mongoose.core.api.data.model.DataItemContainer;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.storage.mock.api.ContainerMockException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
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
public class NagainaAtmosRequestHandler<T extends WSObjectMock> extends NagainaRequestHandlerBase<T> {

	private final static Logger LOG = LogManager.getLogger();

	public NagainaAtmosRequestHandler(RunTimeConfig rtConfig, WSMock<T> sharedStorage) {
		super(rtConfig, sharedStorage);
	}

	@Override
	protected void handleActually(ChannelHandlerContext ctx) {

	}

	@Override
	protected void handleContainerList(String containerName, ChannelHandlerContext ctx) {

	}
}
