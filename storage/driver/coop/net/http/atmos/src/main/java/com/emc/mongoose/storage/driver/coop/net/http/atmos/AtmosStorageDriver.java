package com.emc.mongoose.storage.driver.coop.net.http.atmos;

import static com.emc.mongoose.item.op.OpType.CREATE;
import static com.emc.mongoose.storage.driver.coop.net.http.atmos.AtmosApi.HEADERS_CANONICAL;
import static com.emc.mongoose.storage.driver.coop.net.http.atmos.AtmosApi.KEY_SUBTENANT_ID;
import static com.emc.mongoose.storage.driver.coop.net.http.atmos.AtmosApi.NS_URI_BASE;
import static com.emc.mongoose.storage.driver.coop.net.http.atmos.AtmosApi.OBJ_URI_BASE;
import static com.emc.mongoose.storage.driver.coop.net.http.atmos.AtmosApi.SIGN_METHOD;
import static com.emc.mongoose.storage.driver.coop.net.http.atmos.AtmosApi.SUBTENANT_URI_BASE;
import static com.emc.mongoose.storage.driver.coop.net.http.EmcConstants.KEY_X_EMC_FILESYSTEM_ACCESS_ENABLED;
import static com.emc.mongoose.storage.driver.coop.net.http.EmcConstants.KEY_X_EMC_NAMESPACE;
import static com.emc.mongoose.storage.driver.coop.net.http.EmcConstants.KEY_X_EMC_SIGNATURE;
import static com.emc.mongoose.storage.driver.coop.net.http.EmcConstants.KEY_X_EMC_UID;
import static com.emc.mongoose.storage.driver.coop.net.http.EmcConstants.PREFIX_KEY_X_EMC;
import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.ItemFactory;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.storage.Credential;
import com.emc.mongoose.storage.driver.coop.net.http.HttpStorageDriverBase;

import com.github.akurilov.confuse.Config;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 Created by kurila on 11.11.16.
 */
public class AtmosStorageDriver<I extends Item, O extends Operation<I>>
extends HttpStorageDriverBase<I, O> {
	
	private static final ThreadLocal<StringBuilder> BUFF_CANONICAL = ThreadLocal.withInitial(StringBuilder::new);
	
	private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
	private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
	
	private static final ThreadLocal<Map<String, Mac>> MAC_BY_SECRET = ThreadLocal.withInitial(HashMap::new);
	private static final Function<String, Mac> GET_MAC_BY_SECRET = secret -> {
		try {
			final SecretKeySpec secretKey = new SecretKeySpec(
				BASE64_DECODER.decode(secret.getBytes(UTF_8)), SIGN_METHOD
			);
			final Mac mac = Mac.getInstance(SIGN_METHOD);
			mac.init(secretKey);
			return mac;
		} catch(final NoSuchAlgorithmException | InvalidKeyException e) {
			LogUtil.exception(Level.ERROR, e, "Failed to init MAC for the given secret key");
		} catch(final IllegalArgumentException e) {
			LogUtil.exception(Level.ERROR, e, "Failed to perform the secret key Base-64 decoding");
		}
		return null;
	};
	
	public AtmosStorageDriver(
		final String stepId, final DataInput dataInput, final Config storageConfig, final boolean verifyFlag,
		final int batchSize
	) throws OmgShootMyFootException, InterruptedException {
		super(stepId, dataInput, storageConfig, verifyFlag, batchSize);
		if(namespace != null && !namespace.isEmpty()) {
			sharedHeaders.set(KEY_X_EMC_NAMESPACE, namespace);
		}
		requestNewPathFunc = null; // do not use
	}

	@Override
	protected final String requestNewPath(final String path) {
		throw new AssertionError("Should not be invoked");
	}
	
	@Override
	protected final String requestNewAuthToken(final Credential credential)
	throws InterruptRunException {
		
		final String nodeAddr = storageNodeAddrs[0];
		final HttpHeaders reqHeaders = new DefaultHttpHeaders();
		reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		reqHeaders.set(HttpHeaderNames.DATE, DATE_SUPPLIER.get());
		//reqHeaders.set(KEY_X_EMC_DATE, reqHeaders.get(HttpHeaderNames.DATE));
		if(fsAccess) {
			reqHeaders.set(KEY_X_EMC_FILESYSTEM_ACCESS_ENABLED, Boolean.toString(fsAccess));
		}
		applyDynamicHeaders(reqHeaders);
		applySharedHeaders(reqHeaders);
		applyAuthHeaders(reqHeaders, HttpMethod.PUT, SUBTENANT_URI_BASE, credential);
		
		final FullHttpRequest getSubtenantReq = new DefaultFullHttpRequest(
			HttpVersion.HTTP_1_1, HttpMethod.PUT, SUBTENANT_URI_BASE, Unpooled.EMPTY_BUFFER, reqHeaders,
			EmptyHttpHeaders.INSTANCE
		);
		
		final FullHttpResponse getSubtenantResp;
		try {
			getSubtenantResp = executeHttpRequest(getSubtenantReq);
		} catch(final InterruptedException e) {
			throw new InterruptRunException(e);
		} catch(final ConnectException e) {
			LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
			return null;
		}
		
		final String subtenantId;
		if(HttpStatusClass.SUCCESS.equals(getSubtenantResp.status().codeClass())) {
			subtenantId = getSubtenantResp.headers().get(KEY_SUBTENANT_ID);
		} else {
			Loggers.ERR.warn("Creating the subtenant: got response {}", getSubtenantResp.status().toString());
			return null;
		}
		getSubtenantResp.release();
		
		return subtenantId;
	}
	
	@Override
	public final List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException {
		return null;
	}
	
	@Override
	protected final void appendHandlers(final Channel channel) {
		super.appendHandlers(channel);
		channel.pipeline().addLast(new AtmosResponseHandler<>(this, verifyFlag, fsAccess));
	}

	@Override
	protected final HttpMethod dataHttpMethod(final OpType opType) {
		switch(opType) {
			case NOOP:
				return HttpMethod.HEAD;
			case CREATE:
				return HttpMethod.POST;
			case READ:
				return HttpMethod.GET;
			case UPDATE:
				return HttpMethod.PUT;
			case DELETE:
				return HttpMethod.DELETE;
			default:
				throw new AssertionError("Unsupported I/O type: " + opType);
		}
	}

	@Override
	protected final HttpMethod tokenHttpMethod(final OpType opType) {
		switch(opType) {
			case NOOP:
				return HttpMethod.HEAD;
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
	protected final HttpMethod pathHttpMethod(final OpType opType) {
		throw new AssertionError("Not implemented yet");
	}

	@Override
	protected final String dataUriPath(
		final I item, final String srcPath, final String dstPath, final OpType opType
	) {
		if(fsAccess) {
			return NS_URI_BASE + super.dataUriPath(item, srcPath, dstPath, opType);
		} else if(CREATE.equals(opType)) {
			return OBJ_URI_BASE;
		} else {
			return OBJ_URI_BASE + super.dataUriPath(item, srcPath, dstPath, opType);
		}
	}

	@Override
	protected final String tokenUriPath(
		final I item, final String srcPath, final String dstPath, final OpType opType
	) {
		if(CREATE.equals(opType)) {
			return SUBTENANT_URI_BASE;
		} else {
			return SUBTENANT_URI_BASE + '/' + item.name();
		}
	}

	@Override
	protected final String pathUriPath(
		final I item, final String srcPath, final String dstPath, final OpType opType
	) {
		throw new AssertionError("Not implemented yet");
	}
	
	@Override
	protected final void applyMetaDataHeaders(final HttpHeaders httpHeaders) {
	}
	
	@Override
	protected final void applyAuthHeaders(
		final HttpHeaders httpHeaders, final HttpMethod httpMethod, final String dstUriPath,
		final Credential credential
	) {
		final String authToken;
		final String uid;
		final String secret;
		if(credential != null) {
			authToken = authTokens.get(credential);
			uid = credential.getUid();
			secret = credential.getSecret();
		} else if(this.credential != null) {
			authToken = authTokens.get(this.credential);
			uid = this.credential.getUid();
			secret = this.credential.getSecret();
		} else {
			authToken = null;
			uid = null;
			secret = null;
		}
		
		if(uid != null && !uid.isEmpty()) {
			if(authToken != null && !authToken.isEmpty() && !dstUriPath.equals(SUBTENANT_URI_BASE)) {
				httpHeaders.set(KEY_X_EMC_UID, authToken + '/' + uid);
			} else {
				httpHeaders.set(KEY_X_EMC_UID, uid);
			}
		}
		
		if(secret != null && !secret.isEmpty()) {
			final Mac mac = MAC_BY_SECRET.get().computeIfAbsent(secret, GET_MAC_BY_SECRET);
			final String canonicalForm = getCanonical(httpHeaders, httpMethod, dstUriPath);
			final byte sigData[] = mac.doFinal(canonicalForm.getBytes());
			httpHeaders.set(KEY_X_EMC_SIGNATURE, BASE64_ENCODER.encodeToString(sigData));
		}
	}

	protected String getCanonical(
		final HttpHeaders httpHeaders, final HttpMethod httpMethod, final String dstUriPath
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
				if(headerName.startsWith(PREFIX_KEY_X_EMC) && !headerName.equals(KEY_X_EMC_SIGNATURE)) {
					sortedHeaders.put(headerName, header.getValue());
				}
			}
		}
		for(final Map.Entry<String, String> header : httpHeaders) {
			headerName = header.getKey().toLowerCase();
			if(headerName.startsWith(PREFIX_KEY_X_EMC) && !headerName.equals(KEY_X_EMC_SIGNATURE)) {
				sortedHeaders.put(headerName, header.getValue());
			}
		}
		for(final Map.Entry<String, String> sortedHeader : sortedHeaders.entrySet()) {
			buffCanonical
				.append('\n').append(sortedHeader.getKey())
				.append(':').append(sortedHeader.getValue());
		}

		if(Loggers.MSG.isTraceEnabled()) {
			Loggers.MSG.trace("Canonical representation:\n{}", buffCanonical);
		}

		return buffCanonical.toString();
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
