package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfigImpl;
import com.emc.mongoose.base.data.DataSource;
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 09.06.14.
 */
public abstract class WSRequestConfigBase<T extends WSObject>
extends RequestConfigImpl<T>
implements WSRequestConfig<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	public final static long serialVersionUID = 42L;
	private final String userAgent, signMethod;
	//
	private final HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
		//
		private final String FMT_ERR_MSG = "Request failed, try #%d";
		//
		@Override
		public final boolean retryRequest(
			final IOException e, final int i, final HttpContext httpContext
		) {
			if(LOG.isTraceEnabled(Markers.ERR)) {
				ExceptionHandler.trace(
					LOG, Level.TRACE, e, String.format(Locale.ROOT, FMT_ERR_MSG, i)
				);
			}
			return retryFlag;
		}
		//
	};
	//
	public static WSRequestConfigBase getInstance() {
		return newInstanceFor(Main.RUN_TIME_CONFIG.getStorageApi());
	}
	//
	private final static String NAME_CLS_IMPL = "WSRequestConfigImpl";
	//
	public static WSRequestConfigBase newInstanceFor(final String api) {
		WSRequestConfigBase reqConf = null;
		final String apiImplClsFQN =
			WSRequestConfigBase.class.getPackage().getName() +
				Main.DOT + REL_PKG_PROVIDERS + Main.DOT +
				api.toLowerCase() + Main.DOT + NAME_CLS_IMPL;
		try {
			reqConf = WSRequestConfigBase.class.cast(
				Class.forName(apiImplClsFQN).getConstructors()[0].newInstance()
			);
		} catch(final ClassNotFoundException e) {
			LOG.fatal(Markers.ERR, "API implementation not found: \"{}\"", apiImplClsFQN);
		} catch(final ClassCastException e) {
			LOG.fatal(Markers.ERR, "Class \"{}\" is not valid API implementation", apiImplClsFQN);
		} catch(final Exception e) {
			synchronized(LOG) {
				LOG.fatal(Markers.ERR, "WS API config instantiation failure: {}", e.toString());
				LOG.debug(Markers.ERR, "cause: {}", e.getCause());
			}
		}
		return reqConf;
	}
	//
	protected ConcurrentHashMap<String, String> sharedHeadersMap;
	protected final Mac mac;
	protected final URIBuilder uriBuilder = new URIBuilder();
	protected CloseableHttpClient httpClient;
	//
	public WSRequestConfigBase()
	throws NoSuchAlgorithmException {
		super();
		signMethod = runTimeConfig.getHttpSignMethod();
		mac = Mac.getInstance(signMethod);
		final String
			runName = runTimeConfig.getRunName(),
			runVersion = runTimeConfig.getRunVersion(),
			contentType = runTimeConfig.getHttpContentType();
		userAgent = runName + '/' + runVersion;
		sharedHeadersMap = new ConcurrentHashMap<String, String>() {
			{
				//put(HttpHeaders.USER_AGENT, userAgent);
				put(HttpHeaders.CONNECTION, VALUE_KEEP_ALIVE);
				put(HttpHeaders.CONTENT_TYPE, contentType);
			}
		};
	}
	//
	@Override
	public final WSRequestConfigBase<T> setAPI(final String api) {
		super.setAPI(api);
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T> setDataSource(final DataSource<T> dataSrc) {
		super.setDataSource(dataSrc);
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T> setUserName(final String userName) {
		super.setUserName(userName);
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T> setRetries(final boolean retryFlag) {
		super.setRetries(retryFlag);
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T> setLoadType(final Request.Type loadType) {
		super.setLoadType(loadType);
		return this;
	}
	//
	public final String getNameSpace() {
		return sharedHeadersMap.get(KEY_EMC_NS);
	}
	public final WSRequestConfigBase<T> setNameSpace(final String nameSpace) {
		if(nameSpace==null) {
			LOG.debug(Markers.MSG, "Using empty namespace");
		} else {
			sharedHeadersMap.put(KEY_EMC_NS, nameSpace);
		}
		return this;
	}
	//
	@Override
	public final String getAddr() {
		return uriBuilder.getHost();
	}
	@Override
	public final WSRequestConfigBase<T> setAddr(final String addr) {
		uriBuilder.setHost(addr);
		super.setAddr(addr);
		return this;
	}
	//
	@Override
	public final int getPort() {
		return uriBuilder.getPort();
	}
	@Override
	public final WSRequestConfigBase<T> setPort(final int port) {
		uriBuilder.setPort(port);
		super.setPort(port);
		return this;
	}
	//
	@Override
	public final String getScheme() {
		return uriBuilder.getScheme();
	}
	@Override
	public final WSRequestConfigBase<T> setScheme(final String scheme) {
		uriBuilder.setScheme(scheme);
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T> setProperties(final RunTimeConfig runTimeConfig) {
		//
		String paramName = "storage.scheme";
		try {
			setScheme(this.runTimeConfig.getStorageProto());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		}
		//
		paramName = "data.namespace";
		try {
			setNameSpace(this.runTimeConfig.getDataNameSpace());
		} catch(final NoSuchElementException e) {
			LOG.debug(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalStateException e) {
			LOG.debug(Markers.ERR, "Failed to set the namespace", e);
		}
		//
		super.setProperties(runTimeConfig);
		//
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T> setSecret(final String secret) {
		SecretKeySpec keySpec;
		LOG.trace(Markers.MSG, "Applying secret key {}", secret);
		try {
			keySpec = new SecretKeySpec(secret.getBytes(DEFAULT_ENC), signMethod);
			synchronized(mac) {
				mac.init(keySpec);
			}
		} catch(UnsupportedEncodingException e) {
			LOG.fatal(Markers.ERR, "Configuration error", e);
		} catch(InvalidKeyException e) {
			LOG.error(Markers.ERR, "Invalid secret key", e);
		}
		//
		super.setSecret(secret);
		//
		return this;
	}
	//
	public final LinkedList<BasicHeader> getSharedHeaders() {
		final LinkedList<BasicHeader> headers = new LinkedList<>();
		for(final String headerName: sharedHeadersMap.keySet()) {
			headers.add(new BasicHeader(headerName, sharedHeadersMap.get(headerName)));
		}
		return headers;
	}
	//
	@Override
	public final Map<String, String> getSharedHeadersMap() {
		return sharedHeadersMap;
	}
	//
	@Override
	public final String getUserAgent() {
		return userAgent;
	}
	//
	@Override
	public final CloseableHttpClient getClient() {
		if(httpClient == null) {
			httpClient = HttpClientBuilder
				.create()
				.setConnectionManager(new BasicHttpClientConnectionManager())
				.setDefaultHeaders(getSharedHeaders())
				.setRetryHandler(getRetryHandler())
				.disableCookieManagement()
				.setUserAgent(userAgent)
				.build();
		}
		return httpClient;
	}
	//
	@Override
	public WSRequestConfigBase<T> setClient(final CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
		return this;
	}
	//
	@Override
	public final HttpRequestRetryHandler getRetryHandler() {
		return retryHandler;
	}
	//
	@Override
	public WSRequestConfigBase<T> clone()
	throws CloneNotSupportedException {
		final WSRequestConfigBase<T> copy = (WSRequestConfigBase<T>) super.clone();
		copy
			.setAddr(getAddr())
			.setLoadType(getLoadType())
			.setPort(getPort())
			.setUserName(getUserName())
			.setSecret(getSecret())
			.setScheme(getScheme())
			.setClient(getClient());
		return copy;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setScheme(String.class.cast(in.readObject()));
		LOG.trace(Markers.MSG, "Got scheme {}", uriBuilder.getScheme());
		sharedHeadersMap = (ConcurrentHashMap<String,String>) in.readObject();
		/*final int headersCount = in.readInt();
		sharedHeadersMap = new ConcurrentHashMap<>(headersCount);
		LOG.trace(Markers.MSG, "Got headers count {}", headersCount);
		String key, value;
		for(int i = 0; i < headersCount; i ++) {
			key = String.class.cast(in.readObject());
			LOG.trace(Markers.MSG, "Got header key {}", key);
			value = String.class.cast(in.readObject());
			LOG.trace(Markers.MSG, "Got header value {}", value);
			sharedHeadersMap.put(key, value);
		}*/
		LOG.trace(Markers.MSG, "Got headers map {}", sharedHeadersMap);
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(uriBuilder.getScheme());
		out.writeObject(sharedHeadersMap);
		/*out.writeInt(sharedHeadersMap.size());
		for(final String key: sharedHeadersMap.keySet()) {
			out.writeObject(key);
			out.writeObject(sharedHeadersMap.get(key));
		}*/
	}
	//
	@Override
	public final void applyDataItem(final HttpRequest httpRequest, final T dataItem)
	throws IllegalStateException, URISyntaxException {
		applyURI(httpRequest, dataItem);
		switch(loadType) {
			case CREATE:
				applyPayLoad(httpRequest, dataItem);
				break;
			case UPDATE:
				applyRangesHeaders(httpRequest, dataItem);
				applyPayLoad(httpRequest, dataItem.getPendingUpdatesContentEntity());
				break;
			case APPEND:
				applyAppendRangeHeader(httpRequest, dataItem);
				applyPayLoad(httpRequest, dataItem.getPendingAugmentContentEntity());
				break;
		}
	}
	//
	@Override
	public final void applyHeadersFinally(final HttpRequest httpRequest) {
		applyDateHeader(httpRequest);
		applyAuthHeader(httpRequest);
		if(LOG.isTraceEnabled(Markers.MSG)) {
			synchronized(LOG) {
				LOG.trace(
					Markers.MSG, "built request: {} {}",
					httpRequest.getRequestLine().getMethod(), httpRequest.getRequestLine().getUri()
				);
				for(final Header header: httpRequest.getAllHeaders()) {
					LOG.trace(Markers.MSG, "\t{}: {}", header.getName(), header.getValue());
				}
				for(final String header: sharedHeadersMap.keySet()) {
					LOG.trace(Markers.MSG, "\t{}: {}", header, sharedHeadersMap.get(header));
				}
				if(httpRequest.getClass().isInstance(HttpEntityEnclosingRequest.class)) {
					LOG.trace(
						Markers.MSG, "\tcontent: {} bytes",
						HttpEntityEnclosingRequest.class.cast(httpRequest)
							.getEntity().getContentLength()
					);
				} else {
					LOG.trace(Markers.MSG, "\t---- no content ----");
				}
			}
		}
	}
	//
	protected abstract void applyURI(final HttpRequest httpRequest, final T dataItem)
	throws IllegalArgumentException, URISyntaxException;
	//
	protected final void applyPayLoad(
		final HttpRequest httpRequest, final HttpEntity httpEntity
	) {
		HttpEntityEnclosingRequest httpReqWithPayLoad = null;
		try {
			httpReqWithPayLoad = HttpEntityEnclosingRequest.class.cast(httpRequest);
		} catch(final ClassCastException e) {
			LOG.error(
				Markers.ERR, "\"{}\" HTTP request can't have a content entity",
				httpRequest.getRequestLine().getMethod()
			);
		}
		if(httpReqWithPayLoad != null) {
			httpReqWithPayLoad.setEntity(httpEntity);
		}
	}
	// merge subsequent updated ranges functionality is here
	protected final void applyRangesHeaders(
		final HttpRequest httpRequest, final T dataItem
	) {
		long rangeBeg = -1, rangeEnd = -1;
		int rangeLen = dataItem.getRangeSize(), rangeCount = dataItem.getCountRangesTotal();
		for(int i = 0; i < rangeCount; i++) {
			if(dataItem.isRangeUpdatePending(i)) {
				if(rangeBeg < 0) { // begin of the possible updated ranges sequence
					rangeBeg = i * rangeLen;
					rangeEnd = rangeBeg + rangeLen - 1;
					LOG.trace(
						Markers.MSG, "Begin of the possible updated ranges sequence @{}",
						rangeBeg
					);
				} else if(rangeEnd > 0) { // next range in the sequence of updated ranges
					rangeEnd += rangeLen;
				}
				if(i==rangeCount - 1) { // this is the last range which is updated also
					LOG.trace(Markers.MSG, "End of the updated ranges sequence @{}", rangeEnd);
					httpRequest.addHeader(
						HttpHeaders.RANGE, String.format(MSG_TMPL_RANGE_BYTES, rangeBeg, rangeEnd)
					);
				}
			} else if(rangeBeg > -1 && rangeEnd > -1) { // end of the updated ranges sequence
				LOG.trace(Markers.MSG, "End of the updated ranges sequence @{}", rangeEnd);
				httpRequest.addHeader(
					HttpHeaders.RANGE, String.format(MSG_TMPL_RANGE_BYTES, rangeBeg, rangeEnd)
				);
				// drop the updated ranges sequence info
				rangeBeg = -1;
				rangeEnd = -1;
			}
		}
	}
	//
	protected final void applyAppendRangeHeader(
		final HttpRequest httpRequest, final T dataItem
	) {
		httpRequest.addHeader(
			HttpHeaders.RANGE,
			String.format(MSG_TMPL_RANGE_BYTES_APPEND, dataItem.getSize())
		);
	}
	//
	protected void applyDateHeader(final HttpRequest httpRequest) {
		final String rfc1123date = DateUtils.formatDate(new Date());
		httpRequest.setHeader(HttpHeaders.DATE, rfc1123date);
	}
	//
	protected abstract void applyAuthHeader(final HttpRequest httpRequest);
	//
	//@Override
	//public final int hashCode() {
	//	return uriBuilder.hashCode()^mac.hashCode()^api.hashCode();
	//}
}
