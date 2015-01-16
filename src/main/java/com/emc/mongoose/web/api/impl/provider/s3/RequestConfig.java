package com.emc.mongoose.web.api.impl.provider.s3;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.web.api.MutableHTTPRequest;
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.web.data.impl.BasicWSObject;
//
import com.emc.mongoose.web.load.WSLoadExecutor;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
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
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static String
		FMT_PATH = "/%s/%s",
		KEY_BUCKET = "api.s3.bucket",
		KEY_BUCKET_FILESYSTEM = KEY_BUCKET + ".filesystem",
		KEY_BUCKET_VERSIONING = KEY_BUCKET + ".versioning",
		MSG_NO_BUCKET = "Bucket is not specified",
		FMT_MSG_ERR_BUCKET_NOT_EXIST = "Created bucket \"%s\" still doesn't exist";
	private final String fmtAuthValue;
	//
	private Bucket<T> bucket;
	private boolean bucketFileSystem = false, bucketVersioning = false;
	//
	public RequestConfig()
	throws NoSuchAlgorithmException {
		this(null);
	}
	//
	protected RequestConfig(final RequestConfig<T> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
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
			LOG.fatal(Markers.ERR, "No such algorithm: \"{}\"", signMethod);
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
	public final boolean getBucketFileSystem() {
		return bucketFileSystem;
	}
	//
	public final RequestConfig<T> setBucketFileSystem(final boolean flag) {
		this.bucketFileSystem = flag;
		return this;
	}
	//
	public final boolean getBucketVersioning() {
		return bucketVersioning;
	}
	//
	public final RequestConfig<T> setBucketVersioning(final boolean flag) {
		this.bucketVersioning = flag;
		return this;
	}
	//
	@Override
	public final RequestConfig<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		setBucketFileSystem(runTimeConfig.getBoolean(KEY_BUCKET_FILESYSTEM));
		setBucketVersioning(runTimeConfig.getBoolean(KEY_BUCKET_VERSIONING));
		//
		try {
			setBucket(new Bucket<T>(this, this.runTimeConfig.getString(KEY_BUCKET)));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, KEY_BUCKET);
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
	protected final void applyURI(final MutableHTTPRequest httpRequest, final T dataItem)
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
		httpRequest.setUriPath(String.format(FMT_PATH, bucket.getName(), dataItem.getId()));
	}
	//
	@Override
	protected final void applyAuthHeader(final MutableHTTPRequest httpRequest) {
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
	public final String getCanonical(final MutableHTTPRequest httpRequest) {
		final StringBuffer buffer = new StringBuffer(httpRequest.getRequestLine().getMethod());
		//
		for(final String headerName: HEADERS4CANONICAL) {
			// support for multiple non-unique header keys
			for(final Header header: httpRequest.getHeaders(headerName)) {
				buffer.append('\n').append(header.getValue());
			}
			if(sharedHeadersMap.containsKey(headerName)) {
				buffer.append('\n').append(sharedHeadersMap.get(headerName));
			}
		}
		//
		for(final String emcHeaderName: HEADERS_EMC) {
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
		buffer.append('\n').append(httpRequest.getUriPath());
		//
		LOG.trace(Markers.MSG, "Canonical request representation:\n{}", buffer);
		//
		return buffer.toString();
	}
	//
	protected final void applyObjectId(final T dataObject, final HttpResponse unused) {
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
	@Override @SuppressWarnings("unchecked")
	public final Producer<T> getAnyDataProducer(final long maxCount, final LoadExecutor<T> client) {
		Producer<T> producer = null;
		if(anyDataProducerEnabled) {
			try {
				producer = new BucketProducer<>(
					bucket, BasicWSObject.class, maxCount, (WSLoadExecutor<T>) client
				);
			} catch(final NoSuchMethodException e) {
				TraceLogger.failure(LOG, Level.ERROR, e, "Unexpected failure");
			}
		}
		return producer;
	}
	//
	@Override
	public final void configureStorage(final LoadExecutor<T> loadExecutor)
	throws IllegalStateException {
		if(bucket == null) {
			throw new IllegalStateException("Bucket is not specified");
		}
		final String bucketName = bucket.getName();
		if(bucket.exists(loadExecutor)) {
			LOG.debug(Markers.MSG, "Bucket \"{}\" already exists", bucketName);
		} else {
			bucket.create(loadExecutor);
			if(bucket.exists(loadExecutor)) {
				runTimeConfig.set(KEY_BUCKET, bucketName);
			} else {
				throw new IllegalStateException(
					String.format(FMT_MSG_ERR_BUCKET_NOT_EXIST, bucketName)
				);
			}
		}
	}
}
