package com.emc.mongoose.object.api.provider.s3;
//
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.UUID;
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

	//
	private String bucketName;
	//
	public WSRequestConfigImpl() {
		api = WSRequestConfigImpl.class.getSimpleName();
	}
	//
	public final String getBucketName() {
		return bucketName;
	}
	//
	public final WSRequestConfigImpl setBucketName(final String bucketName)
	throws IllegalStateException {
		if(bucketName == null || bucketName.length()==0) {
			final WSBucket<T> newBucket = new WSBucket<>(this, UUID.randomUUID().toString());
			newBucket.create();
			this.bucketName = newBucket.getName();
		} else {
			this.bucketName = bucketName;
		}
		return this;
	}
	//
	@Override
	public final WSRequestConfigImpl setProperties(final RunTimeConfig props) {
		super.setProperties(props);
		//
		final String paramName = "api.s3.bucket";
		try {
			setBucketName(RunTimeConfig.getString(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		}
		//
		return this;
	}
	//
	@Override
	public WSRequestConfigImpl clone() {
		final WSRequestConfigImpl copy = WSRequestConfigImpl.class.cast(super.clone());
		copy.setNameSpace(getNameSpace());
		copy.setBucketName(getBucketName());
		return copy;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setBucketName(String.class.cast(in.readObject()));
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(bucketName);
	}
	//
	@Override
	protected final void applyURI(final HttpRequestBase httpRequest, final WSObject dataItem)
	throws IllegalStateException, URISyntaxException {
		if(httpRequest==null) {
			throw new IllegalArgumentException(MSG_NO_REQ);
		}
		if(bucketName==null) {
			throw new IllegalArgumentException(MSG_NO_BUCKET);
		}
		if(dataItem==null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		synchronized(uriBuilder) {
			httpRequest.setURI(
				uriBuilder.setPath(
					String.format(FMT_PATH, bucketName, dataItem.getId())
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
