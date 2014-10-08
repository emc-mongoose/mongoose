package com.emc.mongoose.object.api.provider.s3;
//
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.api.provider.s3.Bucket;
import com.emc.mongoose.object.api.provider.s3.WSRequestConfigImpl;
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
//
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
//
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
/**
 Created by kurila on 02.10.14.
 */
public final class WSBucket<T extends WSObject>
implements Bucket<T>{
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	final WSRequestConfigImpl reqConf;
	final String name;
	//
	public WSBucket(final WSRequestConfigImpl reqConf, final String name) {
		this.reqConf = reqConf;
		//
		if(name == null || name.length() == 0) {
			final Date
				dt = Calendar.getInstance(
					TimeZone.getTimeZone("GMT+0"), Locale.ROOT
				).getTime();
			this.name = "mongoose-" + WSRequestConfig.FMT_DT.format(dt);
		} else {
			this.name = name;
		}
	}
	//
	@Override
	public final String getName() {
		return name;
	}
	//
	@Override
	public final boolean exists()
	throws IllegalStateException {
		boolean flagExists = false;
		//
		final HttpRequestBase checkReq = new HttpHead("/" + name);
		reqConf.applyHeadersFinally(checkReq);
		final CloseableHttpClient httpClient = reqConf.getClient();
		//
		if(httpClient == null) {
			throw new IllegalStateException("No HTTP client specified");
		}
		try(
			final CloseableHttpResponse httpResp = httpClient.execute(
				new HttpHost(reqConf.getAddr(), reqConf.getPort(), reqConf.getScheme()),
				checkReq
			)
		) {
			final StatusLine statusLine = httpResp.getStatusLine();
			if(statusLine == null) {
				LOG.warn(Markers.MSG, "No response status");
			} else {
				final int statusCode = statusLine.getStatusCode();
				if(statusCode == HttpStatus.SC_OK) {
					LOG.debug(Markers.MSG, "Bucket \"{}\" exists", name);
					flagExists = true;
				} else {
					final String statusMsg = statusLine.getReasonPhrase();
					LOG.debug(
						Markers.MSG, "Checking bucket \"{}\" response: {}/{}",
						name, statusCode, statusMsg
					);
				}
			}
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to check the bucket \""+name+"\"");
		}
		//
		return flagExists;
	}
	//
	@Override
	public final void create()
	throws IllegalStateException {
		//
		final HttpRequestBase createReq = new HttpPut("/" + name);
		reqConf.applyHeadersFinally(createReq);
		final CloseableHttpClient httpClient = reqConf.getClient();
		//
		if(httpClient == null) {
			throw new IllegalStateException("No HTTP client specified");
		}
		try(
			final CloseableHttpResponse httpResp = httpClient.execute(
				new HttpHost(reqConf.getAddr(), reqConf.getPort(), reqConf.getScheme()),
				createReq
			)
		) {
			final StatusLine statusLine = httpResp.getStatusLine();
			if(statusLine == null) {
				LOG.warn(Markers.MSG, "No response status");
			} else {
				final int statusCode = statusLine.getStatusCode();
				if(statusCode == HttpStatus.SC_OK) {
					LOG.info(Markers.MSG, "Bucket \"{}\" created", name);
				} else {
					final String statusMsg = statusLine.getReasonPhrase();
					LOG.warn(
						Markers.ERR, "Create bucket \"{}\" response: {}/{}",
						name, statusCode, statusMsg
					);
				}
			}
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to create the bucket \""+name+"\"");
		}
	}
	//
	@Override
	public final void delete()
	throws IllegalStateException {
		//
		final HttpRequestBase deleteReq = new HttpDelete("/" + name);
		reqConf.applyHeadersFinally(deleteReq);
		final CloseableHttpClient httpClient = reqConf.getClient();
		//
		if(httpClient == null) {
			throw new IllegalStateException("No HTTP client was specified");
		}
		try(
			final CloseableHttpResponse httpResp = httpClient.execute(
				new HttpHost(reqConf.getAddr(), reqConf.getPort(), reqConf.getScheme()),
				deleteReq
			)
		) {
			final StatusLine statusLine = httpResp.getStatusLine();
			if(statusLine == null) {
				LOG.warn(Markers.MSG, "No response status");
			} else {
				final int statusCode = statusLine.getStatusCode();
				if(statusCode == HttpStatus.SC_OK) {
					LOG.info(Markers.MSG, "Bucket \"{}\" deleted", name);
				} else {
					final String statusMsg = statusLine.getReasonPhrase();
					LOG.warn(
						Markers.ERR, "Delete bucket \"{}\" response: {}/{}",
						name, statusCode, statusMsg
					);
				}
			}
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to delete the bucket \""+name+"\"");
		}
		//
	}
	//
	@Override
	public final List<T> list() {
		final List<T> dataItems = Collections.emptyList();
		//
		final HttpRequestBase listReq = new HttpGet("/" + name);
		reqConf.applyHeadersFinally(listReq);
		final CloseableHttpClient httpClient = reqConf.getClient();
		//
		if(httpClient == null) {
			throw new IllegalStateException("No HTTP client specified");
		}
		try(
			final CloseableHttpResponse httpResp = httpClient.execute(
				new HttpHost(reqConf.getAddr(), reqConf.getPort(), reqConf.getScheme()),
				listReq
			)
		) {
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
								respEntity.getContent(),
								new DefaultHandler() {
									//
									private boolean insideKeyElement = false;
									//
									@Override
									public final void startElement(
										final String uri, final String localName,
										final String qName, final Attributes attrs
									) throws SAXException {
										if("key".equals(qName.toLowerCase())) {
											insideKeyElement = true;
										}
										super.startElement(uri, localName, qName, attrs);
									}
									//
									@Override
									public final void characters(
										final char chars[], final int start, final int length
									) throws SAXException {
										if(insideKeyElement) {
											LOG.info(
												Markers.MSG, "Data item: \"{}\"",
												new String(chars, start, length)
											);
										}
										super.characters(chars, start, length);
									}
									//
									@Override
									public final void endElement(
										final String uri, final String localName, final String qName
									) throws SAXException {
										if("key".equals(qName.toLowerCase())) {
											insideKeyElement = false;
										}
										super.endElement(uri, localName, qName);
									}
								}
							);
						} catch(final ParserConfigurationException | SAXException e) {
							ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to create SAX parser");
						}
					} else {
						LOG.warn(Markers.MSG, "Unexpected response content type: \"{}\"", respContentType);
					}
				} else {
					final String statusMsg = statusLine.getReasonPhrase();
					LOG.debug(
						Markers.MSG, "Listing bucket \"{}\" response: {}/{}",
						name, statusCode, statusMsg
					);
				}
			}
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to list the bucket \""+name+"\"");
		}
		//
		return dataItems;
	}
}
