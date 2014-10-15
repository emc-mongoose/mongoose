package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestBase;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.object.api.provider.atmos.WSRequestConfigImpl;
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import com.emc.mongoose.util.pool.GenericInstancePool;
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
/**
 Created by kurila on 06.06.14.
 */
public class WSRequestImpl<T extends WSObject>
extends RequestBase<T>
implements WSRequest<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static WSRequestImpl POISON = new WSRequestImpl() {
		@Override
		public final void execute()
		throws InterruptedException {
			throw new InterruptedException("Attempted to eat the poison");
		}
	};
	//
	protected WSRequestConfig<T> wsReqConf = null; // overrides RequestBase.reqConf field
	protected HttpRequestBase httpRequest = null;
	public WSRequestImpl() {
		super();
	}
	//
	@SuppressWarnings("unchecked")
	public static Request getInstanceFor(
		final RequestConfig reqConf, final DataItem dataItem
	) {
		Request request;
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
			request = RequestBase.class.cast(pool.take())
				.setRequestConfig(reqConf)
				.setDataItem(dataItem);
			//assert request != null;
		}
		return request;
	}
	//
	@Override
	public WSRequest<T> setRequestConfig(final RequestConfig<T> reqConf) {
		if(this.wsReqConf == null) { // request instance has not been configured yet?
			this.wsReqConf = (WSRequestConfig<T>) reqConf;
			switch(wsReqConf.getLoadType()) {
				case CREATE:
					httpRequest = com.emc.mongoose.object.api.provider.atmos.WSRequestConfigImpl.class.isInstance(wsReqConf) ?
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
			httpRequest.removeHeaders(WSRequestConfig.KEY_EMC_SIG);
		}
		super.setRequestConfig(reqConf);
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final WSRequest<T> setDataItem(final T dataItem) {
		try {
			wsReqConf.applyDataItem(httpRequest, dataItem);
			super.setDataItem(dataItem);
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to apply data item");
		}
		return this;
	}
	//
	@Override
	public void execute()
	throws InterruptedException, IOException {
		wsReqConf.applyHeadersFinally(httpRequest);
		final CloseableHttpClient httpClient = wsReqConf.getClient();
		httpClient.execute(httpRequest, this);
	}
	//
	@Override
	public final WSRequest<T> handleResponse(final HttpResponse httpResponse) {
		//
		final StatusLine statusLine = httpResponse.getStatusLine();
		if(statusLine==null) {
			LOG.warn(Markers.MSG, "No response status line");
		} else {
			final int statusCode = statusLine.getStatusCode();
			//
			if(LOG.isTraceEnabled(Markers.MSG)) {
				synchronized(LOG) {
					LOG.trace(
						Markers.MSG, "{}/{} <- {} {}", statusCode, statusLine.getReasonPhrase(),
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
						result = Result.SUCC;
						break;
					case (HttpGet.METHOD_NAME):
						if(wsReqConf.getVerifyContentFlag()) { // validate the response content
							final HttpEntity httpEntity = httpResponse.getEntity();
							if(httpEntity==null) {
								LOG.warn(
									Markers.ERR, "No HTTP content entity for request \"{}\"",
									httpRequest.getRequestLine()
								);
								result = Result.FAIL_IO;
								break;
							}
							try(final InputStream in = httpEntity.getContent()) {
								if(dataItem.compareWith(in)) {
									if(LOG.isTraceEnabled(Markers.MSG)) {
										LOG.trace(
											Markers.MSG, "Content verification success for \"{}\"",
											dataItem
										);
									}
									result = Result.SUCC;
								} else {
									LOG.warn(
										Markers.ERR, "Content verification failed for \"{}\"",
										dataItem
									);
									result = Result.FAIL_CORRUPT;
								}
							} catch(final IOException e) {
								LOG.warn(
									Markers.ERR, "Failed to read the object content for \"{}\"",
									dataItem
								);
								result = Result.FAIL_IO;
							}
						} else {
							result = Result.SUCC;
						}
						break;
					case (HttpPut.METHOD_NAME):
					case (HttpPost.METHOD_NAME):
						if(dataItem != null && dataItem.getId() == null) {
							wsReqConf.applyObjectId(dataItem, httpResponse);
						}
						result = Result.SUCC;
						break;
				}
			} else {
				switch(statusCode) {
					case(400):
						LOG.warn(Markers.ERR, "Incorrect request: \"{}\"", httpRequest.getRequestLine());
						result = Result.FAIL_CLIENT;
						break;
					case(403):
						LOG.warn(Markers.ERR, "Access failure");
						result = Result.FAIL_AUTH;
						break;
					case(404):
						LOG.warn(Markers.ERR, "Not found: {}", httpRequest.getURI());
						result = Result.FAIL_NOT_FOUND;
						break;
					case(416):
						LOG.warn(Markers.ERR, "Incorrect range");
						if(LOG.isTraceEnabled(Markers.ERR)) {
							for(final Header rangeHeader: httpRequest.getHeaders(HttpHeaders.RANGE)) {
								LOG.trace(
									Markers.ERR, "Incorrect range \"{}\" for data item: \"{}\"",
									rangeHeader.getValue(), dataItem
								);
							}
						}
						result = Result.FAIL_CLIENT;
						break;
					case(500):
						LOG.warn(Markers.ERR, "Storage internal failure");
						result = Result.FAIL_SVC;
						break;
					case(503):
						LOG.warn(Markers.ERR, "Storage prays about a mercy");
						result = Result.FAIL_SVC;
						break;
					default:
						LOG.warn(Markers.ERR, "Response code: {}", result);
						result = Result.FAIL_UNKNOWN;
				}
				if(LOG.isDebugEnabled(Markers.ERR)) {
					try(ByteArrayOutputStream bOutPut = new ByteArrayOutputStream()) {
						httpResponse.getEntity().writeTo(bOutPut);
						final String errMsg = bOutPut.toString();
						LOG.debug(
							Markers.ERR, "{}, cause request: {}/{}", errMsg, hashCode(), dataItem
						);
					} catch(final IOException e) {
						ExceptionHandler.trace(
							LOG, Level.ERROR, e,
							"Failed to fetch the content of the failed response"
						);
					}
				}
			}
		}
		//
		return this;
	}
	//
}
