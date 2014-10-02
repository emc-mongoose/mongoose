package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.api.provider.atmos.WSRequestConfigImpl;
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.object.load.type.ws.Read;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.pool.GenericInstancePool;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
//
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 06.06.14.
 */
public class WSRequestImpl<T extends WSObject>
implements WSRequest<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	public final static WSRequest POISON = new WSRequestImpl();
	//
	private WSRequestConfig<T> reqConf = null;
	private T dataItem = null;
	private HttpRequestBase httpRequest = null;
	//
	private int statusCode = 0;
	private long start = 0, duration = 0;
	// BEGIN pool related things
	private final static ConcurrentHashMap<WSRequestConfig, GenericInstancePool<WSRequest>>
		POOL_MAP = new ConcurrentHashMap<>();
	//
	@SuppressWarnings("unchecked")
	public static WSRequest getInstanceFor(
		final WSRequestConfig reqConf, final WSObject dataItem
	) {
		WSRequest request;
		if(dataItem == null) {
			request = POISON;
		} else {
			GenericInstancePool pool;
			synchronized(POOL_MAP) {
				if(POOL_MAP.containsKey(reqConf)) {
					pool = POOL_MAP.get(reqConf);
				} else {
					pool = new GenericInstancePool<>(WSRequestImpl.class);
					POOL_MAP.put(reqConf, pool);
				}
			}
			request = WSRequest.class.cast(pool.take())
				.setRequestConfig(reqConf)
				.setDataItem(dataItem);
			//assert request != null;
		}
		return request;
	}
	//
	@Override
	public final void close() {
		final GenericInstancePool<WSRequest> pool = POOL_MAP.get(reqConf);
		pool.release(this);
	}
	// END pool related things
	@Override
	public WSRequest<T> setRequestConfig(final RequestConfig<T> reqConf) {
		if(this.reqConf == null) { // request instance has not been configured yet?
			this.reqConf = (WSRequestConfig<T>) reqConf;
			switch(reqConf.getLoadType()) {
				case CREATE:
					httpRequest = WSRequestConfigImpl.class.getSimpleName().equals(reqConf.getAPI()) ?
						new HttpPost() : new HttpPut();
					break;
				case READ:
					httpRequest = new HttpGet();
					break;
				case DELETE:
					httpRequest = new HttpDelete();
					break;
				case UPDATE:
					httpRequest = new HttpPut();
					break;
				case APPEND:
					httpRequest = new HttpPut();
					break;
			}
		} else { // cleanup
			httpRequest.removeHeaders(HttpHeaders.RANGE);
		}
		return this;
	}
	//
	@Override
	public final T getDataItem() {
		return dataItem;
	}
	//
	@Override
	public final WSRequest<T> setDataItem(final T dataItem) {
		try {
			reqConf.applyDataItem(httpRequest, dataItem);
			this.dataItem = dataItem;
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to apply data item");
		}
		return this;
	}
	//
	@Override
	public final WSRequest<T> call()
	throws InterruptedException, IOException {
		if(dataItem==null) {
			throw new InterruptedException();
		}
		reqConf.applyHeadersFinally(httpRequest);
		final CloseableHttpClient httpClient = reqConf.getClient();
		start = System.nanoTime();
		return httpClient.execute(httpRequest, this);
	}
	//
	@Override
	public final long getStartNanoTime() {
		return start;
	}
	//
	@Override
	public final int getStatusCode() {
		return statusCode;
	}
	//
	@Override
	public final long getDuration() {
		return duration;
	}
	//
	@Override
	public final WSRequest<T> handleResponse(final HttpResponse httpResponse) {
		//
		final StatusLine statusLine = httpResponse.getStatusLine();
		if(statusLine==null) {
			LOG.warn(Markers.MSG, "No response status line");
		} else {
			statusCode = statusLine.getStatusCode();
			//
			if(LOG.isTraceEnabled(Markers.MSG)) {
				synchronized(LOG) {
					LOG.trace(
						Markers.MSG, "{}/{} <- {} {}",
						statusCode, statusLine.getReasonPhrase(),
						httpRequest.getMethod(), httpRequest.getURI()
					);
					//for(final Header header : httpResponse.getAllHeaders()) {
					//	LOG.trace(Markers.MSG, "\t{}: {}", header.getName(), header.getValue());
					//}
				}
			}
			//
			if(statusCode < 300) {
				switch(httpRequest.getMethod()) {
					case (HttpDelete.METHOD_NAME):
						break;
					case (HttpGet.METHOD_NAME):
						if(Read.VERIFY_CONTENT) { // validate the response content
							final HttpEntity httpEntity = httpResponse.getEntity();
							if(httpEntity==null) {
								LOG.warn(
									Markers.ERR, "No HTTP content entity for request \"{}\"",
									httpRequest.getRequestLine()
								);
								statusCode = 500;
								break;
							}
							try(final InputStream in = httpEntity.getContent()) {
								if(dataItem.compareWith(in)) {
									if(LOG.isTraceEnabled(Markers.MSG)) {
										LOG.trace(
											Markers.MSG, "Content verification success for \"{}\"",
											Long.toHexString(dataItem.getId())
										);
									}
								} else {
									LOG.warn(
										Markers.ERR, "Content verification failed for \"{}\"",
										Long.toHexString(dataItem.getId())
									);
									statusCode = 512;
								}
							} catch(final IOException e) {
								LOG.warn(
									Markers.ERR, "Failed to read the object content for \"{}\"",
									Long.toHexString(dataItem.getId())
								);
								statusCode = 500;
							}
						}
						break;
					case (HttpPut.METHOD_NAME):
						break;
					case (HttpPost.METHOD_NAME):
						break;
				}
			} else {
				switch(statusCode) {
					case(400):
						LOG.warn(Markers.ERR, "Incorrect request: \"{}\"", httpRequest.getRequestLine());
						break;
					case(403):
						LOG.warn(Markers.ERR, "Access failure");
						try(final InputStream respStream = httpResponse.getEntity().getContent()) {
							final ByteArrayOutputStream byteBuffStream = new ByteArrayOutputStream();
							int nextByte;
							do {
								nextByte = respStream.read();
								byteBuffStream.write(nextByte);
							} while(nextByte >= 0);
							LOG.debug(Markers.ERR, byteBuffStream.toString());
						} catch(final IOException|NullPointerException e) {
							LOG.debug(
								Markers.ERR, "Failed to read response content entity due to {}",
								e.toString()
							);
						}
						break;
					case(404):
						LOG.warn(Markers.ERR, "Not found: {}", httpRequest.getURI());
						break;
					case(416):
						LOG.warn(Markers.ERR, "Incorrect range");
						if(LOG.isTraceEnabled(Markers.ERR)) {
							for(final Header rangeHeader: httpRequest.getHeaders(HttpHeaders.RANGE)) {
								LOG.trace(
									Markers.ERR, "Incorrect range: {}, data item: \"{}\", size: {}",
									rangeHeader.getValue(),
									Long.toHexString(dataItem.getId()), dataItem.getSize()
								);
							}
						}
						break;
					case(500):
						LOG.warn(Markers.ERR, "Storage internal failure");
						break;
					case(503):
						LOG.warn(Markers.ERR, "Storage prays about a mercy");
						break;
					default:
						LOG.warn(Markers.ERR, "Response code: {}", statusCode);
				}
				if(LOG.isTraceEnabled(Markers.ERR)) {
					final ByteArrayOutputStream bOutPut = new ByteArrayOutputStream();
					try {
						httpResponse.getEntity().writeTo(bOutPut);
					} catch(final IOException e) {
						LOG.warn(
							Markers.ERR, "Failed to fetch the content of the failed response", e
						);
					}
					final String errMsg = bOutPut.toString();
					LOG.trace(
						Markers.ERR, "{}, cause request: {}/{}",
						errMsg, hashCode(), Long.toHexString(dataItem.getId())
					);
				}
			}
		}
		//
		duration = System.nanoTime() - start;
		LOG.info(
			Markers.PERF_TRACE, "{},{},{},{}",
			Long.toHexString(dataItem.getId()), statusCode, start, duration
		);
		//
		return this;
	}
	//
}
