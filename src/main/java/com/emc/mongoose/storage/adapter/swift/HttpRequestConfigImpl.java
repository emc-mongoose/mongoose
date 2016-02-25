package com.emc.mongoose.storage.adapter.swift;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.io.conf.HttpRequestConfigBase;
//
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
//
/**
 Created by kurila on 26.03.14.
 */
public final class HttpRequestConfigImpl<T extends HttpDataItem, C extends Container<T>>
extends HttpRequestConfigBase<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	public final static String KEY_X_AUTH_TOKEN = "X-Auth-Token";
	public final static String KEY_X_AUTH_USER = "X-Auth-User";
	public final static String KEY_X_AUTH_KEY = "X-Auth-Key";
	public final static String KEY_X_VERSIONING = "X-Versions-Location";
	public final static String DEFAULT_VERSIONS_CONTAINER = "archive";
	//
	private String uriSvcBasePath = "v1", uriSvcBaseContainerPath = null;
	private WSAuthTokenImpl<T> authToken = null;
	//
	public HttpRequestConfigImpl()
	throws NoSuchAlgorithmException {
		this(null);
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
			if(reqConf2Clone.uriSvcBaseContainerPath != null) {
				uriSvcBaseContainerPath = reqConf2Clone.uriSvcBaseContainerPath;
			}
			setAuthToken(reqConf2Clone.getAuthToken());
			setContainer(reqConf2Clone.getContainer());
			//
			if(authToken == null) {
				setAuthToken(new WSAuthTokenImpl<>(this, reqConf2Clone.appConfig.getAuthToken()));
			}
			if(container == null) {
				setContainer(
					(C)new BasicContainer<T>(reqConf2Clone.appConfig.getItemContainerName()));
			}
		}
		//
		refreshContainerPath();
	}
	//
	private void refreshContainerPath() {
		if(uriSvcBasePath == null) {
			LOG.debug(Markers.MSG, "Swift API URI base path is <null>, not refreshing the container path");
			return;
		}
		final String nameSpace = getNameSpace();
		if(nameSpace == null) {
			LOG.debug(Markers.MSG, "Swift namespace is <null>, not refreshing the container path");
			return;
		}
		if(container == null) {
			LOG.debug(Markers.MSG, "Swift container is <null>, not refreshing the container path");
			return;
		}
		uriSvcBaseContainerPath = "/"+uriSvcBasePath+"/"+nameSpace+"/"+container.getName();
	}
	//
	public final String getSvcBasePath() {
		return uriSvcBasePath;
	}
	//
	public final WSAuthTokenImpl<T> getAuthToken() {
		return authToken;
	}
	//
	public final HttpRequestConfigImpl<T, C> setAuthToken(final WSAuthTokenImpl<T> authToken)
	throws IllegalArgumentException {
		if(authToken == null) {
			throw new IllegalArgumentException("Setting <null> auth token is illegal");
		}
		this.authToken = authToken;
		return this;
	}
	//
	@Override
	public final HttpRequestConfigImpl<T, C> setNameSpace(final String nameSpace) {
		super.setNameSpace(nameSpace);
		refreshContainerPath();
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
		super.setAppConfig(this.appConfig);
		//
		if(this.appConfig.containsKey(AppConfig.KEY_AUTH_TOKEN)) {
			authToken = new WSAuthTokenImpl<>(this, this.appConfig.getAuthToken());
		} else {
			LOG.error(Markers.ERR, "Swift auth token is not specified");
		}
		//
		if(this.appConfig.containsKey(AppConfig.KEY_ITEM_CONTAINER_NAME)) {
			setContainer(
				(C) new BasicContainer<T>(
					this.appConfig.getItemContainerName()
				)
			);
		} else {
			LOG.error(Markers.ERR, "Swift container is not specified");
		}
		//
		refreshContainerPath();
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		uriSvcBasePath = String.class.cast(in.readObject());
		Object t = in.readObject();
		if(t != null) {
			setAuthToken(new WSAuthTokenImpl<>(this, String.class.cast(t)));
		} else {
			LOG.debug(Markers.MSG, "Note: no auth token has been got from load client side");
		}
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(uriSvcBasePath);
		out.writeObject(authToken == null ? null : authToken.getValue());
	}
	//
	@Override
	protected final String getDataUriPath(final T dataItem)
	throws IllegalArgumentException {
		if(uriSvcBaseContainerPath == null) {
			LOG.warn(Markers.ERR, "Illegal URI template: <null>");
		}
		if(dataItem == null) {
			throw new IllegalArgumentException("Illegal data item: <null>");
		}
		//applyObjectId(dataItem, null);
		return uriSvcBaseContainerPath + getFilePathFor(dataItem);
	}
	@Override
	protected String getContainerUriPath(final Container<T> container)
	throws IllegalArgumentException, URISyntaxException {
		return "/" + uriSvcBasePath + "/" + nameSpace + "/" + container.getName();
	}
	//
	private Header headerAuthToken = null;
	//
	@Override
	protected final void applyAuthHeader(final HttpRequest httpRequest) {
		final String authTokenValue = authToken == null ? null : authToken.getValue();
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
		String authTokenValue;
		if(authToken == null) {
			throw new IllegalStateException("No auth token specified");
		} else {
			authTokenValue = authToken.getValue();
			if(authTokenValue == null || authTokenValue.length() < 1) {
				authToken.create(storageNodeAddrs[0]);
				authTokenValue = authToken.getValue();
			}
		}
		if(authTokenValue == null) {
			throw new IllegalStateException("No auth token was created");
		}
		sharedHeaders.updateHeader(new BasicHeader(KEY_X_AUTH_TOKEN, authTokenValue));
		appConfig.setProperty(AppConfig.KEY_AUTH_TOKEN, authTokenValue);
		// configure a container
		if(container == null) {
			throw new IllegalStateException("Container is not specified");
		}
		final HttpSwiftContainerHelper<T, C>
			containerHelper = new HttpSwiftContainerHelper<>(this, container);
		if(containerHelper.exists(storageNodeAddrs[0])) {
			LOG.info(Markers.MSG, "Container \"{}\" already exists", container);
		} else {
			containerHelper.create(storageNodeAddrs[0]);
			if(containerHelper.exists(storageNodeAddrs[0])) {
				appConfig.setProperty(AppConfig.KEY_ITEM_CONTAINER_NAME, container.getName());
			} else {
				throw new IllegalStateException(
					String.format("Container \"%s\" still doesn't exist", container)
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
		final String containerName = container.getName();
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
	public final ItemSrc<T> getContainerListInput(final long maxCount, final String addr) {
		return new WSContainerItemSrc<>(
			new HttpSwiftContainerHelper<>(this, container), addr, getItemClass(), maxCount
		);
	}
	//
}
