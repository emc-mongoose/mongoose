package com.emc.mongoose.object.api.provider.s3;
//
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.api.WSRequestConfigBase;
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpRequestBase;
//
import org.apache.http.impl.client.CloseableHttpClient;
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
public final class WSRequestConfigImpl<T extends WSObject>
extends WSRequestConfigBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	public final static String
		FMT_PATH = "/%s/%x",
		FMT_AUTH_VALUE = RunTimeConfig.getString("api.s3.auth.prefix") + " %s:%s",
		MSG_NO_BUCKET = "Bucket is not specified";
	//
	private WSBucket<T> bucket;
	//
	public WSRequestConfigImpl() {
		api = WSRequestConfigImpl.class.getSimpleName();
	}
	//
	public final WSBucket<T> getBucket() {
		return bucket;
	}
	//
	public final WSRequestConfigImpl<T> setBucket(final WSBucket<T> bucket) {
		this.bucket = bucket;
		return this;
	}
	//
	@Override
	public final WSRequestConfigImpl<T> setClient(final CloseableHttpClient httpClient) {
		super.setClient(httpClient);
		bucket.create();
		return this;
	}
	//
	@Override
	public final WSRequestConfigImpl<T> setProperties(final RunTimeConfig props) {
		super.setProperties(props);
		//
		final String paramName = "api.s3.bucket";
		try {
			setBucket(new WSBucket<T>(this, RunTimeConfig.getString(paramName)));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		}
		//
		return this;
	}
	//
	@Override
	public WSRequestConfigImpl<T> clone() {
		final WSRequestConfigImpl<T> copy = (WSRequestConfigImpl<T>) super.clone();
		copy.setNameSpace(getNameSpace());
		copy.setBucket(getBucket());
		return copy;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setBucket(new WSBucket<T>(this, String.class.cast(in.readObject())));
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(bucket.getName());
	}
	//
	@Override
	protected final void applyURI(final HttpRequestBase httpRequest, final WSObject dataItem)
	throws IllegalStateException, URISyntaxException {
		if(httpRequest==null) {
			throw new IllegalArgumentException(MSG_NO_REQ);
		}
		if(bucket==null) {
			throw new IllegalArgumentException(MSG_NO_BUCKET);
		}
		if(dataItem==null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		synchronized(uriBuilder) {
			httpRequest.setURI(
				uriBuilder.setPath(
					String.format(FMT_PATH, bucket, dataItem.getId())
				).build()
			);
		}
	}
	//
	@Override
	protected final void applyAuthHeader(final HttpRequestBase httpRequest) {
		httpRequest.addHeader(HttpHeaders.CONTENT_MD5, ""); // checksum of the data item is not avalable before streaming
		httpRequest.setHeader(
			HttpHeaders.AUTHORIZATION,
			String.format(FMT_AUTH_VALUE, userName, getSignature(getCanonical(httpRequest)))
		);
		httpRequest.removeHeader(httpRequest.getLastHeader(HttpHeaders.CONTENT_MD5)); // remove temporary header
	}
	//
	protected String getCanonical(final HttpRequestBase httpRequest) {
		StringBuffer buffer = new StringBuffer(httpRequest.getMethod());
		//
		for(String headerName: HEADERS4CANONICAL) {
			// support for multiple non-unique header keys
			for(final Header header: httpRequest.getHeaders(headerName)) {
				buffer.append('\n').append(header.getValue());
			}
			if(sharedHeadersMap.containsKey(headerName)) {
				buffer.append('\n').append(sharedHeadersMap.get(headerName));
			}
		}
		//
		for(String emcHeaderName: HEADERS_EMC) {
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
		buffer.append('\n').append(httpRequest.getURI().getRawPath());
		//
		LOG.trace(Markers.MSG, "Canonical request representation:\n{}", buffer);
		//
		return buffer.toString();
	}
	//
	protected final String getSignature(final String canonicalForm) {
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
