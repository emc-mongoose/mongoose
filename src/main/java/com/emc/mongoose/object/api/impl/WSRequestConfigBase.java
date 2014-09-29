package com.emc.mongoose.object.api.impl;
//
import com.emc.mongoose.base.api.impl.RequestConfigBase;
import com.emc.mongoose.object.api.WSObjectRequestConfig;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.object.data.WSDataObject;
//
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
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
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 09.06.14.
 */
public abstract class WSRequestConfigBase<T extends WSDataObject>
extends RequestConfigBase<T>
implements WSObjectRequestConfig<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
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
		return newInstanceFor(RunTimeConfig.getString("storage.api"));
	}
	//
	public static WSRequestConfigBase newInstanceFor(final String api) {
		WSRequestConfigBase reqConf = null;
		final String apiImplClsFQN =
			WSRequestConfigBase.class.getPackage().getName() +
				REL_PKG_WS_PROVIDERS + StringUtils.capitalize(api.toLowerCase());
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
	{
		Mac localMac = null;
		try {
			localMac = Mac.getInstance(VALUE_SIGN_METHOD);
		} catch(final NoSuchAlgorithmException e) {
			LOG.error(
				Markers.ERR,
				"Illegal cipher algorithm: \"{}\", check config propery \"http.req.sign.method\" value",
				VALUE_SIGN_METHOD
			);
		}
		mac = localMac;
	}
	//
	public final String getNameSpace() {
		return sharedHeadersMap.get(KEY_EMC_NS);
	}
	public final WSRequestConfigBase setNameSpace(final String nameSpace) {
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
	public WSRequestConfigBase<T> setProperties(final RunTimeConfig props) {
		//
		String paramName = "storage.scheme";
		try {
			setScheme(RunTimeConfig.getString(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		}
		//
		paramName = "data.namespace";
		try {
			setNameSpace(RunTimeConfig.getString(paramName));
		} catch(final NoSuchElementException e) {
			LOG.debug(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalStateException e) {
			LOG.debug(Markers.ERR, "Failed to set the namespace", e);
		}
		//
		super.setProperties(props);
		//
		return this;
	}
	//
	@Override
	public WSRequestConfigBase<T> setSecret(final String secret) {
		SecretKeySpec keySpec;
		LOG.trace(Markers.MSG, "Applying secret key {}", secret);
		try {
			keySpec = new SecretKeySpec(secret.getBytes(DEFAULT_ENC), VALUE_SIGN_METHOD);
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
	public final ConcurrentHashMap<String, String> getSharedHeadersMap() {
		return sharedHeadersMap;
	}
	//
	@Override
	public final CloseableHttpClient getClient() {
		return httpClient;
	}
	//
	@Override
	public final WSRequestConfigBase<T> setClient(final CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
		return this;
	}
	//
	@Override
	public final HttpRequestRetryHandler getRetryHandler() {
		return retryHandler;
	}
	//
	protected WSRequestConfigBase() {
		sharedHeadersMap = new ConcurrentHashMap<String, String>() {
			{
				put(HttpHeaders.USER_AGENT, DEFAULT_USERAGENT);
				put(HttpHeaders.CONNECTION, VALUE_KEEP_ALIVE);
				put(HttpHeaders.CONTENT_TYPE, REQ_DATA_TYPE);
			}
		};
	}
	//
	@Override
	public abstract WSRequestConfigBase<T> clone();
	//
	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		sharedHeadersMap = (ConcurrentHashMap<String, String>) in.readObject();
		setScheme(String.class.cast(in.readObject()));
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(sharedHeadersMap);
		out.writeObject(uriBuilder.getScheme());
	}
	//
	@Override
	public final void applyDataItem(final HttpRequestBase httpRequest, final T dataItem)
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
	public final void applyHeadersFinally(final HttpRequestBase httpRequest) {
		applyDateHeader(httpRequest);
		applyAuthHeader(httpRequest);
		if(LOG.isTraceEnabled(Markers.MSG)) {
			synchronized(LOG) {
				LOG.trace(
					Markers.MSG, "built request: {} {}",
					httpRequest.getMethod(), httpRequest.getURI()
				);
				for(final Header header: httpRequest.getAllHeaders()) {
					LOG.trace(Markers.MSG, "\t{}: {}", header.getName(), header.getValue());
				}
				for(final String header: sharedHeadersMap.keySet()) {
					LOG.trace(Markers.MSG, "\t{}: {}", header, sharedHeadersMap.get(header));
				}
				try {
					LOG.trace(
						Markers.MSG, "\tcontent: {} bytes",
						HttpEntityEnclosingRequest.class.cast(httpRequest)
							.getEntity().getContentLength()
					);
				} catch(final ClassCastException e) {
					LOG.trace(Markers.MSG, "\t---- no content ----");
				}
			}
		}
	}
	//
	protected abstract void applyURI(final HttpRequestBase httpRequest, final T dataItem)
	throws IllegalArgumentException, URISyntaxException;
	//
	protected final void applyPayLoad(final HttpRequestBase httpRequest, final HttpEntity httpEntity) {
		HttpEntityEnclosingRequest httpReqWithPayLoad = null;
		try {
			httpReqWithPayLoad = HttpEntityEnclosingRequest.class.cast(httpRequest);
		} catch(final ClassCastException e) {
			LOG.error(
				Markers.ERR, "\"{}\" HTTP request can't have a content entity",
				httpRequest.getMethod()
			);
		}
		if(httpReqWithPayLoad!=null) {
			httpReqWithPayLoad.setEntity(httpEntity);
		}
	}
	// merge subsequent updated ranges functionality is here
	protected final void applyRangesHeaders(
		final HttpRequestBase httpRequest, final T dataItem
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
		final HttpRequestBase httpRequest, final T dataItem
	) {
		httpRequest.addHeader(
			HttpHeaders.RANGE,
			String.format(MSG_TMPL_RANGE_BYTES_APPEND, dataItem.getSize())
		);
	}
	//
	protected final void applyDateHeader(final HttpRequestBase httpRequest) {
		final String rfc1123date = DateUtils.formatDate(new Date());
		httpRequest.setHeader(HttpHeaders.DATE, rfc1123date);
		//httpRequest.setHeader(KEY_EMC_DATE, rfc1123date);
	}
	//
	protected abstract void applyAuthHeader(final HttpRequestBase httpRequest);
	//
	//@Override
	//public final int hashCode() {
	//	return uriBuilder.hashCode()^mac.hashCode()^api.hashCode();
	//}
}
