package com.emc.mongoose.storage.driver.coop.netty.http.swift;

import static com.emc.mongoose.base.item.op.OpType.CREATE;
import static com.emc.mongoose.base.item.op.Operation.SLASH;
import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.AUTH_URI;
import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.DEFAULT_VERSIONS_LOCATION;
import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.KEY_X_AUTH_KEY;
import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.KEY_X_AUTH_TOKEN;
import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.KEY_X_AUTH_USER;
import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.KEY_X_COPY_FROM;
import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.KEY_X_OBJECT_MANIFEST;
import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.KEY_X_VERSIONS_LOCATION;
import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.MAX_LIST_LIMIT;
import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.URI_BASE;
import static com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftApi.parseContainerListing;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.emc.mongoose.base.config.IllegalArgumentNameException;
import com.emc.mongoose.base.data.DataInput;
import com.emc.mongoose.base.env.DateUtil;
import com.emc.mongoose.base.config.IllegalConfigurationException;
import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.ItemFactory;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.composite.data.CompositeDataOperation;
import com.emc.mongoose.base.item.op.partial.data.PartialDataOperation;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.storage.Credential;
import com.emc.mongoose.storage.driver.coop.netty.http.HttpStorageDriverBase;
import com.github.akurilov.confuse.Config;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http.HttpVersion;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import org.apache.logging.log4j.Level;

/** Created by andrey on 07.10.16. */
public class SwiftStorageDriver<I extends Item, O extends Operation<I>>
				extends HttpStorageDriverBase<I, O> {

	private static final String PART_NUM_MASK = "0000000";
	private static final ThreadLocal<StringBuilder> CONTAINER_LIST_QUERY = ThreadLocal.withInitial(StringBuilder::new);
	protected final boolean versioning;
	private final String namespacePath;

	public SwiftStorageDriver(
					final String stepId,
					final DataInput dataInput,
					final Config storageConfig,
					final boolean verifyFlag,
					final int batchSize)
					throws IllegalConfigurationException, InterruptedException {
		super(stepId, dataInput, storageConfig, verifyFlag, batchSize);
		final Config httpConfig = storageConfig.configVal("net-http");
		versioning = httpConfig.boolVal("versioning");
		if (namespace == null) {
			throw new IllegalArgumentNameException("Namespace is not set");
		}
		namespacePath = URI_BASE + SLASH + namespace;
	}

	@Override
	protected final String requestNewPath(final String path)  {
		// check the destination container if it exists w/ HEAD request
		final var nodeAddr = storageNodeAddrs[0];
		final var containerUri = namespacePath + (path.startsWith(SLASH) ? path : SLASH + path);
		final var uriQuery = uriQuery();
		final var reqUri = uriQuery == null || uriQuery.isEmpty() ? containerUri : containerUri + uriQuery;
		var reqHeaders = (HttpHeaders) new DefaultHttpHeaders();
		reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		applySharedHeaders(reqHeaders);
		applyDynamicHeaders(reqHeaders);
		final var credential = pathToCredMap.getOrDefault(path, this.credential);
		applyAuthHeaders(reqHeaders, HttpMethod.HEAD, reqUri, credential);
		final FullHttpRequest checkContainerReq = new DefaultFullHttpRequest(
						HttpVersion.HTTP_1_1,
						HttpMethod.HEAD,
						reqUri,
						Unpooled.EMPTY_BUFFER,
						reqHeaders,
						EmptyHttpHeaders.INSTANCE);
		FullHttpResponse checkContainerResp = null;
		try {
			checkContainerResp = executeHttpRequest(checkContainerReq);
		} catch (final InterruptedException e) {
			throwUnchecked(e);
		} catch (final ConnectException e) {
			LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
			return null;
		}
		final boolean containerExists, versioningEnabled;
		final var checkContainerRespStatus = checkContainerResp.status();
		if (HttpStatusClass.SUCCESS.equals(checkContainerRespStatus.codeClass())) {
			Loggers.MSG.info("Container \"{}\" already exists", path);
			containerExists = true;
			final var versionsLocation = checkContainerResp.headers().get(KEY_X_VERSIONS_LOCATION);
			versioningEnabled = versionsLocation != null && !versionsLocation.isEmpty();
		} else if (HttpResponseStatus.NOT_FOUND.equals(checkContainerRespStatus)) {
			containerExists = false;
			versioningEnabled = false;
		} else {
			Loggers.ERR.warn(
							"Unexpected container checking response: {}", checkContainerRespStatus.toString());
			checkContainerResp.release();
			return null;
		}
		checkContainerResp.release();
		// create or update the destination container if it doesn't exists
		if (!containerExists || (!versioningEnabled && versioning)) {
			reqHeaders = new DefaultHttpHeaders();
			reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
			reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			applySharedHeaders(reqHeaders);
			applyDynamicHeaders(reqHeaders);
			if (versioning) {
				reqHeaders.set(KEY_X_VERSIONS_LOCATION, DEFAULT_VERSIONS_LOCATION);
			}
			applyAuthHeaders(reqHeaders, HttpMethod.PUT, reqUri, credential);
			final FullHttpRequest putContainerReq = new DefaultFullHttpRequest(
							HttpVersion.HTTP_1_1,
							HttpMethod.PUT,
							reqUri,
							Unpooled.EMPTY_BUFFER,
							reqHeaders,
							EmptyHttpHeaders.INSTANCE);
			final FullHttpResponse putContainerResp;
			try {
				putContainerResp = executeHttpRequest(putContainerReq);
				try {
					final var putContainerRespStatus = putContainerResp.status();
					if (HttpStatusClass.SUCCESS.equals(putContainerRespStatus.codeClass())) {
						Loggers.MSG.info("Container \"{}\" created", path);
					} else {
						Loggers.ERR.warn(
										"Create/update container response: {}", putContainerRespStatus.toString());
						return null;
					}
				} finally {
					putContainerResp.release();
				}
			} catch (final InterruptedException e) {
				throwUnchecked(e);
			} catch (final ConnectException e) {
				LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
				return null;
			}
		}
		return path;
	}

	@Override
	protected final String requestNewAuthToken(final Credential credential)
					 {
		final var nodeAddr = storageNodeAddrs[0];
		final var reqHeaders = (HttpHeaders) new DefaultHttpHeaders();
		reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		reqHeaders.set(HttpHeaderNames.DATE, DateUtil.formatNowRfc1123());
		final var uid = credential == null ? this.credential.getUid() : credential.getUid();
		if (uid != null && !uid.isEmpty()) {
			reqHeaders.set(KEY_X_AUTH_USER, uid);
		}
		final var secret = credential == null ? this.credential.getSecret() : credential.getSecret();
		if (secret != null && !secret.isEmpty()) {
			reqHeaders.set(KEY_X_AUTH_KEY, secret);
		}
		reqHeaders.set(HttpHeaderNames.ACCEPT, "*/*");
		final FullHttpRequest getAuthTokenReq = new DefaultFullHttpRequest(
						HttpVersion.HTTP_1_1,
						HttpMethod.GET,
						AUTH_URI,
						Unpooled.EMPTY_BUFFER,
						reqHeaders,
						EmptyHttpHeaders.INSTANCE);
		FullHttpResponse getAuthTokenResp = null;
		try {
			getAuthTokenResp = executeHttpRequest(getAuthTokenReq);
		} catch (final InterruptedException e) {
			throwUnchecked(e);
		} catch (final ConnectException e) {
			LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
			return null;
		}
		final var authTokenValue = getAuthTokenResp.headers().get(KEY_X_AUTH_TOKEN);
		getAuthTokenResp.release();
		return authTokenValue;
	}

	@Override
	public final List<I> list(
					final ItemFactory<I> itemFactory,
					final String path,
					final String prefix,
					final int idRadix,
					final I lastPrevItem,
					final int count)
					throws IOException {
		final var countLimit = count < 1 || count > MAX_LIST_LIMIT ? MAX_LIST_LIMIT : count;
		final var nodeAddr = storageNodeAddrs[0];
		final var reqHeaders = (HttpHeaders) new DefaultHttpHeaders();
		reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		applyDynamicHeaders(reqHeaders);
		applySharedHeaders(reqHeaders);
		final var uriBuilder = CONTAINER_LIST_QUERY.get();
		uriBuilder.setLength(0);
		uriBuilder.append(namespacePath).append(path).append("?format=json");
		if (prefix != null && !prefix.isEmpty()) {
			uriBuilder.append("&prefix=").append(prefix);
		}
		if (lastPrevItem != null) {
			var lastItemName = lastPrevItem.name();
			if (lastItemName.contains("/")) {
				lastItemName = lastItemName.substring(lastItemName.lastIndexOf('/') + 1);
			}
			uriBuilder.append("&marker=").append(lastItemName);
		}
		uriBuilder.append("&limit=").append(countLimit);
		final var uri = uriBuilder.toString();
		authTokens.computeIfAbsent(credential, requestAuthTokenFunc);
		applyAuthHeaders(reqHeaders, HttpMethod.GET, uri, credential);
		final FullHttpRequest checkBucketReq = new DefaultFullHttpRequest(
						HttpVersion.HTTP_1_1,
						HttpMethod.GET,
						uri,
						Unpooled.EMPTY_BUFFER,
						reqHeaders,
						EmptyHttpHeaders.INSTANCE);
		final List<I> buff = new ArrayList<>(countLimit);
		try {
			final var listResp = executeHttpRequest(checkBucketReq);
			try {
				final var respStatus = listResp.status();
				if (HttpStatusClass.SUCCESS.equals(respStatus.codeClass())) {
					if (HttpResponseStatus.NO_CONTENT.equals(respStatus)) {
						throw new EOFException();
					} else {
						final var listRespContent = listResp.content();
						try (final InputStream contentStream = new ByteBufInputStream(listRespContent)) {
							parseContainerListing(buff, contentStream, path, itemFactory, idRadix);
						}
					}
				} else {
					Loggers.ERR.warn("Failed to get the container listing, response: \"{}\"", respStatus);
				}
			} finally {
				listResp.release();
			}
		} catch (final InterruptedException e) {
			throwUnchecked(e);
		} catch (final ConnectException e) {
			LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
		}
		return buff;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected final boolean submit(final O op) throws IllegalStateException {
		if (!isStarted()) {
			throw new IllegalStateException();
		}
		if (op instanceof CompositeDataOperation) {
			final var compositeOp = (CompositeDataOperation) op;
			if (compositeOp.allSubOperationsDone()) {
				return super.submit(op);
			} else {
				final List<O> subOps = compositeOp.subOperations();
				final var n = subOps.size();
				for (var i = 0; i < n; i += super.submit(subOps, i, n)) {
					LockSupport.parkNanos(1);
				}
				return true;
			}
		} else {
			return super.submit(op);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected final int submit(final List<O> ops, final int from, final int to)
					throws IllegalStateException {
		if (!isStarted()) {
			throw new IllegalStateException();
		}
		O nextOp;
		for (var i = from; i < to; i++) {
			nextOp = ops.get(i);
			if (nextOp instanceof CompositeDataOperation) {
				final var compositeOp = (CompositeDataOperation) nextOp;
				if (compositeOp.allSubOperationsDone()) {
					if (!super.submit(nextOp)) {
						return i - from;
					}
				} else {
					final var subOps = (List<O>) compositeOp.subOperations();
					final var n = subOps.size();
					if (n > 0) {
						// NOTE: blocking sub-ops submission
						while (!super.submit(subOps.get(0))) {
							LockSupport.parkNanos(1);
						}
						try {
							for (var j = 1; j < n; j++) {
								childOpQueue.put(subOps.get(j));
							}
						} catch (final InterruptedException e) {
							LogUtil.exception(
											Level.DEBUG,
											e,
											"{}: interrupted while enqueueing the child sub-operations",
											toString());
							throwUnchecked(e);
						}
					} else {
						throw new AssertionError("Composite load operation yields 0 sub-operations");
					}
				}
			} else {
				if (!super.submit(nextOp)) {
					return i - from;
				}
			}
		}
		return to - from;
	}

	@Override
	protected final HttpRequest httpRequest(final O op, final String nodeAddr)
					throws URISyntaxException {
		final HttpRequest httpRequest;
		final var opType = op.type();
		if (op instanceof CompositeDataOperation) {
			if (CREATE.equals(opType)) {
				final var compositeDataOp = (CompositeDataOperation) op;
				if (compositeDataOp.allSubOperationsDone()) {
					httpRequest = manifestCreateRequest(compositeDataOp, nodeAddr);
				} else { // this is the initial state of the task
					throw new AssertionError(
									"Initial request for the composite load operation is not allowed");
				}
			} else {
				throw new AssertionError("Non-create composite load operations are not implemented yet");
			}
		} else if (op instanceof PartialDataOperation) {
			if (CREATE.equals(opType)) {
				httpRequest = uploadPartRequest((PartialDataOperation) op, nodeAddr);
			} else {
				throw new AssertionError("Non-create composite operations are not implemented yet");
			}
		} else {
			httpRequest = super.httpRequest(op, nodeAddr);
		}
		return httpRequest;
	}

	@Override
	protected final HttpMethod tokenHttpMethod(final OpType opType) {
		switch (opType) {
		case NOOP:
		case CREATE:
			return HttpMethod.GET;
		default:
			throw new AssertionError("Not implemented yet");
		}
	}

	@Override
	protected final HttpMethod pathHttpMethod(final OpType opType) {
		switch (opType) {
		case NOOP:
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

	private HttpRequest manifestCreateRequest(
					final CompositeDataOperation compositeDataOp, final String nodeAddr) {
		final var item = (I) compositeDataOp.item();
		final var srcPath = compositeDataOp.srcPath();
		final var uriPath = dataUriPath(item, srcPath, compositeDataOp.dstPath(), CREATE);
		final var uriQuery = uriQuery();
		final var uri = uriQuery == null || uriQuery.isEmpty() ? uriPath : uriPath + uriQuery;
		final var httpHeaders = (HttpHeaders) new DefaultHttpHeaders();
		if (nodeAddr != null) {
			httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		}
		httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		final var objManifestPath = super.dataUriPath(item, srcPath, compositeDataOp.dstPath(), CREATE);
		httpHeaders.set(
						KEY_X_OBJECT_MANIFEST,
						(objManifestPath.startsWith("/") ? objManifestPath.substring(1) : objManifestPath) + "/");
		final var httpMethod = HttpMethod.PUT;
		final var httpRequest = (HttpRequest) new DefaultHttpRequest(HTTP_1_1, httpMethod, uri, httpHeaders);
		applyMetaDataHeaders(httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);
		applyAuthHeaders(httpHeaders, httpMethod, uri, compositeDataOp.credential());
		return httpRequest;
	}

	private HttpRequest uploadPartRequest(
					final PartialDataOperation partialDataOp, final String nodeAddr) {
		final var item = (I) partialDataOp.item();
		final var srcPath = partialDataOp.srcPath();
		final var partNumStr = Integer.toString(partialDataOp.partNumber() + 1);
		final var uriPath = dataUriPath(item, srcPath, partialDataOp.dstPath(), CREATE)
						+ "/"
						+ PART_NUM_MASK.substring(partNumStr.length())
						+ partNumStr;
		final var uriQuery = uriQuery();
		final var uri = uriQuery == null || uriQuery.isEmpty() ? uriPath : uriPath + uriQuery;
		final var httpHeaders = (HttpHeaders) new DefaultHttpHeaders();
		if (nodeAddr != null) {
			httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		}
		final var httpMethod = HttpMethod.PUT;
		final var httpRequest = (HttpRequest) new DefaultHttpRequest(HTTP_1_1, httpMethod, uri, httpHeaders);
		try {
			httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, ((DataItem) item).size());
		} catch (final IOException ignored) {}
		applyMetaDataHeaders(httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);
		applyAuthHeaders(httpHeaders, httpMethod, uri, partialDataOp.credential());
		return httpRequest;
	}

	@Override
	protected final void appendHandlers(final Channel channel) {
		super.appendHandlers(channel);
		channel.pipeline().addLast(new SwiftResponseHandler<>(this, verifyFlag));
	}

	@Override
	protected final String dataUriPath(
					final I item, final String srcPath, final String dstPath, final OpType opType) {
		return namespacePath + super.dataUriPath(item, srcPath, dstPath, opType);
	}

	@Override
	protected final String tokenUriPath(
					final I item, final String srcPath, final String dstPath, final OpType opType) {
		return AUTH_URI;
	}

	@Override
	protected final String pathUriPath(
					final I item, final String srcPath, final String dstPath, final OpType opType) {
		final var itemName = item.name();
		if (itemName.startsWith(SLASH)) {
			return namespacePath + itemName;
		} else {
			return namespacePath + SLASH + itemName;
		}
	}

	@Override
	protected final void applyMetaDataHeaders(final HttpHeaders httpHeaders) {}

	@Override
	protected final void applyAuthHeaders(
					final HttpHeaders httpHeaders,
					final HttpMethod httpMethod,
					final String dstUriPath,
					final Credential credential) {
		final String authToken;
		final String uid;
		final String secret;
		if (credential != null) {
			authToken = authTokens.get(credential);
			uid = credential.getUid();
			secret = credential.getSecret();
		} else if (this.credential != null) {
			authToken = authTokens.get(this.credential);
			uid = this.credential.getUid();
			secret = this.credential.getSecret();
		} else {
			authToken = authTokens.get(Credential.NONE);
			uid = null;
			secret = null;
		}
		if (dstUriPath.equals(AUTH_URI)) {
			if (uid != null && !uid.isEmpty()) {
				httpHeaders.set(KEY_X_AUTH_USER, uid);
			}
			if (secret != null && !secret.isEmpty()) {
				httpHeaders.set(KEY_X_AUTH_KEY, secret);
			}
		} else if (authToken != null && !authToken.isEmpty()) {
			httpHeaders.set(KEY_X_AUTH_TOKEN, authToken);
		}
	}

	@Override
	protected final void applyCopyHeaders(final HttpHeaders httpHeaders, final String srcPath)
					throws URISyntaxException {
		httpHeaders.set(
						KEY_X_COPY_FROM,
						srcPath != null && !srcPath.isEmpty() && srcPath.startsWith(namespacePath)
										? srcPath.substring(namespacePath.length())
										: srcPath);
	}

	@Override
	public final String toString() {
		return String.format(super.toString(), "swift");
	}
}
