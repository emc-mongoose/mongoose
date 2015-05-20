package com.emc.mongoose.storage.adapter.atmos;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.impl.io.req.conf.WSRequestConfigBase;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.commons.codec.binary.Base64;
//
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URISyntaxException;
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
	private final static String KEY_SUBTENANT = "api.type.atmos.subtenant";
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
	protected WSRequestConfigImpl(final WSRequestConfigImpl<T> reqConf2Clone)
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
	public WSRequestConfigImpl<T> clone() {
		WSRequestConfigImpl<T> copy = null;
		try {
			copy = new WSRequestConfigImpl<>(this);
		} catch(final NoSuchAlgorithmException e) {
			LOG.fatal(LogUtil.ERR, "No such algorithm: \"{}\"", signMethod);
		}
		return copy;
	}
	//
	@Override
	public MutableWSRequest.HTTPMethod getHTTPMethod() {
		MutableWSRequest.HTTPMethod method;
		switch(loadType) {
			case CREATE:
				method = MutableWSRequest.HTTPMethod.POST;
				break;
			case READ:
				method = MutableWSRequest.HTTPMethod.GET;
				break;
			case DELETE:
				method = MutableWSRequest.HTTPMethod.DELETE;
				break;
			default: // UPDATE, APPEND
				method = MutableWSRequest.HTTPMethod.PUT;
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
	public final WSRequestConfigImpl<T> setUserName(final String userName)
	throws IllegalStateException {
		if(userName == null) {
			throw new IllegalStateException("User name is not specified for Atmos REST API");
		} else {
			super.setUserName(userName);
			if(sharedHeaders != null) {
				if(subTenant==null || subTenant.getValue().length() < 1) {
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
	public final WSRequestConfigBase<T> setSecret(final String secret) {
		super.setSecret(secret);
		LOG.trace(LogUtil.MSG, "Applying secret key {}", secret);
		secretKey = new SecretKeySpec(Base64.decodeBase64(secret), signMethod);
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T> setNameSpace(final String nameSpace) {
		super.setNameSpace(nameSpace);
		//if(nameSpace == null || nameSpace.length() < 1) {
			LOG.debug(LogUtil.MSG, "Using empty namespace");
		/*} else {
			sharedHeaders.updateHeader(new BasicHeader(KEY_EMC_NS, nameSpace));
		}*/
		return this;
	}
	//
	@Override
	public final WSRequestConfigImpl<T> setFileAccessEnabled(final boolean flag) {
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
	public final WSRequestConfigImpl<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		try {
			setSubTenant(new WSSubTenantImpl<>(this, runTimeConfig.getString(KEY_SUBTENANT)));
		} catch(final NoSuchElementException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, KEY_SUBTENANT);
		}
		//
		if(runTimeConfig.getStorageFileAccessEnabled()) {
			uriBasePath = PREFIX_URI + API_TYPE_FS;
		} else {
			uriBasePath = PREFIX_URI + API_TYPE_OBJ;
		}
		//
		return this;
	}
	//
	@Override
	public final Producer<T> getAnyDataProducer(final long maxCount, final String addr) {
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
			LOG.debug(LogUtil.MSG, "Note: no subtenant has got from load client side");
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
	protected final void applyURI(final MutableWSRequest httpRequest, final T dataItem) {
		if(httpRequest == null) {
			throw new IllegalArgumentException(MSG_NO_REQ);
		}
		if(dataItem == null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		if(fsAccess || !IOTask.Type.CREATE.equals(loadType)) {
			httpRequest.setUriPath(uriBasePath + "/" + dataItem.getId());
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
			if(sharedHeaders.containsHeader(headerName)) {
				buffer.append('\n').append(sharedHeaders.getFirstHeader(headerName).getValue());
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
			if(sharedHeaders.containsHeader(emcHeaderName)) {
				buffer
					.append('\n').append(emcHeaderName.toLowerCase())
					.append(':').append(sharedHeaders.getFirstHeader(emcHeaderName).getValue());
			} else {
				for(final Header emcHeader: httpRequest.getHeaders(emcHeaderName)) {
					buffer.append('\n').append(emcHeaderName.toLowerCase()).append(':').append(
						emcHeader.getValue()
					);
				}
			}
		}
		//
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(LogUtil.MSG, "Canonical request form:\n{}", buffer.toString());
		}
		//
		return buffer.toString();
	}
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
					LOG.trace(LogUtil.ERR, "Got empty object id");
				}
			} else if(LOG.isTraceEnabled(LogUtil.ERR)) {
				LOG.trace(
					LogUtil.ERR, "Invalid response location header value: \"{}\"", valueLocation
				);
			}
		}
		//
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(
				LogUtil.MSG, "Applied object \"{}\" id \"{}\" from the source \"{}\"",
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
	public void configureStorage(final String storageAddrs[])
	throws IllegalStateException {
		/* show interesting system info
		try {
			MutableWSRequest req = WSIOTask.HTTPMethod.GET
				.createRequest()
				.setUriPath("/rest/service");
			applyHeadersFinally(req);
			final HttpResponse resp = WSLoadExecutor.class.cast(client).execute(req);
			if(resp != null) {
				final StatusLine statusLine = resp.getStatusLine();
				if(statusLine != null) {
					final int statusCode = statusLine.getStatusCode();
					if(statusCode >= 200 && statusCode < 300) {
						final HttpEntity contentEntity = resp.getEntity();
						if(contentEntity != null && contentEntity.getContentLength() > 0) {
							try(
								final BufferedReader streamReader = new BufferedReader(
									new InputStreamReader(
										contentEntity.getContent()
									)
								)
							) {
								final StrBuilder strBuilder = new StrBuilder();
								String nextLine;
								do {
									nextLine = streamReader.readLine();
									strBuilder.append(nextLine).appendNewLine();
								} while(nextLine != null);
								LOG.info(Markers.MSG, strBuilder.toString());
							} catch(final IOException e) {
								TraceLogger.failure(
									LOG, Level.DEBUG, e,
									"Atmos system info response content reading failure"
								);
							}
						}
					}
				}
			}
		} catch(final IOException e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Atmos system info request failure");
		}*/
		// create the subtenant if neccessary
		final String subTenantValue = subTenant.getValue();
		if(subTenantValue == null || subTenantValue.length() == 0) {
			subTenant.create(storageAddrs[0]);
		}
		/*re*/setSubTenant(subTenant);
		runTimeConfig.set(KEY_SUBTENANT, subTenant.getValue());
	}
	//
	@Override
	public void receiveResponse(final HttpResponse response, final T dataItem) {
		applyObjectId(dataItem, response);
	}
}
