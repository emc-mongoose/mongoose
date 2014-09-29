package com.emc.mongoose.object.api.impl.provider.http;
//
import com.emc.mongoose.object.api.impl.WSRequestConfigBase;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.object.data.WSDataObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
//
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
/**
 Created by kurila on 26.03.14.
 */
public final class Atmos
extends WSRequestConfigBase<WSDataObject> {
	//
	private final static Logger LOG = LogManager.getLogger();
	public final static String
		FMT_PATH =
			"/" + RunTimeConfig.getString("api.atmos.path.rest") +
			"/" + RunTimeConfig.getString("api.atmos.interface") + "/%x";
	//
	private String subTenant;
	//
	public
	Atmos() {
		api = Atmos.class.getSimpleName();
	}
	//
	public final String getSubTenant() {
		return subTenant;
	}
	//
	public final
	Atmos setSubTenant(final String subTenant)
	throws IllegalStateException {
		this.subTenant = subTenant;
		if(subTenant==null) {
			throw new IllegalStateException("Subtenant is not specified for Atmos REST API");
		} else if(userName!=null) {
			sharedHeadersMap.put(KEY_EMC_UID, subTenant+'/'+userName);
		}
		return this;
	}
	//
	@Override
	public final
	Atmos setUserName(final String userName) {
		super.setUserName(userName);
		if(userName==null) {
			throw new IllegalStateException("User name is not specified for Atmos REST API");
		} else if(subTenant!=null) {
			sharedHeadersMap.put(KEY_EMC_UID, subTenant+'/'+userName);
		}
		return this;
	}
	//
	@Override
	public final
	Atmos setProperties(final RunTimeConfig props) {
		super.setProperties(props);
		//
		final String paramName = "api.atmos.subtenant";
		try {
			setSubTenant(RunTimeConfig.getString(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		}
		//
		return this;
	}
	//
	@Override
	public
	Atmos clone() {
		final Atmos copy = new Atmos();
		copy.setAddr(getAddr());
		copy.setLoadType(getLoadType());
		copy.setPort(getPort());
		copy.setUserName(getUserName());
		copy.setSecret(getSecret());
		copy.setScheme(getScheme());
		copy.setClient(getClient());
		copy.setNameSpace(getNameSpace());
		copy.setSubTenant(getSubTenant());
		return copy;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setSubTenant(String.class.cast(in.readObject()));
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(subTenant);
	}
	//
	@Override
	protected final void applyURI(final HttpRequestBase httpRequest, final WSDataObject dataItem)
	throws URISyntaxException {
		if(httpRequest==null) {
			throw new IllegalArgumentException(MSG_NO_REQ);
		}
		if(dataItem==null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		synchronized(uriBuilder) {
			httpRequest.setURI(
				uriBuilder.setPath(
					String.format(FMT_PATH, dataItem.getId())
				).build()
			);
		}
	}
	//
	@Override
	protected final void applyAuthHeader(final HttpRequestBase httpRequest) {
		if(httpRequest.getLastHeader(HttpHeaders.RANGE)==null) {
			httpRequest.addHeader(HttpHeaders.RANGE, ""); // temporary required for canonical form
		}
		//
		httpRequest.addHeader(KEY_EMC_SIG, getSignature(getCanonical(httpRequest)));
		//
		final Header headerLastRange = httpRequest.getLastHeader(HttpHeaders.RANGE);
		if(headerLastRange!=null && headerLastRange.getValue().length()==0) { // the header is temp
			httpRequest.removeHeader(headerLastRange);
		}

	}
	//
	protected String getCanonical(final HttpRequestBase httpRequest) {
		final StringBuilder buffer = new StringBuilder(httpRequest.getMethod());
		//Map<String, String> sharedHeaders = sharedConfig.getSharedHeaders();
		Header header;
		for(final String headerName: HEADERS4CANONICAL) {
			header = httpRequest.getFirstHeader(headerName);
			if(header!=null) {
				buffer.append('\n').append(header.getValue());
			}
			if(sharedHeadersMap.containsKey(headerName)) {
				buffer.append('\n').append(sharedHeadersMap.get(headerName));
			}
		}
		//
		buffer.append('\n').append(httpRequest.getURI().getRawPath());
		//
		for(final String emcHeaderName: HEADERS_EMC) {
			header = httpRequest.getFirstHeader(emcHeaderName);
			if(header!=null) {
				buffer
					.append('\n').append(emcHeaderName)
					.append(':').append(header.getValue());
			}
			if(sharedHeadersMap.containsKey(emcHeaderName)) {
				buffer
					.append('\n').append(emcHeaderName)
					.append(':').append(sharedHeadersMap.get(emcHeaderName));
			}
		}
		//
		return buffer.toString();
	}
	//
	protected final String getSignature(final String canonicalForm) {
		LOG.trace(Markers.MSG, "Canonical form: {}", canonicalForm);
		byte[] signature = null;
		try {
			synchronized(mac) {
				signature = mac.doFinal(canonicalForm.getBytes(DEFAULT_ENC));
			}
		} catch(Exception e) {
			LOG.error(e);
		}
		final String signature64 = Base64.encodeBase64String(signature);
		LOG.trace(Markers.MSG, "Calculated signature: '{}'", signature64);
		return signature64;
	}
	//
}
