package com.emc.mongoose.storage.adapter.atmos;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.io.task.WSIOTask;
import com.emc.mongoose.core.impl.io.req.conf.WSRequestConfigBase;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.impl.util.RunTimeConfig;
import com.emc.mongoose.core.api.persist.Markers;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
//
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
/**
 Created by kurila on 26.03.14.
 */
public final class WSRequestConfigImpl<T extends WSObject>
extends WSRequestConfigBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String KEY_SUBTENANT = "api.atmos.subtenant";
	//
	public final static String
		FMT_SLASH = "%s/%s", FMT_URI ="/rest/%s",
		API_TYPE_OBJ = "objects", API_TYPE_FS = "interface",
		DEFAULT_ACCEPT_VALUE = "*/*";
	//
	private WSSubTenantImpl<T> subTenant;
	private String uriBasePath;
	//
	public WSRequestConfigImpl()
	throws NoSuchAlgorithmException {
		this(null);
	}
	//
	protected WSRequestConfigImpl(final WSRequestConfigImpl<T> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
		//
		if(fsAccess) {
			uriBasePath = String.format(FMT_URI, API_TYPE_FS);
		} else {
			uriBasePath = String.format(FMT_URI, API_TYPE_OBJ);
		}
		//
		if(reqConf2Clone != null) {
			setSubTenant(reqConf2Clone.getSubTenant());
			setUserName(reqConf2Clone.getUserName());
			setSecret(reqConf2Clone.getSecret());
		}
		//
		if(!sharedHeadersMap.containsKey(HttpHeaders.ACCEPT)) {
			sharedHeadersMap.put(HttpHeaders.ACCEPT, DEFAULT_ACCEPT_VALUE);
		}
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public WSRequestConfigImpl<T> clone() {
		WSRequestConfigImpl<T> copy = null;
		try {
			copy = new WSRequestConfigImpl<>(this);
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
	public final WSSubTenantImpl<T> getSubTenant() {
		return subTenant;
	}
	//
	public final WSRequestConfigImpl<T> setSubTenant(final WSSubTenantImpl<T> subTenant)
	throws IllegalStateException {
		this.subTenant = subTenant;
		if(sharedHeadersMap != null && userName != null) {
			if(
				subTenant == null || subTenant.getName().length() < 1
			) {
				sharedHeadersMap.put(KEY_EMC_UID, userName);
			} else {
				sharedHeadersMap.put(KEY_EMC_UID, subTenant.getName() + '/' + userName);
			}
		}
		return this;
	}
	//
	@Override
	public final WSRequestConfigImpl<T> setUserName(final String userName)
	throws IllegalStateException {
		if(userName == null) {
			throw new IllegalStateException("User name is not specified for Atmos REST API");
		} else {
			super.setUserName(userName);
			if(sharedHeadersMap != null) {
				if(
					subTenant==null || subTenant.getName().length() < 1
				) {
					sharedHeadersMap.put(KEY_EMC_UID, userName);
				} else {
					sharedHeadersMap.put(
						KEY_EMC_UID, String.format(FMT_SLASH, subTenant.getName(), userName)
					);
				}
			}
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
	//
	@Override
	public final WSRequestConfigBase<T> setNameSpace(final String nameSpace) {
		return this;
	}
	//
	@Override
	public final WSRequestConfigImpl<T> setFileAccessEnabled(final boolean flag) {
		super.setFileAccessEnabled(flag);
		if(flag) {
			uriBasePath = String.format(FMT_URI, API_TYPE_FS);
		} else {
			uriBasePath = String.format(FMT_URI, API_TYPE_OBJ);
		}
		return this;
	}
	//
	@Override
	public final WSRequestConfigImpl<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		try {
			setSubTenant(new WSSubTenantImpl<>(this, runTimeConfig.getString(KEY_SUBTENANT)));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, KEY_SUBTENANT);
		}
		//
		if(runTimeConfig.getStorageFileAccessEnabled()) {
			uriBasePath = String.format(FMT_URI, API_TYPE_FS);
		} else {
			uriBasePath = String.format(FMT_URI, API_TYPE_OBJ);
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
		final Object t = in.readObject();
		if(t == null) {
			setSubTenant(null);
		} else {
			setSubTenant(new WSSubTenantImpl<>(this, String.class.cast(t)));
		}
		uriBasePath = String.class.cast(in.readObject());
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(subTenant == null ? null : subTenant.getName());
		out.writeObject(uriBasePath);
	}
	//
	@Override
	protected final void applyURI(final MutableWSRequest httpRequest, final T dataItem) {
		if(httpRequest == null) {
			throw new IllegalArgumentException(MSG_NO_REQ);
		}
		if(dataItem == null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		if(fsAccess || !IOTask.Type.CREATE.equals(loadType)) {
			httpRequest.setUriPath(String.format(FMT_SLASH, uriBasePath, dataItem.getId()));
		} else if(!uriBasePath.equals(httpRequest.getUriPath())) { // "/rest/objects"
			httpRequest.setUriPath(uriBasePath);
		} // else do nothing, uri is "/rest/objects" already

	}
	//
	@Override
	protected final void applyAuthHeader(final MutableWSRequest httpRequest) {
		httpRequest.setHeader(KEY_EMC_SIG, getSignature(getCanonical(httpRequest)));
	}
	//
	private final static String HEADERS4CANONICAL[] = {
		HttpHeaders.CONTENT_TYPE, HttpHeaders.RANGE, HttpHeaders.DATE
	};
	//
	@Override
	public final String getCanonical(final MutableWSRequest httpRequest) {
		final StringBuilder buffer = new StringBuilder(httpRequest.getRequestLine().getMethod());
		//Map<String, String> sharedHeaders = sharedConfig.getSharedHeaders();
		for(final String headerName : HEADERS4CANONICAL) {
			// support for multiple non-unique header keys
			if(sharedHeadersMap.containsKey(headerName)) {
				buffer.append('\n').append(sharedHeadersMap.get(headerName));
			} else if(httpRequest.containsHeader(headerName)) {
				for(final Header header: httpRequest.getHeaders(headerName)) {
					buffer.append('\n').append(header.getValue());
				}
			} else {
				buffer.append('\n');
			}
		}
		//
		buffer.append('\n').append(httpRequest.getUriPath());
		//
		for(final String emcHeaderName: HEADERS_EMC) {
			if(sharedHeadersMap.containsKey(emcHeaderName)) {
				buffer
					.append('\n').append(emcHeaderName.toLowerCase())
					.append(':').append(sharedHeadersMap.get(emcHeaderName));
			} else {
				for(final Header emcHeader: httpRequest.getHeaders(emcHeaderName)) {
					buffer.append('\n').append(emcHeaderName.toLowerCase()).append(':').append(
						emcHeader.getValue()
					);
				}
			}
		}
		//
		if(LOG.isTraceEnabled()) {
			LOG.trace(Markers.MSG, "Canonical request form:\n{}", buffer.toString());
		}
		//
		return buffer.toString();
	}
	//
	private final static String
		FMT_MSG_ERR_LOCATION_HEADER_VALUE = "Invalid response location header value: \"%s\"";
	//
	@Override
	protected final void applyObjectId(final T dataObject, final HttpResponse httpResponse) {
		if(
			IOTask.Type.CREATE.equals(loadType) &&
			httpResponse.containsHeader(HttpHeaders.LOCATION)
		) {
			final String valueLocation = httpResponse
				.getFirstHeader(HttpHeaders.LOCATION)
				.getValue();
			if(
				valueLocation != null &&
				valueLocation.startsWith(uriBasePath) &&
				valueLocation.length() - uriBasePath.length() > 1
			) {
				final String id = valueLocation.substring(uriBasePath.length() + 1);
				if(id.length() > 0) {
					dataObject.setId(id);
				} else {
					LOG.trace(Markers.ERR, "Got empty object id");
				}
			} else if(LOG.isTraceEnabled(Markers.ERR)) {
				LOG.trace(
					Markers.ERR, String.format(FMT_MSG_ERR_LOCATION_HEADER_VALUE, valueLocation)
				);
			}
		}
		//
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "Applied object \"{}\" id \"{}\" from the source \"{}\"",
				Long.toHexString(dataObject.getOffset()), dataObject.getId(),
				httpResponse.getFirstHeader(HttpHeaders.LOCATION)
			);
		}
	}
	//
	@Override
	public final void applyDataItem(final MutableWSRequest httpRequest, final T dataItem)
	throws IllegalStateException, URISyntaxException {
		if(fsAccess) {
			super.applyObjectId(dataItem, null);
		}
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
	public void configureStorage(final LoadExecutor<T> client)
	throws IllegalStateException {
		// TODO issue
		// TODO issue #148
		/*if(subTenant == null) {
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
		}*/
	}
	//
	@Override
	public void receiveResponse(final HttpResponse response, final T dataItem) {
		applyObjectId(dataItem, response);
	}
}
