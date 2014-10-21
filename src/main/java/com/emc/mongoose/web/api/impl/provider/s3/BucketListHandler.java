package com.emc.mongoose.web.api.impl.provider.s3;
//
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.web.data.impl.BasicWSObject;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.rmi.RemoteException;
/**
 Created by kurila on 09.10.14.
 */
final class BucketListHandler<T extends BasicWSObject>
extends DefaultHandler {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String
		QNAME_ITEM = "Contents",
		QNAME_ITEM_ID = "Key",
		QNAME_ITEM_SIZE = "Size";
	//
	private final Consumer<T> consumer;
	@SuppressWarnings("FieldCanBeLocal")
	private volatile long count = 0;
	private volatile boolean
		isInsideItem = false,
		isInsideItemId = false,
		isInsideItemSize = false;
	private volatile String
		strId = null, strSize = null;
	//
	BucketListHandler(final Consumer<T> consumer) {
		this.consumer = consumer;
	}
	//
	@Override
	public final void startElement(
		final String uri, final String localName, final String qName, Attributes attrs
	) throws SAXException {
		isInsideItem = isInsideItem || QNAME_ITEM.equals(qName);
		isInsideItemId = isInsideItem && QNAME_ITEM_ID.equals(qName);
		isInsideItemSize = isInsideItem && QNAME_ITEM_SIZE.equals(qName);
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
			long size = 0;
			if(strSize != null && strSize.length() > 0) {
				try {
					size = Long.parseLong(strSize);
				} catch(final NumberFormatException e) {
					ExceptionHandler.trace(
						LOG, Level.WARN, e, "Data object size should be a 64 bit number"
					);
				}
			} else {
				LOG.trace(Markers.ERR, "No \"{}\" element or empty", QNAME_ITEM_SIZE);
			}
			//
			if(strId !=null && strId.length() > 0 && size > 0) {
				try {
					consumer.submit((T) new BasicWSObject(strId, size));
				} catch(final RemoteException e) {
					ExceptionHandler.trace(
						LOG, Level.WARN, e, "Failed to submit new data object to remote consumer"
					);
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
		}
		super.characters(buff, start, length);
	}
	//
	@Override
	public final void endDocument()
	throws SAXException {
		if(consumer != null) {
			try {
				consumer.setMaxCount(count);
			} catch(final RemoteException e) {
				ExceptionHandler.trace(
					LOG, Level.WARN, e, "Failed to limit data items count for remote consumer"
				);
			}
		}
		super.endDocument();
	}
}
