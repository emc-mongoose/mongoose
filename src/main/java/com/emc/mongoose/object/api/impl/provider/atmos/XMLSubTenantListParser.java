package com.emc.mongoose.object.api.impl.provider.atmos;
//
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.object.data.WSObject;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.lang.reflect.Constructor;
import java.rmi.RemoteException;
/**
 Created by kurila on 23.01.15.
 */
public final class XMLSubTenantListParser<T extends WSObject>
extends DefaultHandler {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Consumer<T> consumer;
	private final Constructor<T> dataConstructor;
	private final long maxCount;
	private volatile long count = 0;
	//
	XMLSubTenantListParser(
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
		// TODO
		super.startElement(uri, localName, qName, attrs);
	}
	@Override
	public final void endElement(
		final String uri, final String localName, final String qName
	) throws SAXException {
		// TODO
		super.endElement(uri, localName, qName);
	}
	//
	@Override
	public final void characters(
		final char buff[], final int start, final int length
	) throws SAXException {
		// TODO
		super.characters(buff, start, length);
	}
	//
	@Override
	public final void endDocument()
	throws SAXException {
		LOG.debug(Markers.MSG, "End of bucket listing, got {} items", count);
		if(consumer != null) {
			try {
				consumer.shutdown();
			} catch(final RemoteException e) {
				TraceLogger.failure(
					LOG, Level.WARN, e, "Failed to limit data items count for remote consumer"
				);
			}
		}
		super.endDocument();
	}
}
