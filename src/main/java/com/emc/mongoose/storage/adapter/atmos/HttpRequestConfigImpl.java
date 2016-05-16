package com.emc.mongoose.storage.adapter.atmos;
// mongoose-core-api.jar

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.token.Token;
import com.emc.mongoose.core.impl.io.conf.HttpRequestConfigBase;
import com.emc.mongoose.core.impl.item.token.BasicToken;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.*;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

// mongoose-core-impl.jar
// mongoose-common.jar
//
//
//
//
/**
 Created by kurila on 26.03.14.
 */
public final class HttpRequestConfigImpl<T extends HttpDataItem, C extends Container<T>>
extends HttpRequestConfigBase<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static String PREFIX_URI ="/rest/";
	public final static String API_TYPE_OBJ = "objects";
	public final static String API_TYPE_FS = "namespace";
	//
	public final static Header
		DEFAULT_ACCEPT_HEADER = new BasicHeader(HttpHeaders.ACCEPT, "*/*");
	//
	private String uriBasePath;
	//
	public HttpRequestConfigImpl()
	throws NoSuchAlgorithmException {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	public HttpRequestConfigImpl(final AppConfig appConfig) {
		super(appConfig);
	}
	//
	protected HttpRequestConfigImpl(final HttpRequestConfigImpl<T, C> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
		//
		if(fsAccess) {
			uriBasePath = PREFIX_URI + API_TYPE_FS;
		} else {
			uriBasePath = PREFIX_URI + API_TYPE_OBJ;
		}
		//
		if(reqConf2Clone != null) {
			setAuthToken(reqConf2Clone.getAuthToken());
			setUserName(reqConf2Clone.getUserName());
			setSecret(reqConf2Clone.getSecret());
		}
		//
		if(sharedHeaders == null) {
			sharedHeaders = new HashMap<>();
		}
		sharedHeaders.put(HttpHeaders.ACCEPT, DEFAULT_ACCEPT_HEADER);
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public HttpRequestConfigImpl<T, C> clone() {
		HttpRequestConfigImpl<T, C> copy = null;
		try {
			copy = new HttpRequestConfigImpl<>(this);
		} catch(final NoSuchAlgorithmException e) {
			LOG.fatal(Markers.ERR, "No such algorithm: \"{}\"", SIGN_METHOD);
		}
		return copy;
	}
	//
	@Override
	public final HttpEntityEnclosingRequest createDataRequest(final T obj, final String nodeAddr)
	throws URISyntaxException {
		//if(fsAccess) {
		//	super.applyObjectId(obj, null);
		//}
		final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(
			getHttpMethod(), getObjectDstPath(obj)
		);
		try {
			applyHostHeader(request, nodeAddr);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply a host header");
		}
		switch(loadType) {
			case WRITE:
				if(obj.hasScheduledUpdates() || obj.isAppending()) {
					applyRangesHeaders(request, obj);
				}
				applyPayLoad(request, obj);
				break;
			case READ:
				// TODO partial content support
			case DELETE:
				applyPayLoad(request, null);
				break;
		}
		return request;
	}
	//
	@Override
	public final HttpEntityEnclosingRequest createContainerRequest(
		final C container, final String nodeAddr
	) throws URISyntaxException {
		throw new IllegalStateException("No container request is possible using Atmos API");
	}
	//
	@Override
	public String getHttpMethod() {
		switch(loadType) {
			case WRITE:
				return METHOD_POST;
			case READ:
				return METHOD_GET;
			case DELETE:
				return METHOD_DELETE;
			default: // UPDATE, APPEND
				return METHOD_PUT;
		}
	}
	//
	@Override
	public final HttpRequestConfigImpl<T, C> setAuthToken(final Token subTenant)
	throws IllegalStateException {
		super.setAuthToken(subTenant);
		if(sharedHeaders == null) {
			sharedHeaders = new HashMap<>();
		}
		if(userName != null) {
			if(subTenant == null || subTenant.toString().length() < 1) {
				sharedHeaders.put(KEY_EMC_UID, new BasicHeader(KEY_EMC_UID, userName));
			} else {
				sharedHeaders.put(
					KEY_EMC_UID, new BasicHeader(KEY_EMC_UID, subTenant.toString() + "/" + userName)
				);
			}
		}
		return this;
	}
	//
	@Override
	public final HttpRequestConfigImpl<T, C> setUserName(final String userName)
	throws IllegalStateException {
		if(userName == null) {
			throw new IllegalStateException("User name is not specified for Atmos REST API");
		} else {
			super.setUserName(userName);
			if(sharedHeaders == null) {
				sharedHeaders = new HashMap<>();
			}
			if(authToken == null || authToken.toString().length() < 1) {
				sharedHeaders.put(KEY_EMC_UID, new BasicHeader(KEY_EMC_UID, userName));
			} else {
				sharedHeaders.put(
					KEY_EMC_UID,
					new BasicHeader(KEY_EMC_UID, authToken.toString() + "/" + userName)
				);
			}
		}
		return this;
	}
	//
	@Override
	public final HttpRequestConfigBase<T, C> setSecret(final String secret) {
		super.setSecret(secret);
		LOG.trace(Markers.MSG, "Applying secret key {}", secret);
		secretKey = secret == null || secret.isEmpty() ?
			null : new SecretKeySpec(Base64.decodeBase64(secret), SIGN_METHOD);
		return this;
	}
	//
	@Override
	public final HttpRequestConfigBase<T, C> setNameSpace(final String nameSpace) {
		super.setNameSpace(nameSpace);
		//if(nameSpace == null || nameSpace.length() < 1) {
			LOG.debug(Markers.MSG, "Using empty namespace");
		/*} else {
			sharedHeaders.updateHeader(new BasicHeader(KEY_EMC_NS, nameSpace));
		}*/
		return this;
	}
	//
	@Override
	public final HttpRequestConfigImpl<T, C> setFileAccessEnabled(final boolean flag) {
		super.setFileAccessEnabled(flag);
		if(flag) {
			uriBasePath = PREFIX_URI + API_TYPE_FS;
		} else {
			uriBasePath = PREFIX_URI + API_TYPE_OBJ;
		}
		return this;
	}
	//
	@Override
	public final HttpRequestConfigImpl<T, C> setAppConfig(final AppConfig appConfig) {
		super.setAppConfig(appConfig);
		//
		try {
			final String t = appConfig.getAuthToken();
			setAuthToken(t == null ? null : new BasicToken(t));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, AppConfig.KEY_AUTH_TOKEN);
		}
		//
		if(this.appConfig.getStorageHttpFsAccess()) {
			uriBasePath = PREFIX_URI + API_TYPE_FS;
		} else {
			uriBasePath = PREFIX_URI + API_TYPE_OBJ;
		}
		//
		return this;
	}
	//
	@Override
	public final Input<T> getContainerListInput(final long maxCount, final String addr) {
		// TODO implement sub tenant listing producer
		return null;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		uriBasePath = (String) in.readObject();
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(uriBasePath);
	}
	//
	@Override
	protected final String getObjectDstPath(final T object) {
		if(object == null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		if(fsAccess || !LoadType.WRITE.equals(loadType)) {
			return uriBasePath + "/" + object.getName();
		} else { // "/rest/objects"
			return uriBasePath;
		}
	}
	//
	@Override
	protected final String getObjectSrcPath(final T object) {
		if(object == null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		if(fsAccess || !LoadType.WRITE.equals(loadType)) {
			return uriBasePath + "/" + object.getName();
		} else { // "/rest/objects"
			return uriBasePath;
		}
	}
	//
	@Override
	protected final String getContainerPath(final Container<T> container)
	throws IllegalArgumentException, URISyntaxException {
		throw new IllegalStateException("No container request is possible using Atmos API");
	}
	//
	private final static ThreadLocal<StringBuilder>
		THR_LOC_METADATA_STR_BUILDER = new ThreadLocal<>();
	@Override @SuppressWarnings("unchecked")
	protected final void applyMetaDataHeaders(final HttpRequest request) {
		StringBuilder md = THR_LOC_METADATA_STR_BUILDER.get();
		if(md == null) {
			md = new StringBuilder();
			THR_LOC_METADATA_STR_BUILDER.set(md);
		} else {
			md.setLength(0); // reset/clear
		}
		//
		if(authToken != null) {
			final String subtenantId = authToken.toString();
			if(subtenantId != null && subtenantId.length() > 0) {
				md.append("subtenant=").append(authToken.toString());
			}
		}
		// the "offset" tag is required for WS mock
		if(
			LoadType.WRITE.equals(loadType) &&
			request instanceof HttpEntityEnclosingRequest
		) {
			final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			if(entity != null && entity instanceof HttpDataItem) {
				if(md.length() > 0) {
					md.append(',');
				}
				md.append("offset=").append(((T) entity).getOffset());
			}
		}
		//
		if(md.length() > 0) {
			request.setHeader(KEY_EMC_TAGS, md.toString());
		}
	}
	//
	@Override
	protected final void applyAuthHeader(final HttpRequest httpRequest) {
		final String signature = getSignature(getCanonical(httpRequest));
		if(signature != null) {
			httpRequest.setHeader(KEY_EMC_SIG, signature);
		}
	}
	//
	private final static String HEADERS_CANONICAL[] = {
		HttpHeaders.CONTENT_TYPE, HttpHeaders.RANGE, HttpHeaders.DATE
	};
	//
	private final static ThreadLocal<StringBuilder>
		THR_LOC_CANONICAL_STR_BUILDER = new ThreadLocal<>();
	//
	@Override
	public final String getCanonical(final HttpRequest httpRequest) {
		//
		StringBuilder canonical = THR_LOC_CANONICAL_STR_BUILDER.get();
		if(canonical == null) {
			canonical = new StringBuilder();
			THR_LOC_CANONICAL_STR_BUILDER.set(canonical);
		} else {
			canonical.setLength(0); // reset/clear
		}
		canonical.append(httpRequest.getRequestLine().getMethod());
		//
		for(final String headerName : HEADERS_CANONICAL) {
			// support for multiple non-unique header keys
			if(httpRequest.containsHeader(headerName)) {
				for(final Header header : httpRequest.getHeaders(headerName)) {
					canonical.append('\n').append(header.getValue());
				}
			} else if(sharedHeaders != null && sharedHeaders.containsKey(headerName)) {
				canonical.append('\n').append(sharedHeaders.get(headerName).getValue());
			} else {
				canonical.append('\n');
			}
		}
		//
		final String uri = httpRequest.getRequestLine().getUri();
		canonical.append('\n').append(uri.contains("?") ? uri.substring(0, uri.indexOf("?")) : uri);
		// x-emc-*
		String headerName;
		Map<String, String> sortedHeaders = new TreeMap<>();
		if(sharedHeaders != null) {
			for(final Header header : sharedHeaders.values()) {
				headerName = header.getName().toLowerCase();
				if(headerName.startsWith(PREFIX_KEY_EMC)) {
					sortedHeaders.put(headerName, header.getValue());
				}
			}
		}
		for(final Header header : httpRequest.getAllHeaders()) {
			headerName = header.getName().toLowerCase();
			if(headerName.startsWith(PREFIX_KEY_EMC)) {
				sortedHeaders.put(headerName, header.getValue());
			}
		}
		for(final String k : sortedHeaders.keySet()) {
			canonical.append('\n').append(k).append(':').append(sortedHeaders.get(k));
		}
		//
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Canonical request form:\n{}", canonical.toString());
		}
		//
		return canonical.toString();
	}
	//
	@Override
	protected final void applyObjectId(final T dataObject, final HttpResponse httpResponse) {
		final Header locationHeader = httpResponse == null ?
			null : httpResponse.getFirstHeader(HttpHeaders.LOCATION);
		if(locationHeader != null && LoadType.WRITE.equals(loadType)) {
			final String valueLocation = httpResponse
				.getFirstHeader(HttpHeaders.LOCATION)
				.getValue();
			if(
				valueLocation != null &&
				valueLocation.startsWith(uriBasePath) &&
				valueLocation.length() - uriBasePath.length() > 1
			) {
				final String oid = valueLocation.substring(uriBasePath.length() + 1);
				if(oid.length() > 0) {
					dataObject.setName(oid);
				} else {
					LOG.trace(Markers.ERR, "Got empty object id");
				}
			} else if(LOG.isTraceEnabled(Markers.ERR)) {
				LOG.trace(
					Markers.ERR, "Invalid response location header value: \"{}\"", valueLocation
				);
			}
		}
		//
		if(httpResponse != null && LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "Applied object \"{}\" id \"{}\" from the source \"{}\"",
				Long.toHexString(dataObject.getOffset()), dataObject.getName(),
				httpResponse.getFirstHeader(HttpHeaders.LOCATION)
			);
		}
	}
	//
	@Override
	public void configureStorage(final String storageAddrs[])
	throws IllegalStateException {
		// create the subtenant if necessary
		final String subTenantValue = authToken == null ? null : authToken.toString();
		if(subTenantValue == null || subTenantValue.length() == 0) {
			new AtmosSubTenantHelper(this, null).create(storageAddrs[0]);
		}
		/*re*/
		setAuthToken(authToken);
		appConfig.setProperty(AppConfig.KEY_AUTH_TOKEN, authToken.toString());
		super.configureStorage(storageAddrs);
	}
	//
	@Override
	protected final void createDirectoryPath(final String nodeAddr, final String dirPath)
	throws IllegalStateException {
		LOG.info(Markers.MSG, "Using the storage directory \"{}\"", dirPath);
	}
	//
	@Override
	public void applySuccResponseToObject(final HttpResponse response, final T dataItem) {
		super.applySuccResponseToObject(response, dataItem);
		applyObjectId(dataItem, response);
	}
}
