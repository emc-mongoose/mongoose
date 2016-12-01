package com.emc.mongoose.storage.driver.net.http.swift;

import com.emc.mongoose.common.io.AsyncCurrentDateInput;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;

import com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.model.io.task.partial.data.PartialDataIoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.storage.driver.net.http.base.HttpStorageDriverBase;
import static com.emc.mongoose.model.io.task.IoTask.SLASH;
import static com.emc.mongoose.storage.driver.net.http.base.EmcConstants.KEY_X_EMC_FILESYSTEM_ACCESS_ENABLED;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftConstants.AUTH_URI;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftConstants.DEFAULT_VERSIONS_LOCATION;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftConstants.KEY_X_AUTH_KEY;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftConstants.KEY_X_AUTH_TOKEN;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftConstants.KEY_X_AUTH_USER;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftConstants.KEY_X_COPY_FROM;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftConstants.KEY_X_OBJECT_MANIFEST;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftConstants.KEY_X_VERSIONS_LOCATION;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftConstants.URI_BASE;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.log.Markers;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 Created by andrey on 07.10.16.
 */
public class SwiftStorageDriver<I extends Item, O extends IoTask<I, R>, R extends IoResult>
extends HttpStorageDriverBase<I, O, R> {

	private static final Logger LOG = LogManager.getLogger();
	private static final String PART_NUM_MASK = "0000000";

	private final String namespacePath;

	public SwiftStorageDriver(
		final String jobName, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final boolean verifyFlag, final SocketConfig socketConfig
	) throws IllegalStateException {
		super(jobName, loadConfig, storageConfig, verifyFlag, socketConfig);
		if(authToken != null && !authToken.isEmpty()) {
			setAuthToken(authToken);
		}
		namespacePath = URI_BASE + SLASH + namespace;
	}
	
	@Override
	public final boolean createPath(final String path)
	throws RemoteException {

		// check the destination container if it exists w/ HEAD request
		final String nodeAddr = storageNodeAddrs[0];
		final HttpHeaders reqHeaders = new DefaultHttpHeaders();
		reqHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		reqHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		reqHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateInput.INSTANCE.get());
		applySharedHeaders(reqHeaders);
		final String containerUri = namespacePath + path;
		final FullHttpRequest checkContainerReq = new DefaultFullHttpRequest(
			HttpVersion.HTTP_1_1, HttpMethod.HEAD, containerUri, Unpooled.EMPTY_BUFFER, reqHeaders,
			EmptyHttpHeaders.INSTANCE
		);
		final FullHttpResponse checkContainerResp;
		try {
			checkContainerResp = executeHttpRequest(checkContainerReq);
		} catch(final InterruptedException e) {
			return false;
		}

		final boolean containerExists, versioningEnabled;
		final HttpResponseStatus checkContainerRespStatus = checkContainerResp.status();
		if(HttpResponseStatus.NOT_FOUND.equals(checkContainerRespStatus)) {
			containerExists = false;
			versioningEnabled = false;
		} else if(HttpStatusClass.SUCCESS.equals(checkContainerRespStatus.codeClass())) {
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
			LOG.warn(
				Markers.ERR, "Unexpected container checking response: {}",
				checkContainerRespStatus.toString()
			);
			checkContainerResp.release();
			return false;
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
			final FullHttpRequest putContainerReq = new DefaultFullHttpRequest(
				HttpVersion.HTTP_1_1, HttpMethod.PUT, containerUri, Unpooled.EMPTY_BUFFER,
				reqHeaders, EmptyHttpHeaders.INSTANCE
			);
			final FullHttpResponse putContainerResp;
			try {
				putContainerResp = executeHttpRequest(putContainerReq);
			} catch(final InterruptedException e) {
				return false;
			}
			final HttpResponseStatus putContainerRespStatus = putContainerResp.status();
			if(!HttpStatusClass.SUCCESS.equals(putContainerRespStatus.codeClass())) {
				LOG.warn(
					Markers.ERR, "Create/update container response: {}",
					putContainerRespStatus.toString()
				);
				putContainerResp.release();
				return false;
			}
			putContainerResp.release();
		}

		return true;
	}

	@Override
	public final Input<I> getPathListingInput(
		final String path, final ItemFactory<I> itemFactory, final int idRadix,
		final String idPrefix
	) throws RemoteException {
		return null;
	}
	
	@Override @SuppressWarnings("unchecked")
	protected final void submit(final O task)
	throws InterruptedIOException {
		if(task instanceof CompositeDataIoTask) {
			final CompositeDataIoTask compositeTask = (CompositeDataIoTask) task;
			if(compositeTask.allSubTasksDone()) {
				super.submit(task);
			} else {
				final List<O> subTasks = compositeTask.getSubTasks();
				final int n = subTasks.size();
				for(int i = 0; i < n; i += super.submit(subTasks, i, n)) {
					LockSupport.parkNanos(1);
				}
			}
		} else {
			super.submit(task);
		}
	}
	
	@Override @SuppressWarnings("unchecked")
	protected final int submit(final List<O> tasks, final int from, final int to)
	throws InterruptedIOException {
		O nextIoTask;
		for(int i = from; i < to; i ++) {
			nextIoTask = tasks.get(i);
			if(nextIoTask instanceof CompositeDataIoTask) {
				final CompositeDataIoTask compositeTask = (CompositeDataIoTask) nextIoTask;
				if(compositeTask.allSubTasksDone()) {
					super.submit(nextIoTask);
				} else {
					final List<O> subTasks = compositeTask.getSubTasks();
					final int n = subTasks.size();
					for(int j = 0; j < n; j += super.submit(subTasks, j, n)) {
						LockSupport.parkNanos(1);
					}
				}
			} else {
				super.submit(nextIoTask);
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
			if(IoType.CREATE.equals(ioType)) {
				final CompositeDataIoTask mpuTask = (CompositeDataIoTask) ioTask;
				if(mpuTask.allSubTasksDone()) {
					httpRequest = getManifestCreateRequest(mpuTask, nodeAddr);
				} else { // this is the initial state of the task
					throw new IllegalStateException(
						"Initial request for the composite I/O task is not allowed"
					);
				}
			} else {
				throw new IllegalStateException(
					"Non-create multipart operations are not implemented yet"
				);
			}
		} else if(ioTask instanceof PartialDataIoTask) {
			if(IoType.CREATE.equals(ioType)) {
				httpRequest = getUploadPartRequest((PartialDataIoTask) ioTask, nodeAddr);
			} else {
				throw new IllegalStateException(
					"Non-create multipart operations are not implemented yet"
				);
			}
		} else {
			httpRequest = super.getHttpRequest(ioTask, nodeAddr);
		}
		return httpRequest;
	}
	
	private HttpRequest getManifestCreateRequest(
		final CompositeDataIoTask mpuTask, final String nodeAddr
	) {
		final I item = (I) mpuTask.getItem();
		final String srcPath = mpuTask.getSrcPath();
		final String uriPath = getUriPath(item, srcPath, mpuTask.getDstPath(), IoType.CREATE);
		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		httpHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateInput.INSTANCE.get());
		httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		final String objManifestPath = super.getUriPath(
			item, srcPath, mpuTask.getDstPath(), IoType.CREATE
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
		applyAuthHeaders(httpMethod, uriPath, httpHeaders);
		return httpRequest;
	}
	
	private HttpRequest getUploadPartRequest(
		final PartialDataIoTask ioTask, final String nodeAddr
	) {
		final I item = (I) ioTask.getItem();
		final String srcPath = ioTask.getSrcPath();
		final String partNumStr = Integer.toString(ioTask.getPartNumber() + 1);
		final String uriPath = getUriPath(item, srcPath, ioTask.getDstPath(), IoType.CREATE) +
			"/" + PART_NUM_MASK.substring(partNumStr.length()) + partNumStr;
		final HttpHeaders httpHeaders = new DefaultHttpHeaders();
		httpHeaders.set(HttpHeaderNames.HOST, nodeAddr);
		httpHeaders.set(HttpHeaderNames.DATE, AsyncCurrentDateInput.INSTANCE.get());
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
		applyAuthHeaders(httpMethod, uriPath, httpHeaders);
		return httpRequest;
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
			if(userName != null && !userName.isEmpty()) {
				reqHeaders.set(KEY_X_AUTH_USER, userName);
			}
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
			}
			authToken = getAuthTokenResp.headers().get(KEY_X_AUTH_TOKEN);
			if(authToken == null || authToken.isEmpty()) {
				LOG.warn(
					Markers.ERR, "Failed to get the auth token, response is: {}",
					getAuthTokenResp.status().toString()
				);
			} else {
				setAuthToken(authToken);
			}
		}
		return authToken;
	}

	@Override
	public final void setAuthToken(final String authToken) {
		this.authToken = authToken;
		sharedHeaders.set(KEY_X_AUTH_TOKEN, authToken);
	}

	@Override
	protected final void appendSpecificHandlers(final ChannelPipeline pipeline) {
		super.appendSpecificHandlers(pipeline);
		pipeline.addLast(new SwiftResponseHandler<>(this, verifyFlag));
	}

	@Override
	protected final String getUriPath(
		final I item, final String srcPath, final String dstPath, final IoType ioType
	) {
		return namespacePath + super.getUriPath(item, srcPath, dstPath, ioType);
	}
	
	@Override
	protected final void applyMetaDataHeaders(final HttpHeaders httpHeaders) {
	}

	@Override
	protected final void applyAuthHeaders(
		final HttpMethod httpMethod, final String dstUriPath, final HttpHeaders httpHeaders
	) {
		if(authToken != null && !authToken.isEmpty()) {
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
