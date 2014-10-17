package com.emc.mongoose.object.api.provider.atmos;
//
import com.emc.mongoose.object.api.WSRequestConfigBase;
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
//
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.UnsupportedEncodingException;
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
	private final static String
		KEY_SUBTENANT = "api.atmos.subtenant",
		OBJ_PATH =
			"/" + Main.RUN_TIME_CONFIG.getString("api.atmos.path.rest") +
			"/" + Main.RUN_TIME_CONFIG.getString("api.atmos.interface");
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
	public final WSRequestConfigImpl<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		try {
			setSubTenant(new WSSubTenant<>(this, this.runTimeConfig.getString(KEY_SUBTENANT)));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, KEY_SUBTENANT);
		}
		//
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
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
		setSubTenant(new WSSubTenant<>(this, String.class.cast(in.readObject())));
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
	protected final void applyURI(final HttpRequest httpRequest, final T dataItem)
	throws URISyntaxException {
		if(httpRequest==null) {
			throw new IllegalArgumentException(MSG_NO_REQ);
		}
		if(dataItem==null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		final String objId = dataItem.getId();
		synchronized(uriBuilder) {
			HttpRequestBase.class
				.cast(httpRequest)
				.setURI(
					uriBuilder
						.setPath(
							objId==null ? OBJ_PATH : OBJ_PATH + '/' + objId
						).build()
				);
		}
	}
	//
	@Override
	protected final void applyDateHeader(final HttpRequest httpRequest) {
		super.applyDateHeader(httpRequest);
		httpRequest.setHeader(
			KEY_EMC_DATE,
			httpRequest.getFirstHeader(HttpHeaders.DATE).getValue()
		);
	}
	//
	@Override
	protected final void applyAuthHeader(final HttpRequest httpRequest) {
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
	private final String HEADERS4CANONICAL[] = {
		HttpHeaders.CONTENT_MD5, HttpHeaders.CONTENT_TYPE, HttpHeaders.RANGE, HttpHeaders.DATE
	};
	//
	@Override
	public final String getCanonical(final HttpRequest httpRequest) {
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
		buffer.append('\n').append(HttpRequestBase.class.cast(httpRequest).getURI().getRawPath());
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
				valueLocation.startsWith(OBJ_PATH) &&
				valueLocation.length() - OBJ_PATH.length() > 1
			) {
				final String id = valueLocation.substring(OBJ_PATH.length() + 1);
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
				runTimeConfig.set(KEY_SUBTENANT, subTenantName);
			} else {
				throw new IllegalStateException(
					String.format("Created subtenant \"%s\" doesn't exist", subTenantName)
				);
			}
		}
	}
}
