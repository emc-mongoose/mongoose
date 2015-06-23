package com.emc.mongoose.storage.adapter.swift;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.data.WSObject;
//
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 04.03.15.
 */
public final class WSContainerProducer<T extends WSObject>
extends Thread
implements Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static JsonFactory JSON_FACTORY = new JsonFactory();
	//
	private volatile Consumer<T> consumer = null;
	private final WSContainerImpl<T> container;
	private final Constructor<T> dataConstructor;
	private final long maxCount;
	private final String addr;
	//
	@SuppressWarnings("unchecked")
	public WSContainerProducer(
		final WSContainerImpl<T> container, final Class<? extends WSObject> dataCls,
		final long maxCount, final String addr
	) throws ClassCastException, NoSuchMethodException {
		super("container-" + container + "-producer");
		this.container = container;
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
		int statusCode;
		try {
			do {
				final HttpResponse httpResp = container.execute(addr, MutableWSRequest.HTTPMethod.GET, lastId);
				//
				if (httpResp == null) {
					LOG.warn(Markers.MSG, "No HTTP response returned");
					break;
				}
				//
				final StatusLine statusLine = httpResp.getStatusLine();
				//
				if (statusLine == null) {
					LOG.warn(Markers.MSG, "No response status returned");
					break;
				}
				//
				statusCode = statusLine.getStatusCode();
				//
				if (statusCode < 200 || statusCode > 300) {
					final String statusMsg = statusLine.getReasonPhrase();
					LOG.warn(
							Markers.ERR, "Listing container \"{}\" response: {}/{}",
							container, statusCode, statusMsg
					);
					break;
				}
				//
				final HttpEntity respEntity = httpResp.getEntity();
				//
				if (respEntity == null) {
					LOG.warn(Markers.MSG, "No HTTP entity returned");
					break;
				}
				//
				//String respContentType = ContentType.APPLICATION_JSON.getMimeType();
				//
				if (respEntity.getContentType() == null) {
					LOG.debug(Markers.ERR, "No content type returned");
					break;
				}
				//
				final String respContentType = respEntity.getContentType().getValue();
				if (!respContentType.toLowerCase().contains("json")) {
					LOG.warn(
							Markers.ERR, "Unexpected response content type: \"{}\"",
							respContentType
					);
					break;
				}
				try (final InputStream in = respEntity.getContent()) {
					handleJsonInputStream(in);
				} catch (final IOException e) {
					LogUtil.exception(
							LOG, Level.ERROR, e,
							"Failed to list the content of container: {}", container
					);
				}
				EntityUtils.consumeQuietly(respEntity);
			} while (statusCode != 204 && lastId != null);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to list the container: {}", container);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Container \"{}\" producer interrupted", container);
		} finally {
			if (consumer != null) {
				try {
					consumer.shutdown();
				} catch(final Exception e) {
					LogUtil.exception(LOG, Level.DEBUG, e, "Failed to shutdown the consumer");
				} finally {
					consumer = null;
				}
			}
		}
	}
	//
	private final static String KEY_SIZE = "bytes", KEY_ID = "name";
	private boolean isInsideObjectToken = false;
	private String lastId = null;
	private long lastSize = -1, offset, count = 0;
	//
	private void handleJsonInputStream(final InputStream in)
	throws IOException, InterruptedException {
		int containerListLenght = 0;
		try(final JsonParser jsonParser = JSON_FACTORY.createParser(in)) {
			final JsonToken rootToken = jsonParser.nextToken();
			JsonToken nextToken;
			T nextDataItem;
			if(JsonToken.START_ARRAY.equals(rootToken)) {
				do {
					nextToken = jsonParser.nextToken();
					switch(nextToken) {
						case START_OBJECT:
							if(isInsideObjectToken) {
								LOG.debug(Markers.ERR, "Looks like the json response is not plain");
							}
							isInsideObjectToken = true;
							break;
						case END_OBJECT:
							if(isInsideObjectToken) {
								if(lastId != null && lastSize > -1) {
									try {
										offset = Long.parseLong(lastId, DataObject.ID_RADIX);
										if(offset < 0) {
											LOG.warn(
												Markers.ERR,
												"Calculated from id ring offset is negative"
											);
										} else if(count < maxCount) {
											nextDataItem = dataConstructor
												.newInstance(lastId, offset, lastSize);
											consumer.submit(nextDataItem);
											if(LOG.isTraceEnabled(Markers.MSG)) {
												LOG.trace(
													Markers.MSG, "Submitted \"{}\" to consumer",
													nextDataItem
												);
											}
											count ++;
											containerListLenght ++;
										} else {
											break;
										}
									} catch(
										final InstantiationException | IllegalAccessException |
											InvocationTargetException e
									) {
										LogUtil.exception(
											LOG, Level.WARN, e,
											"Failed to create data item descriptor"
										);
									} catch(final RemoteException e) {
										LogUtil.exception(
											LOG, Level.WARN, e,
											"Failed to submit new data object to the consumer"
										);
									} catch(final NumberFormatException e) {
										LOG.debug(Markers.ERR, "Invalid id: {}", lastId);
									} catch(final RejectedExecutionException e) {
										LOG.debug(
											Markers.ERR, "Consumer {} rejected the data item",
											consumer
										);
									} catch(final InterruptedException e) {
										LOG.debug(Markers.MSG, "Interrupted");
										break;
									}
								} else {
									LOG.trace(
										Markers.ERR, "Invalid object id ({}) or size ({})",
										lastId, lastSize
									);
								}
							} else {
								LOG.debug(Markers.ERR, "End of json object is not inside object");
							}
							isInsideObjectToken = false;
							break;
						case FIELD_NAME:
							if(KEY_SIZE.equals(jsonParser.getCurrentName())) {
								lastSize = jsonParser.nextLongValue(-1);
							}
							if(KEY_ID.equals(jsonParser.getCurrentName())) {
								lastId = jsonParser.nextTextValue();
							}
							break;
						case VALUE_NUMBER_INT:
						case VALUE_STRING:
						case VALUE_NULL:
						case VALUE_FALSE:
						case VALUE_NUMBER_FLOAT:
						case VALUE_TRUE:
						case VALUE_EMBEDDED_OBJECT:
						case NOT_AVAILABLE:
						default:
							break;
					}
				} while(!JsonToken.END_ARRAY.equals(nextToken));
				// if container's list is empty last data ID has to equal null.
				if (containerListLenght == 0) {
					lastId = null;
				}
			} else {
				LOG.warn(
					Markers.ERR,
					"Response contains root JSON token \"{}\", but array token was expected"
				);
			}
		}
	}
}
