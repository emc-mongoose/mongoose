package com.emc.mongoose.object.api.provider.atmos;
//
import com.emc.mongoose.object.api.WSRequestConfigBase;
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
//
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
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
	public final static String
		KEY_SUBTENANT = "api.atmos.subtenant",
		FMT_PATH =
			"/" + RunTimeConfig.getString("api.atmos.path.rest") +
			"/" + RunTimeConfig.getString("api.atmos.interface") + "/%x";
	//
	private WSSubTenant<T> subTenant;
	//
	public WSRequestConfigImpl()
	throws NoSuchAlgorithmException {
		super();
		api = WSRequestConfigImpl.class.getSimpleName();
	}
	//
	public final WSSubTenant<T> getSubTenant() {
		return subTenant;
	}
	//
	public final WSRequestConfigImpl<T> setSubTenant(final WSSubTenant<T> subTenant)
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
	public final WSRequestConfigImpl<T> setUserName(final String userName) {
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
	public final WSRequestConfigImpl<T> setProperties(final RunTimeConfig props) {
		super.setProperties(props);
		//
		try {
			setSubTenant(new WSSubTenant<>(this, RunTimeConfig.getString(KEY_SUBTENANT)));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, KEY_SUBTENANT);
		}
		//
		return this;
	}
	//
	@Override
	public WSRequestConfigImpl<T> clone()
	throws CloneNotSupportedException {
		final WSRequestConfigImpl copy = (WSRequestConfigImpl<T>) super.clone();
		copy.setNameSpace(getNameSpace());
		copy.setSubTenant(getSubTenant());
		return copy;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setSubTenant(new WSSubTenant<T>(this, String.class.cast(in.readObject())));
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(subTenant.getName());
	}
	//
	@Override
	protected final void applyURI(final HttpRequestBase httpRequest, final WSObject dataItem)
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
	@Override
	public String getCanonical(final HttpRequest httpRequest) {
		final StringBuilder buffer = new StringBuilder(httpRequest.getRequestLine().getMethod());
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
		buffer.append('\n').append(httpRequest.getRequestLine().getUri());
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
	@Override
	public final String getSignature(final String canonicalForm) {
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
	@Override
	public void configureStorage()
	throws IllegalStateException {
		if(subTenant == null) {
			throw new IllegalStateException("Subtenant is not specified");
		}
		final String subTenantName = subTenant.getName();
		if(subTenant.exists()) {
			LOG.debug(Markers.MSG, "Subtenant \"{}\" already exists", subTenantName);
		} else {
			subTenant.create();
			if(subTenant.exists()) {
				RunTimeConfig.set(KEY_SUBTENANT, subTenantName);
			} else {
				throw new IllegalStateException(
					String.format("Created subtenant \"%s\" doesn't exist", subTenantName)
				);
			}
		}
	}
}
