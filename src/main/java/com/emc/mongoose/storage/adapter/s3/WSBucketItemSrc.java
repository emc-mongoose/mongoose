package com.emc.mongoose.storage.adapter.s3;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.data.ContainerHelper;
//
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.impl.item.data.GenericContainerItemSrcBase;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
//
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.07.15.
 */
public final class WSBucketItemSrc<T extends HttpDataItem, C extends Container<T>>
extends GenericContainerItemSrcBase<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final SAXParser parser;
	private final String nodeAddr;
	//
	private boolean eof = false;
	private long doneCount = 0;
	//
	public WSBucketItemSrc(
		final BucketHelper<T, C> bucket, final String nodeAddr, final Class<T> itemCls,
		final long maxCount
	) throws IllegalStateException {
		super(bucket, itemCls, maxCount);
		this.nodeAddr = nodeAddr;
		try {
			parser = SAXParserFactory.newInstance().newSAXParser();
		} catch(final ParserConfigurationException | SAXException e) {
			throw new IllegalStateException(e);
		}
	}
	//
	private final static class PageContentHandler<T extends HttpDataItem, C extends Container<T>>
	extends DefaultHandler {
		private final static String
			QNAME_ITEM = "Contents",
			QNAME_ITEM_ID = "Key",
			QNAME_ITEM_SIZE = "Size",
			QNAME_IS_TRUNCATED = "IsTruncated";
		//
		private final List<T> itemsBuffer;
		private final Constructor<T> itemConstructor;
		private final ContainerHelper<T, C> containerHelper;
		private int count = 0;
		private boolean
			isInsideItem = false,
			itIsItemId = false,
			itIsItemSize = false,
			itIsTruncateFlag = false,
			isTruncated = false;
		private String oid = null, strSize = null;
		private T nextItem;
		//
		private PageContentHandler(
			final List<T> itemsBuffer, final Constructor<T> itemConstructor,
			final ContainerHelper<T, C> containerHelper
		) {
			this.itemsBuffer = itemsBuffer;
			this.itemConstructor = itemConstructor;
			this.containerHelper = containerHelper;
		}
		//
		@Override
		public final void startElement(
			final String uri, final String localName, final String qName, Attributes attrs
		) throws SAXException {
			isInsideItem = isInsideItem || QNAME_ITEM.equals(qName);
			itIsItemId = isInsideItem && QNAME_ITEM_ID.equals(qName);
			itIsItemSize = isInsideItem && QNAME_ITEM_SIZE.equals(qName);
			itIsTruncateFlag = QNAME_IS_TRUNCATED.equals(qName);
			super.startElement(uri, localName, qName, attrs);
		}
		//
		@Override @SuppressWarnings("unchecked")
		public final void endElement(
			final String uri, final String localName, final String qName
		) throws SAXException {
			//
			itIsItemId = itIsItemId && !QNAME_ITEM_ID.equals(qName);
			itIsItemSize = itIsItemSize && !QNAME_ITEM_SIZE.equals(qName);
			itIsTruncateFlag = itIsTruncateFlag && !QNAME_IS_TRUNCATED.equals(qName);
			//
			if(isInsideItem && QNAME_ITEM.equals(qName)) {
				isInsideItem = false;
				//
				long size = -1;
				//
				if(strSize != null && strSize.length() > 0) {
					try {
						size = Long.parseLong(strSize);
					} catch(final NumberFormatException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Data object size should be a 64 bit number"
						);
					}
				} else {
					LOG.trace(Markers.ERR, "No \"{}\" element or empty", QNAME_ITEM_SIZE);
				}
				//
				if(oid != null && oid.length() > 0 && size > -1) {
					try {
						nextItem = containerHelper.buildItem(itemConstructor, oid, size);
						if(nextItem != null) {
							itemsBuffer.add(nextItem);
							count ++;
						}
					} catch(final NumberFormatException e) {
						LOG.debug(Markers.ERR, "Invalid id: {}", oid);
					} catch(final Exception e) {
						LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
					}
				} else {
					LOG.trace(Markers.ERR, "Invalid object id ({}) or size ({})", oid, strSize);
				}
			}
			//
			super.endElement(uri, localName, qName);
		}
		//
		@Override
		public final void characters(final char buff[], final int start, final int length)
		throws SAXException {
			if(itIsItemId) {
				oid = new String(buff, start, length);
			} else if(itIsItemSize) {
				strSize = new String(buff, start, length);
			} else if(itIsTruncateFlag) {
				isTruncated = Boolean.parseBoolean(new String(buff, start, length));
			}
			super.characters(buff, start, length);
		}
	}
	//
	@Override
	protected final void loadNextPage()
	throws EOFException, IOException {
		final int countLimit = (int) Math.min(
			ContainerHelper.DEFAULT_PAGE_SIZE, maxCount - doneCount
		);
		if(eof || countLimit == 0) {
			throw new EOFException();
		}
		// execute the request
		final HttpResponse resp = HttpBucketHelper.class.cast(containerHelper).execute(
			nodeAddr, HttpRequestConfig.METHOD_GET, lastItemId, countLimit,
			HttpRequestConfig.REQUEST_WITH_PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS
		);
		if(resp == null) {
			throw new IllegalStateException("No HTTP response");
		}
		// response validation
		final StatusLine status = resp.getStatusLine();
		if(status == null) {
			throw new IOException("Invalid HTTP response: " + resp);
		}
		final int statusCode = status.getStatusCode();
		if(statusCode < 200 || statusCode > 300) {
			throw new IOException("Listing bucket \"" + containerHelper + "\" response: " + status);
		}
		final HttpEntity respEntity = resp.getEntity();
		if(respEntity == null) {
			throw new IOException("No HTTP entity in the response: " + resp);
		}
		final String respContentType = respEntity.getContentType().getValue();
		if(!ContentType.APPLICATION_XML.getMimeType().equals(respContentType)) {
			LOG.warn(
				Markers.ERR, "Unexpected response content type: \"{}\"", respContentType
			);
		}
		// parse the response content
		parser.reset();
		try(final InputStream in = respEntity.getContent()) {
			final PageContentHandler<T, C> pageContentHandler = new PageContentHandler<>(
				items, itemConstructor, containerHelper
			);
			parser.parse(in, pageContentHandler);
			lastItemId = pageContentHandler.oid;
			if(!pageContentHandler.isTruncated || lastItemId == null) {
				eof = true; // end of bucket list
			}
			doneCount += pageContentHandler.count;
			LOG.debug(
				Markers.MSG, "Listed {} items the last time, response code: {}, last oid: {}",
				pageContentHandler.count, statusCode, lastItemId
			);
		} catch(final SAXException e) {
			e.printStackTrace(System.out);
			throw new IOException(e);
		}
	}
	//
	/**
	 Read the bucket from the beginning
	 @throws IOException
	 */
	@Override
	public final void reset()
	throws IOException {
		super.reset();
		eof = false;
		lastItemId = null;
	}
}
