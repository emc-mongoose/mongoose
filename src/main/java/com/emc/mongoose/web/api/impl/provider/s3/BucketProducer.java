package com.emc.mongoose.web.api.impl.provider.s3;
//
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.web.api.WSIOTask;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.data.impl.BasicWSObject;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import com.emc.mongoose.web.load.WSLoadExecutor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
/**
 Created by kurila on 08.10.14.
 */
public final class BucketProducer<T extends WSObject, U extends WSObject>
extends Thread
implements Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile Consumer<T> consumer = null;
	private final Bucket<T> bucket;
	private final Constructor<T> dataConstructor;
	private final long maxCount;
	private final WSLoadExecutor<T> wsClient;
	//
	@SuppressWarnings("unchecked")
	public BucketProducer(
		final Bucket<T> bucket, final Class<U> dataCls, final long maxCount,
		final WSLoadExecutor<T> wsClient
	) throws ClassCastException, NoSuchMethodException {
		super("bucket-" + bucket.getName() + "-producer");
		this.bucket = bucket;
		this.dataConstructor = (Constructor<T>) dataCls.getConstructor(
			String.class, Long.class, Long.class
		);
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		this.wsClient = wsClient;
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
	public final void run() {
		try {
			final HttpResponse httpResp = bucket.execute(wsClient, WSIOTask.HTTPMethod.GET);
			if(httpResp != null) {
				final StatusLine statusLine = httpResp.getStatusLine();
				if(statusLine==null) {
					LOG.warn(Markers.MSG, "No response status");
				} else {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode == HttpStatus.SC_OK) {
						final HttpEntity respEntity = httpResp.getEntity();
						final String respContentType = respEntity.getContentType().getValue();
						if(ContentType.APPLICATION_XML.getMimeType().equals(respContentType)) {
							try {
								final SAXParser parser = SAXParserFactory
									.newInstance().newSAXParser();
								try(final InputStream in = respEntity.getContent()) {
									parser.parse(
										in,
										new BucketListHandler<>(consumer, dataConstructor, maxCount)
									);
								} catch(final SAXException e) {
									ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to parse");
								}
							} catch(final ParserConfigurationException | SAXException e) {
								ExceptionHandler.trace(
									LOG, Level.ERROR, e, "Failed to create SAX parser"
								);
							}
						} else {
							LOG.warn(
								Markers.MSG, "Unexpected response content type: \"{}\"",
								respContentType
							);
						}
					} else {
						final String statusMsg = statusLine.getReasonPhrase();
						LOG.debug(
							Markers.MSG, "Listing bucket \"{}\" response: {}/{}",
							bucket.getName(), statusCode, statusMsg
						);
					}
				}
				EntityUtils.consumeQuietly(httpResp.getEntity());
			}
		} catch(final IOException e) {
			ExceptionHandler.trace(
				LOG, Level.ERROR, e,
				String.format("Failed to list the bucket \"%s\"", bucket.getName())
			);
		}
	}
	//
}
