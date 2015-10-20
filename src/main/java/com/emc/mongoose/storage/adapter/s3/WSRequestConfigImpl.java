package com.emc.mongoose.storage.adapter.s3;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemSrc;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
//
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
//
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
				this.runTimeConfig.getDataVersioningEnabled()
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
		final String bucketName = String.class.cast(in.readObject());
		LOG.debug(Markers.MSG, "Note: bucket {} has been got from load client side", bucketName);
		setBucket(new WSBucketImpl<>(this, bucketName, runTimeConfig.getDataVersioningEnabled()));
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
	protected final String getUriPath(final T dataItem)
	throws IllegalStateException, URISyntaxException {
		if(bucket == null) {
			throw new IllegalArgumentException(MSG_NO_BUCKET);
		}
		if(dataItem == null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		applyObjectId(dataItem, null);
		return "/" + bucket + getFilePathFor(dataItem);
	}
	//
	@Override
	protected final void applyAuthHeader(final HttpRequest httpRequest) {
		httpRequest.setHeader(
			HttpHeaders.AUTHORIZATION,
			authPrefixValue + userName + ":" + getSignature(getCanonical(httpRequest))
		);
	}
	//
	private static String HEADERS_CANONICAL[] = {
		HttpHeaders.CONTENT_MD5, HttpHeaders.CONTENT_TYPE, HttpHeaders.DATE
	};
	//
	private final static ThreadLocal<StringBuilder>
		THR_LOC_CANONICAL_STR_BUILDER = new ThreadLocal<>();
	//
	@Override
	public final String getCanonical(final HttpRequest httpRequest) {
		//
		StringBuilder canonical = THR_LOC_CANONICAL_STR_BUILDER.get();
		if(canonical == null) {
			canonical = new StringBuilder();
			THR_LOC_CANONICAL_STR_BUILDER.set(canonical);
		} else {
			canonical.setLength(0); // reset/clear
		}
		canonical.append(httpRequest.getRequestLine().getMethod());
		//
		for(final String headerName : HEADERS_CANONICAL) {
			if(httpRequest.containsHeader(headerName)) {
				for(final Header header: httpRequest.getHeaders(headerName)) {
					canonical.append('\n').append(header.getValue());
				}
			} else if(sharedHeaders.containsHeader(headerName)) {
				canonical.append('\n').append(sharedHeaders.getFirstHeader(headerName).getValue());
			} else {
				canonical.append('\n');
			}
		}
		//
		for(final String emcHeaderName : HEADERS_CANONICAL_EMC) {
			if(sharedHeaders.containsHeader(emcHeaderName)) {
				canonical
					.append('\n').append(emcHeaderName.toLowerCase())
					.append(':').append(sharedHeaders.getFirstHeader(emcHeaderName).getValue());
			} else {
				for(final Header emcHeader : httpRequest.getHeaders(emcHeaderName)) {
					canonical
						.append('\n').append(emcHeaderName.toLowerCase())
						.append(':').append(emcHeader.getValue());
				}
			}
		}
		//
		final String uri = httpRequest.getRequestLine().getUri();
		canonical.append('\n');
		if(uri.contains("?") && !uri.endsWith("?" + Bucket.URL_ARG_VERSIONING)) {
			canonical.append(uri.substring(0, uri.indexOf("?")));
		} else {
			canonical.append(uri);
		}
		//
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Canonical representation:\n{}", canonical);
		}
		//
		return canonical.toString();
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final ItemSrc<T> getContainerListInput(final long maxCount, final String addr) {
		return new WSBucketItemSrc<>(bucket, addr, (Class<T>) BasicWSObject.class, maxCount);
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
		if(versioning) {
			bucket.setVersioning(storageNodeAddrs[0], true);
		}
	}
}
