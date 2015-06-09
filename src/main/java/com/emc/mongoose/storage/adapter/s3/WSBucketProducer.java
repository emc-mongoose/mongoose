package com.emc.mongoose.storage.adapter.s3;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.data.WSObject;
// mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.logging.Markers;
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
		//
		HttpResponse httpResp = null;
		try {
			httpResp = bucket.execute(addr, MutableWSRequest.HTTPMethod.GET);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to list the bucket: " + bucket);
		}
		//
		if(httpResp != null) {
			final StatusLine statusLine = httpResp.getStatusLine();
			if(statusLine == null) {
				LOG.warn(Markers.MSG, "No response status returned");
			} else {
				final int statusCode = statusLine.getStatusCode();
				if(statusCode >= 200 && statusCode < 300) {
					final HttpEntity respEntity = httpResp.getEntity();
					if(respEntity != null) {
						String respContentType = ContentType.APPLICATION_XML.getMimeType();
						if(respEntity.getContentType() != null) {
							respContentType = respEntity.getContentType().getValue();
						} else {
							LOG.debug(Markers.ERR, "No content type returned");
						}
						if(ContentType.APPLICATION_XML.getMimeType().equals(respContentType)) {
							try {
								final SAXParser
									parser = SAXParserFactory.newInstance().newSAXParser();
								try(final InputStream in = respEntity.getContent()) {
									////////////////////////////////////////////////////////////////
									parser.parse(
										in,
										new XMLBucketListParser<>(
											consumer, dataConstructor, maxCount
										)
									);
									////////////////////////////////////////////////////////////////
								} catch(final SAXException e) {
									LogUtil.exception(LOG, Level.WARN, e, "Failed to parse");
								} catch(final IOException e) {
									LogUtil.exception(
										LOG, Level.ERROR, e,
										"Failed to read the bucket listing response content: {}",
										bucket
									);
								} finally {
									if(consumer != null) {
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
							} catch(final ParserConfigurationException | SAXException e) {
								LogUtil.exception(
									LOG, Level.ERROR, e, "Failed to create SAX parser"
								);
							}
						} else {
							LOG.warn(
								Markers.MSG, "Unexpected response content type: \"{}\"",
								respContentType
							);
						}
						EntityUtils.consumeQuietly(respEntity);
					}
				} else {
					final String statusMsg = statusLine.getReasonPhrase();
					LOG.warn(
						Markers.ERR, "Listing bucket \"{}\" response: {}/{}",
						bucket, statusCode, statusMsg
					);
				}
			}
		}
	}
	//
}
