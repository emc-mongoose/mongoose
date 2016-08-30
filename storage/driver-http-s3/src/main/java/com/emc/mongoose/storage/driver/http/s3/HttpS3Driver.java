package com.emc.mongoose.storage.driver.http.s3;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.storage.driver.http.base.HttpDriverBase;

import static com.emc.mongoose.storage.driver.http.s3.S3Constants.AUTH_PREFIX;
import static com.emc.mongoose.storage.driver.http.s3.S3Constants.KEY_X_AMZ_COPY_SOURCE;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;

import com.emc.mongoose.ui.log.Markers;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 Created by kurila on 01.08.16.
 */
public final class HttpS3Driver<I extends Item, O extends IoTask<I>>
extends HttpDriverBase<I, O> {
	
	private final static Logger LOG = LogManager.getLogger();
	
	public HttpS3Driver(
		final String runId, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final SocketConfig socketConfig
	) throws UserShootHisFootException {
		super(runId, loadConfig, storageConfig, socketConfig);
	}
	
	@Override
	protected final SimpleChannelInboundHandler<HttpObject> getApiSpecificHandler() {
		return new HttpS3Handler();
	}
	
	@Override
	protected void applyCopyHeaders(final HttpHeaders httpHeaders, final I obj)
	throws URISyntaxException {
		httpHeaders.set(KEY_X_AMZ_COPY_SOURCE, getSrcUriPath(obj));
	}
	
	@Override
	protected void applyAuthHeaders(final HttpHeaders httpHeaders) {
		final String signature = getSignature(getCanonical(httpHeaders));
		if(signature != null) {
			httpHeaders.set(
				HttpHeaderNames.AUTHORIZATION, AUTH_PREFIX + userName + ":" + signature
			);
		}
	}
	
	private String getCanonical(final HttpHeaders httpHeaders) {
		//
		StringBuilder canonical = THR_LOC_CANONICAL_STR_BUILDER.get();
		if(canonical == null) {
			canonical = new StringBuilder();
			THR_LOC_CANONICAL_STR_BUILDER.set(canonical);
		} else {
			canonical.setLength(0); // reset/clear
		}
		canonical.append(httpHeaders.getRequestLine().getMethod());
		//
		for(final String headerName : HEADERS_CANONICAL) {
			if(httpHeaders.containsHeader(headerName)) {
				for(final Header header: httpHeaders.getHeaders(headerName)) {
					canonical.append('\n').append(header.getValue());
				}
			} else if(sharedHeaders != null && sharedHeaders.containsKey(headerName)) {
				canonical.append('\n').append(sharedHeaders.get(headerName).getValue());
			} else {
				canonical.append('\n');
			}
		}
		// x-amz-*, x-emc-*
		String headerName;
		Map<String, String> sortedHeaders = new TreeMap<>();
		if(sharedHeaders != null) {
			for(final String header : sharedHeaders) {
				headerName = header.getName().toLowerCase();
				if(headerName.startsWith(PREFIX_KEY_AMZ) || headerName.startsWith(PREFIX_KEY_EMC)) {
					sortedHeaders.put(headerName, header.getValue());
				}
			}
		}
		for(final Header header : httpHeaders.getAllHeaders()) {
			headerName = header.getName().toLowerCase();
			if(headerName.startsWith(PREFIX_KEY_AMZ) || headerName.startsWith(PREFIX_KEY_EMC)) {
				sortedHeaders.put(headerName, header.getValue());
			}
		}
		for(final String k : sortedHeaders.keySet()) {
			canonical.append('\n').append(k).append(':').append(sortedHeaders.get(k));
		}
		//
		final String uri = httpHeaders.getRequestLine().getUri();
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
	
	private String getSignature(final String canonicalForm) {
		//
		if(secretKey == null) {
			return null;
		}
		//
		final byte sigData[];
		Mac mac = THRLOC_MAC.get();
		if(mac == null) {
			try {
				mac = Mac.getInstance(SIGN_METHOD);
				mac.init(secretKey);
			} catch(final NoSuchAlgorithmException | InvalidKeyException e) {
				throw new IllegalStateException("Failed to init MAC cypher instance");
			}
			THRLOC_MAC.set(mac);
		}
		sigData = mac.doFinal(canonicalForm.getBytes());
		return Base64.encodeBase64String(sigData);
	}
}
