package com.emc.mongoose.storage.adapter.s3;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.GenericContainer;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
//
import com.emc.mongoose.core.impl.data.GenericContainerItemInputBase;
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.ListIterator;
/**
 Created by kurila on 03.07.15.
 */
public final class WSBucketItemInput<T extends WSObject>
extends GenericContainerItemInputBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final SAXParser parser;
	//
	private String nextPageMarker = null;
	private boolean eof = false;
	//
	public WSBucketItemInput(
		final WSBucketImpl<T> bucket, final String nodeAddr, final Class<T> itemCls
	) throws IllegalStateException {
		super(bucket, nodeAddr, itemCls);
		try {
			parser = SAXParserFactory.newInstance().newSAXParser();
		} catch(final ParserConfigurationException | SAXException e) {
			throw new IllegalStateException(e);
		}
	}
	//
	private final static class PageContentHandler<T extends WSObject>
	extends DefaultHandler {
		private final static String
			QNAME_ITEM = "Contents",
			QNAME_ITEM_ID = "Key",
			QNAME_ITEM_SIZE = "Size",
			QNAME_NEXT_MARKER = "NextMarker";
		//
		private final List<T> itemsBuffer;
		private final Constructor<T> itemConstructor;
		private final GenericContainer<T> container;
		private int count = 0;
		private boolean
			isInsideItem = false,
			itIsItemId = false,
			itIsItemSize = false,
			itIsNextMarker = false;
		private String strId = null, strSize = null, nextPageMarker = null;
		private T nextItem;
		//
		private PageContentHandler(
			final List<T> itemsBuffer, final Constructor<T> itemConstructor,
			final GenericContainer<T> container
		) {
			this.itemsBuffer = itemsBuffer;
			this.itemConstructor = itemConstructor;
			this.container = container;
		}
		//
		@Override
		public final void startElement(
			final String uri, final String localName, final String qName, Attributes attrs
		) throws SAXException {
			isInsideItem = isInsideItem || QNAME_ITEM.equals(qName);
			itIsItemId = isInsideItem && QNAME_ITEM_ID.equals(qName);
			itIsItemSize = isInsideItem && QNAME_ITEM_SIZE.equals(qName);
			//
			itIsNextMarker = QNAME_NEXT_MARKER.equals(qName);
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
				if(strId != null && strId.length() > 0 && size > -1) {
					try {
						nextItem = container.buildItem(itemConstructor, strId, size);
						if(nextItem != null) {
							itemsBuffer.add(nextItem);
							count ++;
						}
					} catch(final NumberFormatException e) {
						LOG.debug(Markers.ERR, "Invalid id: {}", strId);
					} catch(final Exception e) {
						LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
					}
				} else {
					LOG.trace(Markers.ERR, "Invalid object id ({}) or size ({})", strId, strSize);
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
				strId = new String(buff, start, length);
			} else if(itIsItemSize) {
				strSize = new String(buff, start, length);
			} else if(itIsNextMarker) {
				nextPageMarker = new String(buff, start, length);
			}
			super.characters(buff, start, length);
		}
		//
		public String getNextPageMarker() {
			return nextPageMarker;
		}
		//
		public int getCount() {
			return count;
		}
	}
	//
	@Override
	protected final ListIterator<T> getNextPageIterator()
	throws EOFException, IOException {
		if(eof) {
			throw new EOFException();
		}
		// execute the request
		final HttpResponse resp = WSBucketImpl.class.cast(container).execute(
			nodeAddr, MutableWSRequest.HTTPMethod.GET, false, nextPageMarker,
			WSRequestConfig.PAGE_SIZE
		);
		// response validation
		if(resp == null) {
			throw new IOException("No HTTP response is returned");
		}
		final StatusLine status = resp.getStatusLine();
		if(status == null) {
			throw new IOException("Invalid HTTP response: " + resp);
		}
		final int statusCode = status.getStatusCode();
		if(statusCode < 200 || statusCode > 300) {
			throw new IOException(
				"Listing bucket \"" + container + "\" response: " + status
			);
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
			final PageContentHandler<T> pageContentHandler = new PageContentHandler<>(
				listPageBuffer, itemConstructor, container
			);
			parser.parse(in, pageContentHandler);
			nextPageMarker = pageContentHandler.getNextPageMarker();
			if(null == nextPageMarker || 0 == pageContentHandler.getCount()) {
				eof = true; // end of bucket list
			}
			LOG.info("Listed {} items the last time", pageContentHandler.getCount());
		} catch(final SAXException e) {
			throw new IOException(e);
		}
		//
		return listPageBuffer.listIterator();
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
		nextPageMarker = null;
	}
}
