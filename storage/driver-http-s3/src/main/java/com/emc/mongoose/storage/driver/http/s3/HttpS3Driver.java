package com.emc.mongoose.storage.driver.http.s3;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.storage.driver.http.base.HttpDriverBase;

import static com.emc.mongoose.storage.driver.http.s3.S3Constants.AUTH_PREFIX;
import static com.emc.mongoose.storage.driver.http.s3.S3Constants.HEADERS_CANONICAL;
import static com.emc.mongoose.storage.driver.http.s3.S3Constants.KEY_X_AMZ_COPY_SOURCE;
import static com.emc.mongoose.storage.driver.http.s3.S3Constants.PREFIX_KEY_AMZ;
import static com.emc.mongoose.storage.driver.http.s3.S3Constants.PREFIX_KEY_EMC;
import static com.emc.mongoose.storage.driver.http.s3.S3Constants.URL_ARG_VERSIONING;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;

import com.emc.mongoose.ui.log.Markers;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.util.AsciiString;
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
	
	private final static ThreadLocal<StringBuilder>
		BUFF_CANONICAL = new ThreadLocal<StringBuilder>() {
			@Override
			protected final StringBuilder initialValue() {
				return new StringBuilder();
			}
		};
		
	private final static ThreadLocal<Mac> THREAD_LOCAL_MAC = new ThreadLocal<>();
	
	private final static Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
	
	public HttpS3Driver(
		final String runId, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final String srcContainer, final SocketConfig socketConfig
	) throws UserShootHisFootException {
		super(runId, loadConfig, storageConfig, srcContainer, socketConfig);
	}
	
	@Override
	protected final HttpS3ClientHandler<I, O> getApiSpecificHandler() {
		return new HttpS3ClientHandler<>(monitorRef);
	}
	
	@Override
	protected void applyCopyHeaders(final HttpHeaders httpHeaders, final I obj)
	throws URISyntaxException {
		httpHeaders.set(KEY_X_AMZ_COPY_SOURCE, getSrcUriPath(obj));
	}
	
	@Override
	protected void applyAuthHeaders(
		final HttpMethod httpMethod, final String dstUriPath, final HttpHeaders httpHeaders
	) {
		final String signature = getSignature(getCanonical(httpMethod, dstUriPath, httpHeaders));
		if(signature != null) {
			httpHeaders.set(
				HttpHeaderNames.AUTHORIZATION, AUTH_PREFIX + userName + ":" + signature
			);
		}
	}
	
	private String getCanonical(
		final HttpMethod httpMethod, final String dstUriPath, final HttpHeaders httpHeaders
	) {
		final StringBuilder buffCanonical = BUFF_CANONICAL.get();
		buffCanonical.setLength(0); // reset/clear
		buffCanonical.append(httpMethod.name());
		
		for(final AsciiString headerName : HEADERS_CANONICAL) {
			if(httpHeaders.contains(headerName)) {
				for(final String headerValue: httpHeaders.getAll(headerName)) {
					buffCanonical.append('\n').append(headerValue);
				}
			} else if(sharedHeaders != null && sharedHeaders.contains(headerName)) {
				buffCanonical.append('\n').append(sharedHeaders.get(headerName));
			} else {
				buffCanonical.append('\n');
			}
		}
		// x-amz-*, x-emc-*
		String headerName;
		Map<String, String> sortedHeaders = new TreeMap<>();
		if(sharedHeaders != null) {
			for(final Map.Entry<String, String> header : sharedHeaders) {
				headerName = header.getKey().toLowerCase();
				if(headerName.startsWith(PREFIX_KEY_AMZ) || headerName.startsWith(PREFIX_KEY_EMC)) {
					sortedHeaders.put(headerName, header.getValue());
				}
			}
		}
		for(final Map.Entry<String, String> header : httpHeaders) {
			headerName = header.getKey().toLowerCase();
			if(headerName.startsWith(PREFIX_KEY_AMZ) || headerName.startsWith(PREFIX_KEY_EMC)) {
				sortedHeaders.put(headerName, header.getValue());
			}
		}
		for(final String k : sortedHeaders.keySet()) {
			buffCanonical.append('\n').append(k).append(':').append(sortedHeaders.get(k));
		}
		//
		buffCanonical.append('\n');
		if(dstUriPath.contains("?") && !dstUriPath.endsWith("?" + URL_ARG_VERSIONING)) {
			buffCanonical.append(dstUriPath.substring(0, dstUriPath.indexOf("?")));
		} else {
			buffCanonical.append(dstUriPath);
		}
		//
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Canonical representation:\n{}", buffCanonical);
		}
		//
		return buffCanonical.toString();
	}
	
	private String getSignature(final String canonicalForm) {
		//
		if(secretKey == null) {
			return null;
		}
		//
		final byte sigData[];
		Mac mac = THREAD_LOCAL_MAC.get();
		if(mac == null) {
			try {
				mac = Mac.getInstance(SIGN_METHOD);
				mac.init(secretKey);
			} catch(final NoSuchAlgorithmException | InvalidKeyException e) {
				throw new IllegalStateException("Failed to init MAC cypher instance");
			}
			THREAD_LOCAL_MAC.set(mac);
		}
		sigData = mac.doFinal(canonicalForm.getBytes());
		return BASE64_ENCODER.encodeToString(sigData);
	}
}
