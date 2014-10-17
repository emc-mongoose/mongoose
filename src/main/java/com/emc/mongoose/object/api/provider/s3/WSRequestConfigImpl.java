package com.emc.mongoose.object.api.provider.s3;
//
import com.emc.mongoose.object.api.WSRequestConfigBase;
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
//
import org.apache.logging.log4j.Level;
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
	public final static String
		FMT_PATH = "/%s/%x",
		KEY_BUCKET = "api.s3.bucket",
		MSG_NO_BUCKET = "Bucket is not specified",
		FMT_MSG_ERR_BUCKET_NOT_EXIST = "Created bucket \"%s\" still doesn't exist";
	private final String fmtAuthValue;
	//
	private WSBucketImpl<T> bucket;
	//
	public WSRequestConfigImpl()
	throws NoSuchAlgorithmException {
		super();
		api = WSRequestConfigImpl.class.getSimpleName();
		fmtAuthValue = runTimeConfig.getString("api.s3.auth.prefix") + " %s:%s";
	}
	//
	public final WSBucketImpl<T> getBucket() {
		return bucket;
	}
	//
	public final WSRequestConfigImpl<T> setBucket(final WSBucketImpl<T> bucket) {
		this.bucket = bucket;
		return this;
	}
	//
	@Override
	public final WSRequestConfigImpl<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		try {
			setBucket(new WSBucketImpl<T>(this, this.runTimeConfig.getString(KEY_BUCKET)));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, KEY_BUCKET);
		}
		//
		return this;
	}
	//
	@Override
	public WSRequestConfigImpl<T> clone()
	throws CloneNotSupportedException {
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
		setBucket(new WSBucketImpl<T>(this, String.class.cast(in.readObject())));
		LOG.trace(Markers.MSG, "Got bucket {}", bucket.getName());
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
	protected final void applyURI(final HttpRequest httpRequest, final WSObject dataItem)
	throws IllegalStateException, URISyntaxException {
		if(httpRequest == null) {
			throw new IllegalArgumentException(MSG_NO_REQ);
		}
		if(bucket == null) {
			throw new IllegalArgumentException(MSG_NO_BUCKET);
		}
		if(dataItem == null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		synchronized(uriBuilder) {
			try {
				HttpRequestBase.class.cast(httpRequest).setURI(
					uriBuilder.setPath(
						String.format(FMT_PATH, bucket.getName(), dataItem.getOffset())
					).build()
				);
			} catch(final Exception e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Request URI setting failure");
			}
		}
	}
	//
	@Override
	protected final void applyAuthHeader(final HttpRequest httpRequest) {
		httpRequest.addHeader(HttpHeaders.CONTENT_MD5, ""); // checksum of the data item is not avalable before streaming
		httpRequest.setHeader(
			HttpHeaders.AUTHORIZATION,
			String.format(fmtAuthValue, userName, getSignature(getCanonical(httpRequest)))
		);
		httpRequest.removeHeader(httpRequest.getLastHeader(HttpHeaders.CONTENT_MD5)); // remove temporary header
	}
	//
	private final String HEADERS4CANONICAL[] = {
		HttpHeaders.CONTENT_MD5, HttpHeaders.CONTENT_TYPE, HttpHeaders.DATE
	};
	//
	@Override
	public final String getCanonical(final HttpRequest httpRequest) {
		StringBuffer buffer = new StringBuffer(httpRequest.getRequestLine().getMethod());
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
		buffer.append('\n').append(HttpRequestBase.class.cast(httpRequest).getURI().getRawPath());
		//
		LOG.trace(Markers.MSG, "Canonical request representation:\n{}", buffer);
		//
		return buffer.toString();
	}
	//
	@Override
	public final void applyObjectId(final T dataObject, final HttpResponse httpResponse) {
		dataObject.setId(Long.toHexString(dataObject.getOffset()));
	}
	//
	@Override
	public final void configureStorage()
	throws IllegalStateException {
		if(bucket == null) {
			throw new IllegalStateException("Bucket is not specified");
		}
		final String bucketName = bucket.getName();
		if(bucket.exists()) {
			LOG.debug(Markers.MSG, "Bucket \"{}\" already exists", bucketName);
		} else {
			bucket.create();
			if(bucket.exists()) {
				runTimeConfig.set(KEY_BUCKET, bucketName);
			} else {
				throw new IllegalStateException(
					String.format(FMT_MSG_ERR_BUCKET_NOT_EXIST, bucketName)
				);
			}
		}
	}
}
