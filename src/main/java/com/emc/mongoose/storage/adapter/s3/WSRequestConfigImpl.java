package com.emc.mongoose.storage.adapter.s3;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.data.WSObject;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.io.req.conf.WSRequestConfigBase;
import com.emc.mongoose.core.impl.data.BasicWSObject;
//
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
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
	public final static String
		KEY_BUCKET_NAME = "api.type.s3.bucket",
		MSG_NO_BUCKET = "Bucket is not specified",
		FMT_MSG_ERR_BUCKET_NOT_EXIST = "Created bucket \"%s\" still doesn't exist";
	private final String authPrefixValue;
	//
	private WSBucketImpl<T> bucket;
	//
	public WSRequestConfigImpl()
	throws NoSuchAlgorithmException {
		this(null);
	}
	//
	protected WSRequestConfigImpl(final WSRequestConfigImpl<T> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
		authPrefixValue = runTimeConfig.getApiS3AuthPrefix() + " ";
		if(reqConf2Clone != null) {
			setBucket(reqConf2Clone.getBucket());
			setNameSpace(reqConf2Clone.getNameSpace());
		}
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public WSRequestConfigImpl<T> clone() {
		WSRequestConfigImpl<T> copy = null;
		try {
			copy = new WSRequestConfigImpl<>(this);
		} catch(final NoSuchAlgorithmException e) {
			LOG.fatal(Markers.ERR, "No such algorithm: \"{}\"", signMethod);
		}
		return copy;
	}
	//
	public final WSBucketImpl<T> getBucket() {
		return bucket;
	}
	//
	public final WSRequestConfigImpl<T> setBucket(final WSBucketImpl<T> bucket) {
		LOG.debug(Markers.MSG, "Req conf instance #{}: set bucket \"{}\"", hashCode(), bucket);
		this.bucket = bucket;
		return this;
	}
	//
	@Override
	public final WSRequestConfigBase<T> setNameSpace(final String nameSpace) {
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
	public final WSRequestConfigImpl<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		try {
			final WSBucketImpl<T> bucket = new WSBucketImpl<>(
				this, this.runTimeConfig.getString(KEY_BUCKET_NAME),
				this.runTimeConfig.getStorageVersioningEnabled()
			);
			setBucket(bucket);
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, KEY_BUCKET_NAME);
		}
		//
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		final String bucketName = String.class.cast(ObjectInputStream.class.cast(in).readUnshared());
		LOG.debug(Markers.MSG, "Note: bucket {} has been got from load client side", bucketName);
		setBucket(new WSBucketImpl<>(this, bucketName, runTimeConfig.getStorageVersioningEnabled()));
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		ObjectOutputStream.class.cast(out).writeUnshared(bucket.getName());
	}
	//
	@Override
	protected final void applyURI(final MutableWSRequest httpRequest, final T dataItem)
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
		httpRequest.setUriPath("/" + bucket + "/" + dataItem.getId());
	}
	//
	@Override
	protected final void applyAuthHeader(final MutableWSRequest httpRequest) {
		httpRequest.setHeader(
			HttpHeaders.AUTHORIZATION,
			authPrefixValue + userName + ":" + getSignature(getCanonical(httpRequest))
		);
	}
	//
	private static String HEADERS4CANONICAL[] = {
		HttpHeaders.CONTENT_MD5, HttpHeaders.CONTENT_TYPE, HttpHeaders.DATE
	};
	//
	@Override
	public final String getCanonical(final MutableWSRequest httpRequest) {
		final StringBuffer buffer = new StringBuffer(httpRequest.getRequestLine().getMethod());
		//
		for(final String headerName : HEADERS4CANONICAL) {
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
		for(final String emcHeaderName : HEADERS_EMC) {
			if(sharedHeaders.containsHeader(emcHeaderName)) {
				buffer
					.append('\n').append(emcHeaderName.toLowerCase())
					.append(':').append(sharedHeaders.getFirstHeader(emcHeaderName).getValue());
			} else {
				for(final Header emcHeader : httpRequest.getHeaders(emcHeaderName)) {
					buffer
						.append('\n').append(emcHeaderName.toLowerCase())
						.append(':').append(emcHeader.getValue());
				}
			}
		}
		//
		buffer.append('\n').append(httpRequest.getUriPath());
		//
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Canonical representation:\n{}", buffer);
		}
		//
		return buffer.toString();
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final Producer<T> getAnyDataProducer(final long maxCount, final String addr) {
		Producer<T> producer = null;
		if(anyDataProducerEnabled) {
			try {
				producer = new WSBucketProducer<>(bucket, BasicWSObject.class, maxCount, addr);
			} catch(final NoSuchMethodException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
			}
		} else {
			LOG.debug(
				Markers.MSG, "req conf {}: using of bucket listing data producer is suppressed",
				hashCode()
			);
		}
		return producer;
	}
	//
	@Override
	public final void configureStorage(final String[] storageNodeAddrs)
	throws IllegalStateException {
		if(bucket == null) {
			throw new IllegalStateException("Bucket is not specified");
		} else {
			LOG.debug(Markers.MSG, "Configure storage w/ bucket \"{}\"", bucket);
		}
		final String bucketName = bucket.getName();
		if(bucket.exists(storageNodeAddrs[0])) {
			LOG.info(Markers.MSG, "Bucket \"{}\" already exists", bucketName);
		} else {
			LOG.debug(Markers.MSG, "Bucket \"{}\" doesn't exist, trying to create", bucketName);
			bucket.create(storageNodeAddrs[0]);
			if(bucket.exists(storageNodeAddrs[0])) {
				runTimeConfig.set(KEY_BUCKET_NAME, bucketName);
			} else {
				throw new IllegalStateException(
					String.format(FMT_MSG_ERR_BUCKET_NOT_EXIST, bucketName)
				);
			}
		}
	}
}
