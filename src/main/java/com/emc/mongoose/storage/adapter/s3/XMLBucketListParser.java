package com.emc.mongoose.storage.adapter.s3;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.data.WSObject;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
//
import java.lang.reflect.Constructor;
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 09.10.14.
 */
public final class XMLBucketListParser<T extends WSObject>
extends DefaultHandler {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String
		QNAME_ITEM = "Contents",
		QNAME_ITEM_ID = "Key",
		QNAME_ITEM_SIZE = "Size",
		QNAME_NEXT_MARKER = "NextMarker";
	//
	private final Consumer<T> consumer;
	private final Constructor<T> dataConstructor;
	private final long maxCount;
	private volatile long count = 0;
	private volatile boolean
		isInsideItem = false,
		isInsideItemId = false,
		isInsideItemSize = false,
		isNextMarker = false;
	private volatile String
		strId = null, strSize = null, bucketListingNextMarker = null;
	//
	XMLBucketListParser(
		final Consumer<T> consumer, final Constructor<T> dataConstructor, final long maxCount
	) {
		this.consumer = consumer;
		this.dataConstructor = dataConstructor;
		this.maxCount = maxCount;
	}
	//
	@Override
	public final void startElement(
		final String uri, final String localName, final String qName, Attributes attrs
	) throws SAXException {
		isInsideItem = isInsideItem || QNAME_ITEM.equals(qName);
		isInsideItemId = isInsideItem && QNAME_ITEM_ID.equals(qName);
		isInsideItemSize = isInsideItem && QNAME_ITEM_SIZE.equals(qName);
		//
		isNextMarker = QNAME_NEXT_MARKER.equals(qName);
		super.startElement(uri, localName, qName, attrs);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void endElement(
		final String uri, final String localName, final String qName
	) throws SAXException {
		//
		isInsideItemId = isInsideItemId && !QNAME_ITEM_ID.equals(qName);
		isInsideItemSize = isInsideItemSize && !QNAME_ITEM_SIZE.equals(qName);
		//
		if(isInsideItem && QNAME_ITEM.equals(qName)) {
			isInsideItem = false;
			//
			long offset, size = -1;
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
					offset = Long.parseLong(strId, DataObject.ID_RADIX);
					if(offset < 0) {
						LOG.warn(Markers.ERR, "Calculated from id ring offset is negative");
					} else if(count < maxCount) {
						consumer.submit(dataConstructor.newInstance(strId, offset, size));
						count ++;
					} else {
						endDocument();
					}
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to submit new data object to the consumer"
					);
				} catch(final  RejectedExecutionException e) {
					LogUtil.exception(LOG, Level.DEBUG, e, "Consumer rejected the data object");
				} catch(final InterruptedException e) {
					endDocument();
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
	public final void characters(
		final char buff[], final int start, final int length
	) throws SAXException {
		if(isInsideItemId) {
			strId = new String(buff, start, length);
		} else if(isInsideItemSize) {
			strSize = new String(buff, start, length);
		} else if(isNextMarker) {
			bucketListingNextMarker = new String(buff, start, length);
		}
		super.characters(buff, start, length);
	}
	//
	public final String getBucketListingNextMarker(){
		return bucketListingNextMarker;
	}
}
