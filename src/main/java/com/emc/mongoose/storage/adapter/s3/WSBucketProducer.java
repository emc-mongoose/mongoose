package com.emc.mongoose.storage.adapter.s3;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.data.WSObject;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
/**
 Created by kurila on 08.10.14.
 */
public final class WSBucketProducer<T extends WSObject>
extends Thread
implements Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile Consumer<T> consumer = null;
	private final WSBucketImpl<T> bucket;
	private final Constructor<T> dataConstructor;
	private final long maxCount;
	private final String addr;
	//
	@SuppressWarnings("unchecked")
	public WSBucketProducer(
		final WSBucketImpl<T> bucket, final Class<? extends WSObject> dataCls, final long maxCount,
		final String addr
	) throws ClassCastException, NoSuchMethodException {
		super("bucket-" + bucket + "-producer");
		this.bucket = bucket;
		this.dataConstructor = (Constructor<T>) dataCls.getConstructor(
			String.class, Long.class, Long.class
		);
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		this.addr = addr;
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer) {
		this.consumer = consumer;
	}
	//
	@Override
	public final Consumer<T> getConsumer() {
		return consumer;
	}
	//
	@Override
	public final void await()
	throws InterruptedException {
		join();
	}
	//
	@Override
	public final void await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException {
		timeUnit.timedJoin(this, timeOut);
	}
	//
	@Override
	public final void run() {
		String bucketListingMarker = null;
		long countSubmit = 0;
		long bucket_max_keys = WSRequestConfig.LIST_SIZE;
		try {
			do {
				//
				bucket_max_keys = (maxCount - countSubmit) > bucket_max_keys ?
					bucket_max_keys : (maxCount - countSubmit);
				//
				final HttpResponse httpResp = bucket.execute(
					addr, MutableWSRequest.HTTPMethod.GET, false,
					bucketListingMarker, bucket_max_keys
				);
				//
				if (httpResp == null) {
					LOG.warn(Markers.MSG, "No HTTP response is returned");
					break;
				}
				//
				final StatusLine statusLine = httpResp.getStatusLine();
				//
				if (statusLine == null) {
					LOG.warn(Markers.MSG, "No response status is returned");
					break;
				}
				//
				final int statusCode = statusLine.getStatusCode();
				//
				if (statusCode < 200 || statusCode > 300) {
					final String statusMsg = statusLine.getReasonPhrase();
					LOG.warn(
							Markers.ERR, "Listing bucket \"{}\" response: {}/{}",
							bucket, statusCode, statusMsg
					);
					break;
				}
				//
				final HttpEntity respEntity = httpResp.getEntity();
				if (respEntity == null) {
					LOG.warn(Markers.ERR, "No HTTP entity is returned");
					break;
				}
				//
				//String respContentType = ContentType.APPLICATION_XML.getMimeType();
				//
				if (respEntity.getContentType() == null) {
					LOG.debug(Markers.ERR, "No content type is returned");
					break;
				}
				//
				final String respContentType = respEntity.getContentType().getValue();
				//
				if (!ContentType.APPLICATION_XML.getMimeType().equals(respContentType)) {
					LOG.warn(
							Markers.MSG, "Unexpected response content type: \"{}\"",
							respContentType
					);
					break;
				}
				//
				final SAXParser
					parser = SAXParserFactory.newInstance().newSAXParser();
				try (final InputStream in = respEntity.getContent()) {
					////////////////////////////////////////////////////////////////
					final XMLBucketListParser xmlBucketListparser = new XMLBucketListParser<>(
						consumer, dataConstructor, maxCount
					);
					//
					parser.parse(in, xmlBucketListparser);
					bucketListingMarker = xmlBucketListparser.getBucketListingNextMarker();
					countSubmit = xmlBucketListparser.getCountSubmit();
					////////////////////////////////////////////////////////////////
				}
				EntityUtils.consumeQuietly(respEntity);
			} while (bucketListingMarker != null);
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to list the bucket: " + bucket + ", next marker: " + bucketListingMarker
			);
		} catch(final SAXException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to parse"
			);
		} catch(final ParserConfigurationException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to create SAX parser"
			);
		} finally {
			if (consumer != null) {
				try {
					consumer.shutdown();
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.WARN, e,
						"Failed to limit data items count for remote consumer"
					);
				} finally {
					consumer = null;
				}
			}
		}
	}
	//
}
