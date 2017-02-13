package com.emc.mongoose.storage.driver.net.http.atmos;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.io.AsyncCurrentDateInput;
import com.emc.mongoose.model.io.task.IoTask;

import static com.emc.mongoose.model.io.IoType.CREATE;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosApi.HEADERS_CANONICAL;
import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosApi.KEY_SUBTENANT_ID;
import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosApi.NS_URI_BASE;
import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosApi.OBJ_URI_BASE;
import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosApi.SIGN_METHOD;
import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosApi.SUBTENANT_URI_BASE;
import static com.emc.mongoose.storage.driver.net.http.base.EmcConstants.KEY_X_EMC_FILESYSTEM_ACCESS_ENABLED;
import static com.emc.mongoose.storage.driver.net.http.base.EmcConstants.KEY_X_EMC_NAMESPACE;
import static com.emc.mongoose.storage.driver.net.http.base.EmcConstants.KEY_X_EMC_SIGNATURE;
import static com.emc.mongoose.storage.driver.net.http.base.EmcConstants.KEY_X_EMC_UID;
import static com.emc.mongoose.storage.driver.net.http.base.EmcConstants.PREFIX_KEY_X_EMC;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.storage.driver.net.http.base.HttpStorageDriverBase;
import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 Created by kurila on 11.11.16.
 */
public final class AtmosStorageDriver<I extends Item, O extends IoTask<I, R>, R extends IoResult<I>>
extends HttpStorageDriverBase<I, O, R> {
	
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
		final boolean verifyFlag
	) throws UserShootHisFootException {
		super(jobName, loadConfig, storageConfig, verifyFlag);
		if(secret != null) {
			secretKey = new SecretKeySpec(
				Base64.getDecoder().decode(secret.getBytes(UTF_8)), SIGN_METHOD
			);
		} else {
			secretKey = null;
		}
		refreshUidHeader();
		if(namespace != null && !namespace.isEmpty()) {
			sharedHeaders.set(KEY_X_EMC_NAMESPACE, namespace);
		}
	}

	private void refreshUidHeader() {
		if(userName != null && !userName.isEmpty()) {
			if(authToken != null && !authToken.isEmpty()) {
				sharedHeaders.set(KEY_X_EMC_UID, authToken + "/" + userName);
			} else {
				sharedHeaders.set(KEY_X_EMC_UID, userName);
			}
		}
	}
	
	@Override
	public final boolean createPath(final String path)
	throws RemoteException {
		return true;
	}

	@Override
	public final List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException {
		return null;
	}

	@Override
	public final String getAuthToken()
	throws RemoteException {
		
		if(authToken == null || authToken.isEmpty()) {
			final String nodeAddr = storageNodeAddrs[0];
			final HttpHeaders reqHeaders = new DefaultHttpHeaders();
			reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
			reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			reqHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateInput.INSTANCE.get());
			//reqHeaders.set(KEY_X_EMC_DATE, reqHeaders.get(HttpHeaderNames.DATE));
			if(fsAccess) {
				reqHeaders.set(KEY_X_EMC_FILESYSTEM_ACCESS_ENABLED, Boolean.toString(fsAccess));
			}
			applyDynamicHeaders(reqHeaders);
			applySharedHeaders(reqHeaders);
			applyAuthHeaders(HttpMethod.PUT, SUBTENANT_URI_BASE, reqHeaders);
			
			final FullHttpRequest getSubtenantReq = new DefaultFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.PUT, SUBTENANT_URI_BASE, Unpooled.EMPTY_BUFFER,
				reqHeaders, EmptyHttpHeaders.INSTANCE
			);
			
			final FullHttpResponse getSubtenantResp;
			try {
				getSubtenantResp = executeHttpRequest(getSubtenantReq);
			} catch(final InterruptedException e) {
				return null;
			} catch(final ConnectException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to connect to the storage node");
				return null;
			}
			
			if(HttpStatusClass.SUCCESS.equals(getSubtenantResp.status().codeClass())) {
				final String subtenantId = getSubtenantResp.headers().get(KEY_SUBTENANT_ID);
				if(subtenantId != null && !subtenantId.isEmpty()) {
					LOG.info(Markers.MSG, "Using the subtenant id: \"{}\"", subtenantId);
					setAuthToken(subtenantId);
				} else {
					LOG.warn(Markers.ERR, "Creating the subtenant: got empty subtenantID");
				}
			} else {
				LOG.warn(Markers.ERR, "Creating the subtenant: got response {}",
					getSubtenantResp.status().toString()
				);
			}
			
			getSubtenantResp.release();
		}

		return authToken;
	}

	@Override
	public final void setAuthToken(final String authToken) {
		super.setAuthToken(authToken);
		refreshUidHeader();
	}

	@Override
	protected final void appendHandlers(final ChannelPipeline pipeline) {
		super.appendHandlers(pipeline);
		pipeline.addLast(new AtmosResponseHandler<>(this, verifyFlag, fsAccess));
	}

	@Override
	protected final HttpMethod getDataHttpMethod(final IoType ioType) {
		switch(ioType) {
			case CREATE:
			case NOOP:
				return HttpMethod.POST;
			case READ:
				return HttpMethod.GET;
			case UPDATE:
				return HttpMethod.PUT;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				throw new AssertionError("Unsupported I/O type: " + ioType);
		}
	}

	@Override
	protected final HttpMethod getTokenHttpMethod(final IoType ioType) {
		switch(ioType) {
			case CREATE:
				return HttpMethod.PUT;
			case READ:
				return HttpMethod.GET;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				throw new AssertionError("Not implemented yet");
		}
	}

	@Override
	protected final HttpMethod getPathHttpMethod(final IoType ioType) {
		throw new AssertionError("Not implemented yet");
	}

	@Override
	protected final String getDataUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	) {
		if(fsAccess) {
			return NS_URI_BASE + super.getDataUriPath(item, srcPath, dstPath, ioType);
		} else if(CREATE.equals(ioType)) {
			return OBJ_URI_BASE;
		} else {
			return OBJ_URI_BASE + super.getDataUriPath(item, srcPath, dstPath, ioType);
		}
	}

	@Override
	protected final String getTokenUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	) {
		if(CREATE.equals(ioType)) {
			return SUBTENANT_URI_BASE;
		} else {
			return SUBTENANT_URI_BASE + "/" + item.getName();
		}
	}

	@Override
	protected final String getPathUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	) {
		throw new AssertionError("Not implemented yet");
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
			httpHeaders.set(KEY_X_EMC_SIGNATURE, signature);
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
				throw new AssertionError("Failed to init MAC cypher instance", e);
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

	@Override
	public final String toString() {
		return String.format(super.toString(), "atmos");
	}
}
