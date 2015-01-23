package com.emc.mongoose.web.api.impl.provider.atmos;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.web.api.MutableHTTPRequest;
import com.emc.mongoose.web.api.WSIOTask;
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
//
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
/**
 Created by kurila on 26.03.14.
 */
public final class RequestConfig<T extends WSObject>
extends WSRequestConfigBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String KEY_SUBTENANT = "api.atmos.subtenant";
	//
	private final RunTimeConfig runTimeConfig = Main.RUN_TIME_CONFIG.get();
	public final static String
		API_PATH_REST = Main.RUN_TIME_CONFIG.get().getString("api.atmos.path.rest"),
		API_PATH_INTERFACE = Main.RUN_TIME_CONFIG.get().getString("api.atmos.interface"),
		URI_PREFIX = String.format("/%s/%s", API_PATH_REST, API_PATH_INTERFACE);
	//
	private SubTenant<T> subTenant;
	//
	public RequestConfig()
	throws NoSuchAlgorithmException {
		this(null);
	}
	//
	protected RequestConfig(final RequestConfig<T> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
		if(reqConf2Clone != null) {
			setNameSpace(reqConf2Clone.getNameSpace());
			setSubTenant(reqConf2Clone.getSubTenant());
		}
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public RequestConfig<T> clone() {
		RequestConfig<T> copy = null;
		try {
			copy = new RequestConfig<>(this);
		} catch(final NoSuchAlgorithmException e) {
			LOG.fatal(Markers.ERR, "No such algorithm: \"{}\"", signMethod);
		}
		return copy;
	}
	//
	@Override
	public WSIOTask.HTTPMethod getHTTPMethod() {
		WSIOTask.HTTPMethod method;
		switch(loadType) {
			case CREATE:
				method = WSIOTask.HTTPMethod.POST;
				break;
			case READ:
				method = WSIOTask.HTTPMethod.GET;
				break;
			case DELETE:
				method = WSIOTask.HTTPMethod.DELETE;
				break;
			default: // UPDATE, APPEND
				method = WSIOTask.HTTPMethod.PUT;
				break;
		}
		return method;
	}
	//
	public final SubTenant<T> getSubTenant() {
		return subTenant;
	}
	//
	public final RequestConfig<T> setSubTenant(final SubTenant<T> subTenant)
	throws IllegalStateException {
		this.subTenant = subTenant;
		if(subTenant == null) {
			throw new IllegalStateException("Subtenant is not specified for Atmos REST API");
		} else if(userName != null) {
			sharedHeadersMap.put(KEY_EMC_UID, subTenant.getName() + '/' + userName);
		}
		return this;
	}
	//
	@Override
	public final RequestConfig<T> setUserName(final String userName) {
		super.setUserName(userName);
		if(userName==null) {
			throw new IllegalStateException("User name is not specified for Atmos REST API");
		} else if(subTenant!=null) {
			sharedHeadersMap.put(KEY_EMC_UID, subTenant.getName() + '/' + userName);
		}
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T> setSecret(final String secret) {
		//
		this.secret = secret;
		//
		SecretKeySpec keySpec;
		LOG.trace(Markers.MSG, "Applying secret key {}", secret);
		try {
			keySpec = new SecretKeySpec(Base64.decodeBase64(secret), signMethod);
			mac.init(keySpec);
		} catch(InvalidKeyException e) {
			LOG.error(Markers.ERR, "Invalid secret key", e);
		}
		//
		return this;
	}
	@Override
	public final RequestConfig<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		try {
			setSubTenant(new SubTenant<>(this, this.runTimeConfig.getString(KEY_SUBTENANT)));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, KEY_SUBTENANT);
		}
		//
		return this;
	}
	//
	@Override
	public final Producer<T> getAnyDataProducer(final long maxCount, final LoadExecutor<T> client) {
		// TODO implement sub tenant listing producer
		return null;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setSubTenant(new SubTenant<>(this, String.class.cast(in.readObject())));
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(subTenant.getName());
	}
	//
	private final String FMT_OBJ_PATH = "%s/%s";
	//
	@Override
	protected final void applyURI(final MutableHTTPRequest httpRequest, final T dataItem) {
		if(httpRequest == null) {
			throw new IllegalArgumentException(MSG_NO_REQ);
		}
		if(dataItem == null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		httpRequest.setUriPath(String.format(FMT_OBJ_PATH, URI_PREFIX, dataItem.getId()));
	}
	//
	private final static String DEFAULT_ACCEPT_VALUE = "*/*";
	//
	@Override
	public final void applyHeadersFinally(final MutableHTTPRequest httpRequest) {
		httpRequest.addHeader(HttpHeaders.ACCEPT, DEFAULT_ACCEPT_VALUE);
		super.applyHeadersFinally(httpRequest);
	}
	/*
	@Override
	protected final void applyDateHeader(final MutableHTTPRequest httpRequest) {
		//super.applyDateHeader(httpRequest);
		httpRequest.setHeader(
			HttpHeaders.DATE, FMT_DATE_RFC1123.format(Main.CALENDAR_DEFAULT.getTime())
		);
	}*/
	//
	@Override
	protected final void applyAuthHeader(final MutableHTTPRequest httpRequest) {
		if(!httpRequest.containsHeader(HttpHeaders.RANGE)) {
			httpRequest.addHeader(HttpHeaders.RANGE, ""); // temporary required for canonical form
		}
		//
		httpRequest.addHeader(KEY_EMC_SIG, getSignature(getCanonical(httpRequest)));
		//
		Header tmpHeader = httpRequest.getLastHeader(HttpHeaders.RANGE);
		if(tmpHeader != null && tmpHeader.getValue().length() == 0) { // the header is temp
			httpRequest.removeHeader(tmpHeader);
		}

	}
	//
	private static String HEADERS4CANONICAL[] = {
		HttpHeaders.CONTENT_TYPE, HttpHeaders.RANGE, HttpHeaders.DATE
	};
	//
	@Override
	public final String getCanonical(final MutableHTTPRequest httpRequest) {
		final StringBuilder buffer = new StringBuilder(httpRequest.getRequestLine().getMethod());
		//Map<String, String> sharedHeaders = sharedConfig.getSharedHeaders();
		for(final String headerName: HEADERS4CANONICAL) {
			// support for multiple non-unique header keys
			for(final Header header: httpRequest.getHeaders(headerName)) {
				buffer.append('\n').append(header.getValue());
			}
			if(sharedHeadersMap.containsKey(headerName)) {
				buffer.append('\n').append(sharedHeadersMap.get(headerName));
			}
		}
		//
		buffer.append('\n').append(httpRequest.getUriPath());
		//
		for(final String emcHeaderName: HEADERS_EMC) {
			for(final Header emcHeader: httpRequest.getHeaders(emcHeaderName)) {
				buffer
					.append('\n').append(emcHeaderName.toLowerCase())
					.append(':').append(emcHeader.getValue());
			}
			if(sharedHeadersMap.containsKey(emcHeaderName)) {
				buffer
					.append('\n').append(emcHeaderName.toLowerCase())
					.append(':').append(sharedHeadersMap.get(emcHeaderName));
			}
		}
		//
		if(LOG.isTraceEnabled()) {
			LOG.trace(Markers.MSG, "Canonical request form:\n{}", buffer.toString());
		}
		//
		return buffer.toString();
	}
	/*
	private final static String
		FMT_MSG_ERR_LOCATION_HEADER_VALUE = "Invalid response location header value: \"%s\"";
	//
	@Override
	public final void applyObjectId(final T dataObject, final HttpResponse httpResponse) {
		final Header headerLocation = httpResponse.getFirstHeader(HttpHeaders.LOCATION);
		if(headerLocation != null) {
			final String valueLocation = headerLocation.getValue();
			if(
				valueLocation != null &&
				valueLocation.startsWith(URI_PREFIX) &&
				valueLocation.length() - URI_PREFIX.length() > 1
			) {
				final String id = valueLocation.substring(URI_PREFIX.length() + 1);
				if(id.length() > 0) {
					dataObject.setId(id);
				} else {
					LOG.trace(Markers.ERR, "Got empty object id");
				}
			} else {
				LOG.trace(Markers.ERR, String.format(FMT_MSG_ERR_LOCATION_HEADER_VALUE, valueLocation));
			}
		} else {
			LOG.trace(Markers.ERR, "No location header in the http response");
		}
	}*/
	//
	@Override
	public void configureStorage(final LoadExecutor<T> client)
	throws IllegalStateException {
		if(subTenant == null) {
			throw new IllegalStateException("Subtenant is not specified");
		}
		final String subTenantName = subTenant.getName();
		if(subTenant.exists(client)) {
			LOG.debug(Markers.MSG, "Subtenant \"{}\" already exists", subTenantName);
		} else {
			subTenant.create(client);
			if(subTenant.exists(client)) {
				runTimeConfig.set(KEY_SUBTENANT, subTenantName);
			} else {
				throw new IllegalStateException(
					String.format("Created subtenant \"%s\" doesn't exist", subTenantName)
				);
			}
		}
	}
	/*
	@Override
	public void receiveResponse(final HttpResponse response, final T dataItem) {
		// applyObjectId(dataItem, response);
	}*/
}
