package com.emc.mongoose.web.api.impl.provider.s3;
//
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.logging.MessageFactoryImpl;
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
import com.emc.mongoose.web.data.WSObject;
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
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
/**
 Created by kurila on 26.03.14.
 */
public final class RequestConfig<T extends WSObject>
extends WSRequestConfigBase<T> {
	//
	private Logger log = LogManager.getLogger(new MessageFactoryImpl(Main.RUN_TIME_CONFIG));
	//
	public final static String
		FMT_PATH = "/%s/%s",
		KEY_BUCKET = "api.s3.bucket",
		MSG_NO_BUCKET = "Bucket is not specified",
		FMT_MSG_ERR_BUCKET_NOT_EXIST = "Created bucket \"%s\" still doesn't exist";
	private final String fmtAuthValue;
	//
	private Bucket<T> bucket;
	//
	public RequestConfig()
	throws NoSuchAlgorithmException {
		this(null);
	}
	//
	protected RequestConfig(final RequestConfig<T> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
		setAPI(RequestConfig.class.getSimpleName());
		fmtAuthValue = runTimeConfig.getString("api.s3.auth.prefix") + " %s:%s";
		if(reqConf2Clone != null) {
			setBucket(reqConf2Clone.getBucket());
			setNameSpace(reqConf2Clone.getNameSpace());
		}
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public RequestConfig<T> clone() {
		RequestConfig<T> copy = null;
		try {
			copy = new RequestConfig<>(this);
		} catch(final NoSuchAlgorithmException e) {
			log.fatal(Markers.ERR, "No such algorithm: \"{}\"", signMethod);
		}
		return copy;
	}
	//
	public final Bucket<T> getBucket() {
		return bucket;
	}
	//
	public final RequestConfig<T> setBucket(final Bucket<T> bucket) {
		this.bucket = bucket;
		return this;
	}
	//
	@Override
	public final RequestConfig<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		log = LogManager.getLogger(new MessageFactoryImpl(runTimeConfig));
		//
		try {
			setBucket(new Bucket<T>(this, this.runTimeConfig.getString(KEY_BUCKET)));
		} catch(final NoSuchElementException e) {
			log.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, KEY_BUCKET);
		}
		//
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setBucket(new Bucket<T>(this, String.class.cast(in.readObject())));
		log.trace(Markers.MSG, "Got bucket {}", bucket.getName());
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
	protected final void applyURI(final HttpRequest httpRequest, final T dataItem)
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
		applyObjectId(dataItem, null); // S3 REST doesn't require http response
		synchronized(uriBuilder) {
			try {
				HttpRequestBase.class.cast(httpRequest).setURI(
					uriBuilder.setPath(
						String.format(FMT_PATH, bucket.getName(), dataItem.getId())
					).build()
				);
			} catch(final Exception e) {
				ExceptionHandler.trace(log, Level.WARN, e, "Request URI setting failure");
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
		log.trace(Markers.MSG, "Canonical request representation:\n{}", buffer);
		//
		return buffer.toString();
	}
	//
	@Override
	public final void applyObjectId(final T dataObject, final HttpResponse unused) {
		dataObject.setId(
			Base64.encodeBase64URLSafeString(
				ByteBuffer
					.allocate(Long.SIZE / Byte.SIZE)
					.putLong(dataObject.getOffset())
					.array()
			)
		);
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
			log.debug(Markers.MSG, "Bucket \"{}\" already exists", bucketName);
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
