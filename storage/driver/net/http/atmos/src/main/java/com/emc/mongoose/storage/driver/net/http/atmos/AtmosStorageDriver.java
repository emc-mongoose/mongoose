package com.emc.mongoose.storage.driver.net.http.atmos;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;

import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosConstants.HEADERS_CANONICAL;
import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosConstants.NS_URI_BASE;
import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosConstants.OBJ_URI_BASE;
import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosConstants.SIGN_METHOD;
import com.emc.mongoose.model.load.LoadType;
import com.emc.mongoose.storage.driver.net.http.base.HttpStorageDriverBase;
import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.SocketConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.AsciiString;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

import static com.emc.mongoose.storage.driver.net.http.base.EmcConstants.KEY_X_EMC_NAMESPACE;
import static com.emc.mongoose.storage.driver.net.http.base.EmcConstants.KEY_X_EMC_SIGNATURE;
import static com.emc.mongoose.storage.driver.net.http.base.EmcConstants.KEY_X_EMC_UID;
import static com.emc.mongoose.storage.driver.net.http.base.EmcConstants.PREFIX_KEY_X_EMC;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 Created by kurila on 11.11.16.
 */
public final class AtmosStorageDriver<I extends Item, O extends IoTask<I>>
extends HttpStorageDriverBase<I, O> {
	
	private static final Logger LOG = LogManager.getLogger();
	
	private static final ThreadLocal<StringBuilder>
		BUFF_CANONICAL = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	
	private static final ThreadLocal<Mac> THREAD_LOCAL_MAC = new ThreadLocal<>();
	
	private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
	
	private final SecretKeySpec secretKey;
	
	public AtmosStorageDriver(
		final String jobName, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final boolean verifyFlag, final SocketConfig socketConfig
	) throws UserShootHisFootException {
		super(jobName, loadConfig, storageConfig, verifyFlag, socketConfig);
		
		SecretKeySpec tmpKey = null;
		if(secret != null) {
			try {
				tmpKey = new SecretKeySpec(secret.getBytes(UTF_8.name()), SIGN_METHOD);
			} catch(final UnsupportedEncodingException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failure");
			}
		}
		secretKey = tmpKey;
		
		if(userName != null && !userName.isEmpty()) {
			if(authToken != null && !authToken.isEmpty()) {
				sharedHeaders.set(KEY_X_EMC_UID, authToken + "/" + userName);
			} else {
				sharedHeaders.set(KEY_X_EMC_UID, userName);
			}
		}
		
		if(namespace != null && !namespace.isEmpty()) {
			sharedHeaders.set(KEY_X_EMC_NAMESPACE, namespace);
		}
	}
	
	@Override
	public final boolean configureStorage()
	throws RemoteException {
		// TODO create the subtenant, set the auth token field to the subtenant value
		return true;
	}
	
	@Override
	public final void channelCreated(final Channel channel)
	throws Exception {
		super.channelCreated(channel);
		final ChannelPipeline pipeline = channel.pipeline();
		pipeline.addLast(new AtmosClientHandler<>(this, verifyFlag));
	}

	@Override
	protected final HttpMethod getHttpMethod(final LoadType loadType) {
		switch(loadType) {
			case CREATE:
				return HttpMethod.POST;
			case READ:
				return HttpMethod.GET;
			case UPDATE:
				return HttpMethod.PUT;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				throw new IllegalStateException();
		}
	}

	@Override
	protected String getUriPath(
		final I item, final String srcPath, final String dstPath, final LoadType ioType
	) {
		if(fsAccess) {
			return NS_URI_BASE + super.getUriPath(item, srcPath, dstPath, ioType);
		} else if(LoadType.CREATE.equals(ioType)) {
			return OBJ_URI_BASE;
		} else {
			return OBJ_URI_BASE + super.getUriPath(item, srcPath, dstPath, ioType);
		}
	}
	
	@Override
	protected final void applyMetaDataHeaders(final HttpHeaders httpHeaders) {
	}
	
	@Override
	protected final void applyAuthHeaders(
		final HttpMethod httpMethod, final String dstUriPath, final HttpHeaders httpHeaders
	) {
		final String signature;
		if(secretKey == null) {
			signature = null;
		} else {
			signature = getSignature(getCanonical(httpMethod, dstUriPath, httpHeaders), secretKey);
		}
		if(signature != null) {
			httpHeaders.set(
				KEY_X_EMC_SIGNATURE, signature
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

		buffCanonical.append('\n').append(dstUriPath);

		// x-emc-*
		String headerName;
		Map<String, String> sortedHeaders = new TreeMap<>();
		if(sharedHeaders != null) {
			for(final Map.Entry<String, String> header : sharedHeaders) {
				headerName = header.getKey().toLowerCase();
				if(headerName.startsWith(PREFIX_KEY_X_EMC)) {
					sortedHeaders.put(headerName, header.getValue());
				}
			}
		}
		for(final Map.Entry<String, String> header : httpHeaders) {
			headerName = header.getKey().toLowerCase();
			if(headerName.startsWith(PREFIX_KEY_X_EMC)) {
				sortedHeaders.put(headerName, header.getValue());
			}
		}
		for(final String k : sortedHeaders.keySet()) {
			buffCanonical.append('\n').append(k).append(':').append(sortedHeaders.get(k));
		}

		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Canonical representation:\n{}", buffCanonical);
		}

		return buffCanonical.toString();
	}

	private String getSignature(final String canonicalForm, final SecretKeySpec secretKey) {

		if(secretKey == null) {
			return null;
		}

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
	
	@Override
	protected final void applyCopyHeaders(final HttpHeaders httpHeaders, final String srcPath)
	throws URISyntaxException {
	}
}
