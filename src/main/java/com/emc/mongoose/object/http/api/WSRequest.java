package com.emc.mongoose.object.http.api;
//
import com.emc.mongoose.api.Request;
import com.emc.mongoose.api.RequestConfig;
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.object.http.data.WSObject;
import com.emc.mongoose.object.http.impl.Read;
import com.emc.mongoose.pool.GenericInstancePool;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
//
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 06.06.14.
 */
public class WSRequest
implements Request<WSObject>, ResponseHandler<Request<WSObject>> {
	//
	private final static Logger LOG = LogManager.getLogger();
	public final static WSRequest POISON = new WSRequest();
	//
	private WSRequestConfig reqConf = null;
	private WSObject dataItem = null;
	private HttpRequestBase httpRequest = null;
	//
	private int statusCode = 0;
	private long start = 0, duration = 0;
	// BEGIN pool related things
	private final static ConcurrentHashMap<WSRequestConfig, GenericInstancePool<WSRequest>>
		POOL_MAP = new ConcurrentHashMap<>();
	//
	public static WSRequest getInstanceFor(final WSRequestConfig reqConf, final WSObject dataItem) {
		WSRequest request;
		if(dataItem==null) {
			request = POISON;
		} else {
			GenericInstancePool<WSRequest> pool;
			synchronized(POOL_MAP) {
				if(POOL_MAP.containsKey(reqConf)) {
					pool = POOL_MAP.get(reqConf);
				} else {
					pool = new GenericInstancePool<>(WSRequest.class);
					POOL_MAP.put(reqConf, pool);
				}
			}
			request = pool.take();
			if(request!=null) {
				request.setRequestConfig(reqConf).setDataItem(dataItem);
			}
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
	public WSRequest setRequestConfig(final RequestConfig<WSObject> reqConf) {
		if(this.reqConf==null) { // request instance has not been configured yet?
			this.reqConf = WSRequestConfig.class.cast(reqConf);
			switch(reqConf.getLoadType()) {
				case CREATE:	httpRequest = new HttpPut();	break;
				case READ:		httpRequest = new HttpGet();	break;
				case DELETE:	httpRequest = new HttpDelete();	break;
				case UPDATE:	httpRequest = new HttpPut();	break;
			}
		} else { // cleanup
			httpRequest.removeHeaders(HttpHeaders.RANGE);
		}
		return this;
	}
	//
	@Override
	public final WSObject getDataItem() {
		return dataItem;
	}
	//
	@Override
	public final WSRequest setDataItem(final WSObject dataItem) {
		try {
			reqConf.applyDataItem(httpRequest, dataItem);
			this.dataItem = dataItem;
		} catch(final URISyntaxException e) {
			LOG.warn(Markers.ERR, "Failed to calculate the request URI: {}", e.toString());
			if(LOG.isTraceEnabled()) {
				final Throwable cause = e.getCause();
				if(cause!=null) {
					LOG.trace(Markers.MSG, cause.toString(), cause.getCause());
				}
			}
		}
		return this;
	}
	//
	@Override
	public final Request<WSObject> call()
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
	public final WSRequest handleResponse(final HttpResponse httpResponse) {
		//
		final StatusLine statusLine = httpResponse.getStatusLine();
		if(statusLine==null) {
			LOG.warn(Markers.MSG, "No response status line");
		} else {
			statusCode = statusLine.getStatusCode();
			//
			if(LOG.isTraceEnabled()) {
				synchronized(LOG) {
					LOG.trace(
						Markers.MSG, "{} {} -> {}/{}",
						httpRequest.getMethod(), httpRequest.getURI(),
						statusCode, statusLine.getReasonPhrase()
					);
					for(final Header header : httpResponse.getAllHeaders()) {
						LOG.trace(Markers.MSG, "\t{}: {}", header.getName(), header.getValue());
					}
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
									if(LOG.isTraceEnabled()) {
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
