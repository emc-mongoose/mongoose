package com.emc.mongoose.storage.adapter.swift;
// mongoose-common.jar

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.token.Token;
import com.emc.mongoose.core.impl.io.conf.HttpRequestConfigBase;
import com.emc.mongoose.core.impl.io.conf.IoConfigBase;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

// mongoose-core-api.jar
// mongoose-core-impl.jar
//
//
//
//
/**
 Created by kurila on 26.03.14.
 */
public final class HttpRequestConfigImpl<T extends HttpDataItem, C extends Container<T>>
extends HttpRequestConfigBase<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static String KEY_X_AUTH_TOKEN = "X-Auth-Token";
	public final static String KEY_X_AUTH_USER = "X-Auth-User";
	public final static String KEY_X_AUTH_KEY = "X-Auth-Key";
	public final static String KEY_X_COPY_FROM = "X-Copy-From";
	public final static String KEY_X_VERSIONING = "X-Versions-Location";
	public final static String DEFAULT_VERSIONS_CONTAINER = "archive";
	//
	private String uriSvcBasePath = "v1";
	private String dstContainerUriPath = null;
	private String srcContainerUriPath = null;
	//
	public HttpRequestConfigImpl()
	throws NoSuchAlgorithmException {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	public HttpRequestConfigImpl(final AppConfig appConfig) {
		super(appConfig);
		setAppConfig(appConfig);
	}
	//
	@SuppressWarnings("unchecked")
	protected HttpRequestConfigImpl(final HttpRequestConfigImpl<T, C> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
		//
		if(reqConf2Clone != null) {
			if(reqConf2Clone.uriSvcBasePath != null) {
				uriSvcBasePath = reqConf2Clone.uriSvcBasePath;
			}
			if(reqConf2Clone.dstContainerUriPath != null) {
				dstContainerUriPath = reqConf2Clone.dstContainerUriPath;
			}
			if(reqConf2Clone.srcContainerUriPath != null) {
				srcContainerUriPath = reqConf2Clone.srcContainerUriPath;
			}
		}
		//
		refreshContainerPaths();
	}
	//
	private void refreshContainerPaths() {
		if(uriSvcBasePath == null) {
			LOG.debug(
				Markers.MSG, "Swift API URI base path is <null>, not refreshing the container path"
			);
			return;
		}
		final String nameSpace = getNameSpace();
		if(nameSpace == null) {
			LOG.debug(Markers.MSG, "Swift namespace is <null>, not refreshing the container path");
			return;
		}
		if(dstContainer == null) {
			LOG.debug(
				Markers.MSG,
				"Swift destination container is <null>, not refreshing the container path"
			);
		} else {
			dstContainerUriPath = "/"+uriSvcBasePath +"/"+nameSpace+"/"+dstContainer.getName();
		}
		if(srcContainer == null) {
			LOG.debug(
				Markers.MSG,
				"Swift source container is <null>, not refreshing the container path"
			);
		} else {
			srcContainerUriPath = "/"+uriSvcBasePath+"/"+nameSpace+"/"+srcContainer.getName();
		}
	}
	//
	public final String getSvcBasePath() {
		return uriSvcBasePath;
	}
	//
	@Override
	public final HttpRequestConfigImpl<T, C> setAuthToken(final Token authToken) {
		super.setAuthToken(authToken);
		if(sharedHeaders == null) {
			sharedHeaders = new HashMap<>();
		}
		if(authToken != null) {
			sharedHeaders.put(
				KEY_X_AUTH_TOKEN, new BasicHeader(KEY_X_AUTH_TOKEN, authToken.getName())
			);
		}
		return this;
	}
	//
	@Override
	public final HttpRequestConfigImpl<T, C> setNameSpace(final String nameSpace) {
		super.setNameSpace(nameSpace);
		refreshContainerPaths();
		return this;
	}
	//
	//
	@Override
	public IoConfigBase<T, C> setDstContainer(final C container) {
		super.setDstContainer(container);
		refreshContainerPaths();
		return this;
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public HttpRequestConfigImpl<T, C> clone() {
		HttpRequestConfigImpl<T, C> copy = null;
		try {
			copy = new HttpRequestConfigImpl<>(this);
		} catch(final NoSuchAlgorithmException e) {
			LOG.fatal(Markers.ERR, "No such algorithm: \"{}\"", SIGN_METHOD);
		}
		return copy;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public HttpRequestConfigImpl<T, C> setAppConfig(final AppConfig appConfig) {
		super.setAppConfig(appConfig);
		refreshContainerPaths();
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		uriSvcBasePath = String.class.cast(in.readObject());
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(uriSvcBasePath);
	}
	//
	@Override
	protected final String getObjectDstPath(final T object)
	throws IllegalArgumentException, IllegalStateException {
		if(dstContainerUriPath == null) {
			throw new IllegalStateException("Illegal URI template: <null>");
		}
		if(object == null) {
			throw new IllegalArgumentException("Illegal data item: <null>");
		}
		return dstContainerUriPath + object.getPath() + object.getName();
	}
	//
	@Override
	protected final String getObjectSrcPath(final T object)
	throws IllegalArgumentException, IllegalStateException {
		if(srcContainerUriPath == null) {
			throw new IllegalStateException("Illegal URI template: <null>");
		}
		if(object == null) {
			throw new IllegalArgumentException("Illegal data item: <null>");
		}
		return srcContainerUriPath + object.getPath() + object.getName();
	}
	//
	@Override
	protected String getContainerPath(final Container<T> container)
	throws IllegalArgumentException, URISyntaxException {
		return "/" + uriSvcBasePath + "/" + nameSpace + "/" + container.getName();
	}
	//
	private Header headerAuthToken = null;
	//
	@Override
	protected final void applyCopyHeaders(final HttpRequest httpRequest, final T object)
	throws URISyntaxException {
		httpRequest.setHeader(KEY_X_COPY_FROM, getObjectSrcPath(object));
	}
	//
	@Override
	protected final void applyAuthHeader(final HttpRequest httpRequest) {
		final String authTokenValue = authToken == null ? null : authToken.toString();
		if(authTokenValue != null && authTokenValue.length() > 0) {
			if(!httpRequest.containsHeader(KEY_X_AUTH_TOKEN)) {
				if(headerAuthToken == null || !authTokenValue.equals(headerAuthToken.getValue())) {
					headerAuthToken = new BasicHeader(KEY_X_AUTH_TOKEN, authTokenValue);
				}
				httpRequest.setHeader(headerAuthToken);
			}
		}
	}
	//
	@Override
	public final String getCanonical(final HttpRequest httpRequest) {
		return null;
	}
	//
	@Override
	public final void configureStorage(final String storageNodeAddrs[])
	throws IllegalStateException {
		// configure an auth token - create if not specified
		String authTokenValue = authToken == null ? null : authToken.toString();
		if(authTokenValue == null || authTokenValue.length() < 1) {
			new SwiftAuthTokenHelper<>(this, null).create(storageNodeAddrs[0]);
		}
		//
		setAuthToken(authToken);
		// configure a container
		final HttpSwiftContainerHelper<T, C>
			containerHelper = new HttpSwiftContainerHelper<>(this, dstContainer);
		if(containerHelper.exists(storageNodeAddrs[0])) {
			LOG.info(Markers.MSG, "Container \"{}\" already exists", dstContainer);
		} else {
			containerHelper.create(storageNodeAddrs[0]);
			if(containerHelper.exists(storageNodeAddrs[0])) {
				appConfig.setProperty(AppConfig.KEY_ITEM_DST_CONTAINER, dstContainer.getName());
			} else {
				throw new IllegalStateException(
					String.format("Container \"%s\" still doesn't exist", dstContainer)
				);
			}
		}
		if(versioning) {
			containerHelper.setVersioning(storageNodeAddrs[0], true);
		}
		super.configureStorage(storageNodeAddrs);
	}
	//
	@Override
	protected final void createDirectoryPath(final String nodeAddr, final String dirPath)
	throws IllegalStateException {
		final String containerName = dstContainer.getName();
		/*final HttpEntityEnclosingRequest createDirReq = createGenericRequest(
			METHOD_PUT,
			"/" + uriSvcBasePath + "/" + containerName + "/" + nameSpace + "/" + dirPath
		);
		createDirReq.setHeader(HttpHeaders.CONTENT_TYPE, "application/directory");
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
					"Failed to create the storage directory \"{}\" in the container \"{}\"",
					dirPath, containerName
				);
			} else {
				final int statusCode = statusLine.getStatusCode();
				if(statusCode >= 200 && statusCode < 300) {*/
					LOG.info(
						Markers.MSG, "Using the storage directory \"{}\" in the container \"{}\"",
						dirPath, containerName
					);
				/*} else {
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
					" in the container \"" + containerName + "\""
			);
		}*/
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final Input<T> getContainerListInput(final long maxCount, final String addr) {
		if(srcContainer == null) {
			return null;
		} else {
			String path = srcContainer.getName();
			try {
				path = pathInput.get();
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to get the path");
			}
			return new WSContainerItemInput<>(
				path, new HttpSwiftContainerHelper<>(this, srcContainer), addr, getItemClass(),
				maxCount
			);
		}
	}
	//
}
