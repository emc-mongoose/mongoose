package com.emc.mongoose.storage.driver.net.http.swift;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.supply.async.AsyncCurrentDateSupplier;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.IoType.CREATE;
import com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.model.io.task.partial.data.PartialDataIoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.storage.Credential;
import com.emc.mongoose.storage.driver.net.http.base.HttpStorageDriverBase;
import static com.emc.mongoose.model.io.task.IoTask.SLASH;
import static com.emc.mongoose.storage.driver.net.http.base.EmcConstants.KEY_X_EMC_FILESYSTEM_ACCESS_ENABLED;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftApi.AUTH_URI;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftApi.DEFAULT_VERSIONS_LOCATION;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftApi.KEY_X_AUTH_KEY;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftApi.KEY_X_AUTH_TOKEN;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftApi.KEY_X_AUTH_USER;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftApi.KEY_X_COPY_FROM;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftApi.KEY_X_OBJECT_MANIFEST;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftApi.KEY_X_VERSIONS_LOCATION;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftApi.MAX_LIST_LIMIT;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftApi.URI_BASE;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftApi.parseContainerListing;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.config.IllegalArgumentNameException;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPipeline;
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

import org.apache.logging.log4j.Level;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 Created by andrey on 07.10.16.
 */
public class SwiftStorageDriver<I extends Item, O extends IoTask<I>>
extends HttpStorageDriverBase<I, O> {

	private static final String PART_NUM_MASK = "0000000";
	private static final ThreadLocal<StringBuilder>
		CONTAINER_LIST_QUERY = new ThreadLocal<StringBuilder>() {
			@Override
			protected final StringBuilder initialValue() {
				return new StringBuilder();
			}
		};

	private final String namespacePath;

	public SwiftStorageDriver(
		final String jobName, final ContentSource contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws UserShootHisFootException {
		super(jobName, contentSrc, loadConfig, storageConfig, verifyFlag);
		if(namespace == null) {
			throw new IllegalArgumentNameException("Namespace is not set");
		}
		namespacePath = URI_BASE + SLASH + namespace;
	}
	
	@Override
	protected final String requestNewPath(final String path) {

		// check the destination container if it exists w/ HEAD request
		final String nodeAddr = storageNodeAddrs[0];
		final HttpHeaders reqHeaders = new DefaultHttpHeaders();
		reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		reqHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateSupplier.INSTANCE.get());
		applySharedHeaders(reqHeaders);
		final String containerUri = namespacePath + path;
		applyAuthHeaders(reqHeaders, HttpMethod.HEAD, containerUri, credential);
		final FullHttpRequest checkContainerReq = new DefaultFullHttpRequest(
			HttpVersion.HTTP_1_1, HttpMethod.HEAD, containerUri, Unpooled.EMPTY_BUFFER, reqHeaders,
			EmptyHttpHeaders.INSTANCE
		);
		final FullHttpResponse checkContainerResp;
		try {
			checkContainerResp = executeHttpRequest(checkContainerReq);
		} catch(final InterruptedException e) {
			return null;
		} catch(final ConnectException e) {
			LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
			return null;
		}

		final boolean containerExists, versioningEnabled;
		final HttpResponseStatus checkContainerRespStatus = checkContainerResp.status();
		if(HttpResponseStatus.NOT_FOUND.equals(checkContainerRespStatus)) {
			containerExists = false;
			versioningEnabled = false;
		} else if(HttpStatusClass.SUCCESS.equals(checkContainerRespStatus.codeClass())) {
			Loggers.MSG.info("Container \"{}\" already exists", path);
			containerExists = true;
			final String versionsLocation = checkContainerResp
				.headers()
				.get(KEY_X_VERSIONS_LOCATION);
			if(versionsLocation == null || versionsLocation.isEmpty()) {
				versioningEnabled = false;
			} else {
				versioningEnabled = true;
			}
		} else {
			Loggers.ERR.warn(
				"Unexpected container checking response: {}", checkContainerRespStatus.toString()
			);
			checkContainerResp.release();
			return null;
		}
		checkContainerResp.release();

		// create or update the destination container if it doesn't exists
		if(
			!containerExists || (versioningEnabled && !versioning) ||
			(!versioningEnabled && versioning)
		) {
			if(fsAccess) {
				reqHeaders.set(KEY_X_EMC_FILESYSTEM_ACCESS_ENABLED, Boolean.toString(true));
			}
			if(versioning) {
				reqHeaders.set(KEY_X_VERSIONS_LOCATION, DEFAULT_VERSIONS_LOCATION);
			}
			applyAuthHeaders(reqHeaders, HttpMethod.PUT, containerUri, credential);
			final FullHttpRequest putContainerReq = new DefaultFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.PUT, containerUri, Unpooled.EMPTY_BUFFER,
				reqHeaders, EmptyHttpHeaders.INSTANCE
			);
			final FullHttpResponse putContainerResp;
			try {
				putContainerResp = executeHttpRequest(putContainerReq);
			} catch(final InterruptedException e) {
				return null;
			} catch(final ConnectException e) {
				LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
				return null;
			}

			final HttpResponseStatus putContainerRespStatus = putContainerResp.status();
			if(HttpStatusClass.SUCCESS.equals(putContainerRespStatus.codeClass())) {
				Loggers.MSG.info("Container \"{}\" created", path);
			} else {
				Loggers.ERR.warn(
					"Create/update container response: {}", putContainerRespStatus.toString()
				);
				putContainerResp.release();
				return null;
			}
			putContainerResp.release();
		}

		return path;
	}
	
	@Override
	protected final String requestNewAuthToken(final Credential credential) {

		final String nodeAddr = storageNodeAddrs[0];
		final HttpHeaders reqHeaders = new DefaultHttpHeaders();
		reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		reqHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateSupplier.INSTANCE.get());
		
		final String uid = credential == null ? this.credential.getUid() : credential.getUid();
		if(uid != null && ! uid.isEmpty()) {
			reqHeaders.set(KEY_X_AUTH_USER, uid);
		}
		final String secret = credential == null ?
			this.credential.getSecret() : credential.getSecret();
		if(secret != null && !secret.isEmpty()) {
			reqHeaders.set(KEY_X_AUTH_KEY, secret);
		}
		reqHeaders.set(HttpHeaderNames.ACCEPT, "*/*");
		final FullHttpRequest getAuthTokenReq = new DefaultFullHttpRequest(
			HttpVersion.HTTP_1_1, HttpMethod.GET, AUTH_URI, Unpooled.EMPTY_BUFFER, reqHeaders,
			EmptyHttpHeaders.INSTANCE
		);

		final FullHttpResponse getAuthTokenResp;
		try {
			getAuthTokenResp = executeHttpRequest(getAuthTokenReq);
		} catch(final InterruptedException e) {
			return null;
		} catch(final ConnectException e) {
			LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
			return null;
		}

		final String authTokenValue = getAuthTokenResp.headers().get(KEY_X_AUTH_TOKEN);
		getAuthTokenResp.release();
		
		return authTokenValue;
	}
	
	@Override
	public final List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException {

		final int countLimit = count < 1 || count > MAX_LIST_LIMIT ? MAX_LIST_LIMIT : count;
		final String nodeAddr = storageNodeAddrs[0];
		final HttpHeaders reqHeaders = new DefaultHttpHeaders();

		reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		reqHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateSupplier.INSTANCE.get());

		applyDynamicHeaders(reqHeaders);
		applySharedHeaders(reqHeaders);

		final StringBuilder queryBuilder = CONTAINER_LIST_QUERY.get();
		queryBuilder.setLength(0);
		queryBuilder.append(namespacePath).append(path).append("?format=json");
		if(prefix != null && !prefix.isEmpty()) {
			queryBuilder.append("&prefix=").append(prefix);
		}
		if(lastPrevItem != null) {
			queryBuilder.append("&marker=").append(lastPrevItem.getName());
		}
		queryBuilder.append("&limit=").append(countLimit);
		final String query = queryBuilder.toString();

		applyAuthHeaders(reqHeaders, HttpMethod.GET, query, credential);

		final FullHttpRequest checkBucketReq = new DefaultFullHttpRequest(
			HttpVersion.HTTP_1_1, HttpMethod.GET, query, Unpooled.EMPTY_BUFFER, reqHeaders,
			EmptyHttpHeaders.INSTANCE
		);
		final List<I> buff = new ArrayList<>(countLimit);
		FullHttpResponse listResp = null;
		try {
			listResp = executeHttpRequest(checkBucketReq);
			final HttpResponseStatus respStatus = listResp.status();
			if(HttpStatusClass.SUCCESS.equals(respStatus.codeClass())) {
				if(HttpResponseStatus.NO_CONTENT.equals(respStatus)) {
					throw new EOFException();
				} else {
					final ByteBuf listRespContent = listResp.content();
					try(final InputStream contentStream = new ByteBufInputStream(listRespContent)) {
						parseContainerListing(buff, contentStream, path, itemFactory, idRadix);
					} finally {
						listRespContent.release();
					}
				}
			} else {
				Loggers.ERR.warn("Failed to get the container listing, response: \"{}\"", respStatus);
			}
		} catch(final InterruptedException ignored) {
		} catch(final ConnectException e) {
			LogUtil.exception(Level.WARN, e, "Failed to connect to the storage node");
		}

		return buff;
	}

	@Override @SuppressWarnings("unchecked")
	protected final boolean submit(final O ioTask)
	throws InterruptedException {
		if(isClosed() || isInterrupted()) {
			throw new InterruptedException();
		}
		ioTask.reset();
		if(ioTask instanceof CompositeDataIoTask) {
			final CompositeDataIoTask compositeTask = (CompositeDataIoTask) ioTask;
			if(compositeTask.allSubTasksDone()) {
				return super.submit(ioTask);
			} else {
				final List<O> subTasks = compositeTask.getSubTasks();
				final int n = subTasks.size();
				for(int i = 0; i < n; i += super.submit(subTasks, i, n)) {
					LockSupport.parkNanos(1);
				}
				return true;
			}
		} else {
			return super.submit(ioTask);
		}
	}
	
	@Override @SuppressWarnings("unchecked")
	protected final int submit(final List<O> ioTasks, final int from, final int to)
	throws InterruptedException {
		if(isClosed() || isInterrupted()) {
			throw new InterruptedException();
		}
		O nextIoTask;
		for(int i = from; i < to; i ++) {
			nextIoTask = ioTasks.get(i);
			nextIoTask.reset();
			if(nextIoTask instanceof CompositeDataIoTask) {
				final CompositeDataIoTask compositeTask = (CompositeDataIoTask) nextIoTask;
				if(compositeTask.allSubTasksDone()) {
					if(!super.submit(nextIoTask)) {
						return i - from;
					}
				} else {
					final List<O> subTasks = compositeTask.getSubTasks();
					final int n = subTasks.size();
					if(n > 0) {
						while(!super.submit(subTasks.get(0))) {
							LockSupport.parkNanos(1);
						}
						for(int j = 1; j < n; j ++) {
							childTasksQueue.put(subTasks.get(j));
						}
					} else {
						throw new AssertionError("Composite I/O task yields 0 sub-tasks");
					}
				}
			} else {
				if(!super.submit(nextIoTask)) {
					return i - from;
				}
			}
		}
		return to - from;
	}
	
	@Override
	protected final HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
	throws URISyntaxException {
		
		final HttpRequest httpRequest;
		final IoType ioType = ioTask.getIoType();
		if(ioTask instanceof CompositeDataIoTask) {
			if(CREATE.equals(ioType)) {
				final CompositeDataIoTask mpuTask = (CompositeDataIoTask) ioTask;
				if(mpuTask.allSubTasksDone()) {
					httpRequest = getManifestCreateRequest(mpuTask, nodeAddr);
				} else { // this is the initial state of the task
					throw new AssertionError(
						"Initial request for the composite I/O task is not allowed"
					);
				}
			} else {
				throw new AssertionError(
					"Non-create multipart operations are not implemented yet"
				);
			}
		} else if(ioTask instanceof PartialDataIoTask) {
			if(CREATE.equals(ioType)) {
				httpRequest = getUploadPartRequest((PartialDataIoTask) ioTask, nodeAddr);
			} else {
				throw new AssertionError(
					"Non-create multipart operations are not implemented yet"
				);
			}
		} else {
			httpRequest = super.getHttpRequest(ioTask, nodeAddr);
		}
		return httpRequest;
	}

	@Override
	protected final HttpMethod getTokenHttpMethod(final IoType ioType) {
		switch(ioType) {
			case NOOP:
			case CREATE:
				return HttpMethod.GET;
			default:
				throw new AssertionError("Not implemented yet");
		}
	}

	@Override
	protected final HttpMethod getPathHttpMethod(final IoType ioType) {
		switch(ioType) {
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

	private HttpRequest getManifestCreateRequest(
		final CompositeDataIoTask mpuTask, final String nodeAddr
	) {
		final I item = (I) mpuTask.getItem();
		final String srcPath = mpuTask.getSrcPath();
		final String uriPath = getDataUriPath(item, srcPath, mpuTask.getDstPath(), CREATE);
		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		if(nodeAddr != null) {
			httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		}
		httpHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateSupplier.INSTANCE.get());
		httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		final String objManifestPath = super.getDataUriPath(
			item, srcPath, mpuTask.getDstPath(), CREATE
		);
		httpHeaders.set(
			KEY_X_OBJECT_MANIFEST,
			(objManifestPath.startsWith("/") ? objManifestPath.substring(1) : objManifestPath) + "/"
		);
		final HttpMethod httpMethod = HttpMethod.PUT;
		final HttpRequest httpRequest = new DefaultHttpRequest(
			HTTP_1_1, httpMethod, uriPath, httpHeaders
		);
		applyMetaDataHeaders(httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);
		applyAuthHeaders(httpHeaders, httpMethod, uriPath, mpuTask.getCredential());
		return httpRequest;
	}
	
	private HttpRequest getUploadPartRequest(
		final PartialDataIoTask ioTask, final String nodeAddr
	) {
		final I item = (I) ioTask.getItem();
		final String srcPath = ioTask.getSrcPath();
		final String partNumStr = Integer.toString(ioTask.getPartNumber() + 1);
		final String uriPath = getDataUriPath(item, srcPath, ioTask.getDstPath(), CREATE) +
			"/" + PART_NUM_MASK.substring(partNumStr.length()) + partNumStr;
		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		if(nodeAddr != null) {
			httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		}
		httpHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateSupplier.INSTANCE.get());
		final HttpMethod httpMethod = HttpMethod.PUT;
		final HttpRequest httpRequest = new DefaultHttpRequest(
			HTTP_1_1, httpMethod, uriPath, httpHeaders
		);
		try {
			httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, ((DataItem) item).size());
		} catch(final IOException ignored) {
		}
		applyMetaDataHeaders(httpHeaders);
		applyDynamicHeaders(httpHeaders);
		applySharedHeaders(httpHeaders);
		applyAuthHeaders(httpHeaders, httpMethod, uriPath, ioTask.getCredential());
		return httpRequest;
	}

	@Override
	protected final void appendHandlers(final ChannelPipeline pipeline) {
		super.appendHandlers(pipeline);
		pipeline.addLast(new SwiftResponseHandler<>(this, verifyFlag));
	}

	@Override
	protected final String getDataUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	) {
		return namespacePath + super.getDataUriPath(item, srcPath, dstPath, ioType);
	}

	@Override
	protected final String getTokenUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	) {
		return AUTH_URI;
	}

	@Override
	protected final String getPathUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	) {
		final String itemName = item.getName();
		if(itemName.startsWith(SLASH)) {
			return namespacePath + itemName;
		} else {
			return namespacePath + SLASH + itemName;
		}
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
			authToken = authTokens.get(Credential.NONE);
			uid = null;
			secret = null;
		}
		
		if(dstUriPath.equals(AUTH_URI)) {
			if(uid != null && !uid.isEmpty()) {
				httpHeaders.set(KEY_X_AUTH_USER, uid);
			}
			if(secret != null && !secret.isEmpty()) {
				httpHeaders.set(KEY_X_AUTH_KEY, secret);
			}
		} else if(authToken != null && !authToken.isEmpty()) {
			httpHeaders.set(KEY_X_AUTH_TOKEN, authToken);
		}
	}

	@Override
	public final void applyCopyHeaders(final HttpHeaders httpHeaders, final String srcPath)
	throws URISyntaxException {
		httpHeaders.set(KEY_X_COPY_FROM, srcPath);
	}
	
	@Override
	public final String toString() {
		return String.format(super.toString(), "swift");
	}
}
