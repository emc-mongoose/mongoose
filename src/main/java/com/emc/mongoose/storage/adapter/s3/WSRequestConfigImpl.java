package com.emc.mongoose.storage.adapter.s3;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemSrc;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.container.BasicContainer;
import com.emc.mongoose.core.impl.io.conf.WSRequestConfigBase;
//
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
//
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 26.03.14.
 */
public final class WSRequestConfigImpl<T extends WSObject, C extends Container<T>>
extends WSRequestConfigBase<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static String
		KEY_BUCKET_NAME = "api.type.s3.bucket",
		MSG_NO_BUCKET = "Bucket is not specified",
		FMT_MSG_ERR_BUCKET_NOT_EXIST = "Created bucket \"%s\" still doesn't exist";
	private final String authPrefixValue;
	//
	public WSRequestConfigImpl()
	throws NoSuchAlgorithmException {
		this(null);
	}
	//
	protected WSRequestConfigImpl(final WSRequestConfigImpl<T, C> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
		authPrefixValue = runTimeConfig.getApiS3AuthPrefix() + " ";
		if(reqConf2Clone != null) {
			setNameSpace(reqConf2Clone.getNameSpace());
		}
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public WSRequestConfigImpl<T, C> clone() {
		WSRequestConfigImpl<T, C> copy = null;
		try {
			copy = new WSRequestConfigImpl<>(this);
		} catch(final NoSuchAlgorithmException e) {
			LOG.fatal(Markers.ERR, "No such algorithm: \"{}\"", signMethod);
		}
		return copy;
	}
	//
	@Override
	public final WSRequestConfigBase<T, C> setNameSpace(final String nameSpace) {
		super.setNameSpace(nameSpace);
		//if(nameSpace == null || nameSpace.length() < 1) {
			LOG.debug(Markers.MSG, "Using empty namespace");
		/*} else {
			sharedHeaders.updateHeader(new BasicHeader(KEY_EMC_NS, nameSpace));
		}*/
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final WSRequestConfigImpl<T, C> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		try {
			setContainer((C) new BasicContainer<T>(this.runTimeConfig.getString(KEY_BUCKET_NAME)));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, KEY_BUCKET_NAME);
		}
		//
		return this;
	}
	//
	@Override
	protected final String getDataUriPath(final T dataItem)
	throws IllegalStateException, URISyntaxException {
		if(container == null) {
			throw new IllegalArgumentException(MSG_NO_BUCKET);
		}
		if(dataItem == null) {
			throw new IllegalArgumentException(MSG_NO_DATA_ITEM);
		}
		applyObjectId(dataItem, null);
		return getContainerUriPath(container) + getFilePathFor(dataItem);
	}
	//
	@Override
	protected final String getContainerUriPath(final Container<T> container)
	throws IllegalArgumentException, URISyntaxException {
		return "/" + container.getName();
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
			if(httpRequest.containsHeader(emcHeaderName)) {
				for(final Header emcHeader : httpRequest.getHeaders(emcHeaderName)) {
					canonical
						.append('\n').append(emcHeaderName.toLowerCase())
						.append(':').append(emcHeader.getValue());
				}
			} else if(sharedHeaders.containsHeader(emcHeaderName)) {
				canonical
					.append('\n').append(emcHeaderName.toLowerCase())
					.append(':').append(sharedHeaders.getFirstHeader(emcHeaderName).getValue());
			}
		}
		//
		final String uri = httpRequest.getRequestLine().getUri();
		canonical.append('\n');
		if(uri.contains("?") && !uri.endsWith("?" + BucketHelper.URL_ARG_VERSIONING)) {
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
	@Override
	public final void configureStorage(final String[] storageNodeAddrs)
	throws IllegalStateException {
		if(container == null) {
			throw new IllegalStateException("Bucket is not specified");
		} else {
			LOG.debug(Markers.MSG, "Configure storage w/ bucket \"{}\"", container);
		}
		final String bucketName = container.getName();
		final WSBucketHelper<T, C> bucket = new WSBucketHelper<>(
			this, container.getName(), getVersioning()
		);
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
		super.configureStorage(storageNodeAddrs);
	}
	//
	@Override
	protected final void createDirectoryPath(final String nodeAddr, final String dirPath)
	throws IllegalStateException {
		final String bucketName = container.getName();
		final HttpEntityEnclosingRequest createDirReq = createGenericRequest(
			METHOD_PUT, "/" + bucketName + "/" + dirPath + "/"
		);
		applyHeadersFinally(createDirReq);
		try {
			final HttpResponse createDirResp = execute(
				nodeAddr, createDirReq,
				REQUEST_NO_PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS
			);
			if(createDirResp == null) {
				throw new NoHttpResponseException("No HTTP response available");
			}
			final StatusLine statusLine = createDirResp.getStatusLine();
			if(statusLine == null) {
				LOG.warn(
					Markers.ERR,
					"Failed to create the storage directory \"{}\" in the bucket \"{}\"",
					dirPath, bucketName
				);
			} else {
				final int statusCode = statusLine.getStatusCode();
				if(statusCode >= 200 && statusCode < 300) {
					LOG.info(
						Markers.MSG, "Using the storage directory \"{}\" in the bucket \"{}\"",
						dirPath, bucketName
					);
				} else {
					final HttpEntity httpEntity = createDirResp.getEntity();
					final StringBuilder msg = new StringBuilder("Create directory \"")
						.append(dirPath).append("\" failure: ")
						.append(statusLine.getReasonPhrase());
					if(httpEntity != null) {
						try(final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
							httpEntity.writeTo(buff);
							msg.append('\n').append(buff.toString());
						} catch(final Exception e) {
							// ignore
						}
					}
					throw new IllegalStateException(msg.toString());
				}
			}
		} catch(final Exception e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to create the storage directory \"" + dirPath +
					" in the bucket \"" + bucketName + "\""
			);
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final ItemSrc<T> getContainerListInput(final long maxCount, final String addr) {
		return new WSBucketItemSrc<>(
			new WSBucketHelper<>(this, container.getName(), getVersioning()),
			addr, getItemClass(), maxCount
		);
	}
}
