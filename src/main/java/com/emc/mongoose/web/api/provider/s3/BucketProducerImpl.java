package com.emc.mongoose.web.api.provider.s3;
//
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.web.data.WSObjectImpl;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
/**
 Created by kurila on 08.10.14.
 */
public final class BucketProducerImpl<T extends WSObjectImpl>
extends Thread
implements Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile Consumer<T> consumer = null;
	private final BucketImpl<T> bucket;
	//
	public BucketProducerImpl(final BucketImpl<T> bucket) {
		super("bucket-" + bucket.getName() + "-producer");
		this.bucket = bucket;
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
		try(final CloseableHttpResponse httpResp = bucket.execute("get")) {
			final StatusLine statusLine = httpResp.getStatusLine();
			if(statusLine == null) {
				LOG.warn(Markers.MSG, "No response status");
			} else {
				final int statusCode = statusLine.getStatusCode();
				if(statusCode == HttpStatus.SC_OK) {
					final HttpEntity respEntity = httpResp.getEntity();
					final String respContentType = respEntity.getContentType().getValue();
					if(ContentType.APPLICATION_XML.getMimeType().equals(respContentType)) {
						try {
							final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
							parser.parse(
								respEntity.getContent(), new BucketListHandler<>(consumer)
							);
						} catch(final ParserConfigurationException | SAXException e) {
							ExceptionHandler.trace(
								LOG, Level.ERROR, e, "Failed to create SAX parser"
							);
						}
					} else {
						LOG.warn(
							Markers.MSG, "Unexpected response content type: \"{}\"", respContentType
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
		} catch(final IOException e) {
			ExceptionHandler.trace(
				LOG, Level.ERROR, e, "Failed to list the bucket \""+bucket.getName()+"\""
			);
		}
	}
	//
}
