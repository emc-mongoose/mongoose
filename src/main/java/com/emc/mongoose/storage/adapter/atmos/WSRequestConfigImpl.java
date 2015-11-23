package com.emc.mongoose.storage.adapter.atmos;
// mongoose-core-api.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.task.IOTask;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import org.apache.commons.codec.binary.Base64;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 26.03.14.
 */
public final class WSRequestConfigImpl<T extends WSObject, C extends Container<T>>
extends WSRequestConfigBase<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static String
		PREFIX_URI ="/rest/", API_TYPE_OBJ = "objects", API_TYPE_FS = "namespace";
	//
	public final static Header
		DEFAULT_ACCEPT_HEADER = new BasicHeader(HttpHeaders.ACCEPT, "*/*");
	//
	private WSSubTenantImpl<T> subTenant;
	private String uriBasePath;
	//
	public WSRequestConfigImpl()
	throws NoSuchAlgorithmException {
		this(null);
	}
	//
	protected WSRequestConfigImpl(final WSRequestConfigImpl<T, C> reqConf2Clone)
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
			setSubTenant(reqConf2Clone.getSubTenant());
			setUserName(reqConf2Clone.getUserName());
			setSecret(reqConf2Clone.getSecret());
		}
		//
		sharedHeaders.updateHeader(DEFAULT_ACCEPT_HEADER);
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public WSRequestConfigImpl<T, C> clone() {
		WSRequestConfigImpl<T, C> copy = null;
		try {
			copy = new WSRequestConfigImpl<>(this);
		} catch(final NoSuchAlgorithmException e) {
			LOG.fatal(Markers.ERR, "No such algorithm: \"{}\"", signMethod);
		}
		return copy;
	}
	//
	@Override
	public final HttpEntityEnclosingRequest createDataRequest(final T obj, final String nodeAddr)
	throws URISyntaxException {
		if(fsAccess) {
			super.applyObjectId(obj, null);
		}
		final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(
			getHttpMethod(), getDataUriPath(obj)
		);
		try {
			applyHostHeader(request, nodeAddr);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to apply a host header");
		}
		switch(loadType) {
			case UPDATE:
			case APPEND:
				applyRangesHeaders(request, obj);
			case CREATE:
				applyPayLoad(request, obj);
				break;
			case READ:
			case DELETE:
				applyPayLoad(request, null);
				break;
		}
		applyHeadersFinally(request);
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
			case CREATE:
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
	public final WSSubTenantImpl<T> getSubTenant() {
		return subTenant;
	}
	//
	public final WSRequestConfigImpl<T, C> setSubTenant(final WSSubTenantImpl<T> subTenant)
	throws IllegalStateException {
		this.subTenant = subTenant;
		if(sharedHeaders != null && userName != null) {
			if(subTenant == null || subTenant.getValue().length() < 1) {
				sharedHeaders.updateHeader(new BasicHeader(KEY_EMC_UID, userName));
			} else {
				sharedHeaders.updateHeader(
					new BasicHeader(KEY_EMC_UID, subTenant.getValue() + "/" + userName)
				);
			}
		}
		return this;
	}
	//
	@Override
	public final WSRequestConfigImpl<T, C> setUserName(final String userName)
	throws IllegalStateException {
		if(userName == null) {
			throw new IllegalStateException("User name is not specified for Atmos REST API");
		} else {
			super.setUserName(userName);
			if(sharedHeaders != null) {
				if(subTenant == null || subTenant.getValue().length() < 1) {
					sharedHeaders.updateHeader(new BasicHeader(KEY_EMC_UID, userName));
				} else {
					sharedHeaders.updateHeader(
						new BasicHeader(
							KEY_EMC_UID, subTenant.getValue() + "/" + userName
						)
					);
				}
			}
		}
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T, C> setSecret(final String secret) {
		super.setSecret(secret);
		LOG.trace(Markers.MSG, "Applying secret key {}", secret);
		secretKey = new SecretKeySpec(Base64.decodeBase64(secret), signMethod);
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T, C> setNameSpace(final String nameSpace) {
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
	public final WSRequestConfigImpl<T, C> setFileAccessEnabled(final boolean flag) {
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
	public final WSRequestConfigImpl<T, C> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		try {
			setSubTenant(
				new WSSubTenantImpl<>(
					this, runTimeConfig.getString(RunTimeConfig.KEY_API_ATMOS_SUBTENANT)
				)
			);
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, RunTimeConfig.KEY_API_ATMOS_SUBTENANT);
		}
		//
		if(runTimeConfig.getDataFileAccessEnabled()) {
			uriBasePath = PREFIX_URI + API_TYPE_FS;
		} else {
			uriBasePath = PREFIX_URI + API_TYPE_OBJ;
		}
		//
		return this;
	}
	//
	@Override
	public final ItemSrc<T> getContainerListInput(final long maxCount, final String addr) {
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
			LOG.debug(Markers.MSG, "Note: no subtenant has got from load client side");
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
		out.writeObject(subTenant == null ? null : subTenant.getValue());
		out.writeObject(uriBasePath);
	}
	//
	@Override
	protected final String getDataUriPath(final T dataItem) {
		if(dataItem == null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		if(fsAccess || !IOTask.Type.CREATE.equals(loadType)) {
			return uriBasePath + getFilePathFor(dataItem);
		} else { // "/rest/objects"
			return uriBasePath;
		}
	}
	@Override
	protected final String getContainerUriPath(final Container<T> container)
	throws IllegalArgumentException, URISyntaxException {
		throw new IllegalStateException("No container request is possible using Atmos API");
	}
	//
	private final static ThreadLocal<StringBuilder>
		THR_LOC_METADATA_STR_BUILDER = new ThreadLocal<>();
	@Override @SuppressWarnings("unchecked")
	protected final void applyMetaDataHeaders(final HttpEntityEnclosingRequest request) {
		StringBuilder md = THR_LOC_METADATA_STR_BUILDER.get();
		if(md == null) {
			md = new StringBuilder();
			THR_LOC_METADATA_STR_BUILDER.set(md);
		} else {
			md.setLength(0); // reset/clear
		}
		//
		if(subTenant != null) {
			final String subtenantId = subTenant.getValue();
			if(subtenantId != null && subtenantId.length() > 0) {
				md.append("subtenant=").append(subTenant.getValue());
			}
		}
		// the "offset" tag is required for WS mock
		if(IOTask.Type.CREATE.equals(loadType)) {
			final HttpEntity entity = request.getEntity();
			if(entity != null && WSObject.class.isInstance(entity)) {
				if(md.length() > 0) {
					md.append(',');
				}
				md.append("offset=").append(((T) request.getEntity()).getOffset());
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
		httpRequest.setHeader(KEY_EMC_SIG, getSignature(getCanonical(httpRequest)));
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
			} else if(sharedHeaders.containsHeader(headerName)) {
				canonical.append('\n').append(sharedHeaders.getFirstHeader(headerName).getValue());
			} else {
				canonical.append('\n');
			}
		}
		//
		final String uri = httpRequest.getRequestLine().getUri();
		canonical.append('\n').append(uri.contains("?") ? uri.substring(0, uri.indexOf("?")) : uri);
		//
		for(final String emcHeaderName: HEADERS_CANONICAL_EMC) {
			if(httpRequest.containsHeader(emcHeaderName)) {
				for(final Header emcHeader: httpRequest.getHeaders(emcHeaderName)) {
					canonical.append('\n').append(emcHeaderName.toLowerCase()).append(':').append(
						emcHeader.getValue()
					);
				}
			} else if(sharedHeaders.containsHeader(emcHeaderName)) {
				canonical
					.append('\n').append(emcHeaderName.toLowerCase())
					.append(':').append(sharedHeaders.getFirstHeader(emcHeaderName).getValue());
			}
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
		if(locationHeader != null && IOTask.Type.CREATE.equals(loadType)) {
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
		if(LOG.isTraceEnabled(Markers.MSG)) {
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
		// create the subtenant if neccessary
		final String subTenantValue = subTenant.getValue();
		if(subTenantValue == null || subTenantValue.length() == 0) {
			subTenant.create(storageAddrs[0]);
		}
		/*re*/setSubTenant(subTenant);
		runTimeConfig.set(RunTimeConfig.KEY_API_ATMOS_SUBTENANT, subTenant.getValue());
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
