package com.emc.mongoose.object.api.impl.provider.swift;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.object.api.MutableWSRequest;
import com.emc.mongoose.object.api.impl.WSRequestConfigBase;
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import org.apache.http.Header;
import org.apache.http.HttpResponse;
//
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.NoSuchAlgorithmException;
//
/**
 Created by kurila on 26.03.14.
 */
public final class WSRequestConfigImpl<T extends WSObject>
extends WSRequestConfigBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static String
		//
		KEY_CONF_AUTH_TOKEN = "api.swift.auth.token",
		KEY_CONF_CONTAINER = "api.swift.container",
		KEY_CONF_SVC_BASEPATH = "api.swift.service.basepath",
		//
		KEY_X_AUTH_TOKEN = "X-Auth-Token",
		KEY_X_AUTH_USER = "X-Auth-User",
		KEY_X_AUTH_KEY = "X-Auth-Key",
		//
		FMT_URI_CONTAINER_PATH = "/%s/%s/%s",
		FMT_URI_OBJECT_PATH = "%s/%s";
	//
	private String nameSpace = null, uriSvcBasePath = null, uriSvcBaseContainerPath = null;
	private WSAuthTokenImpl<T> authToken = null;
	private WSContainerImpl<T> container = null;
	//
	public WSRequestConfigImpl()
	throws NoSuchAlgorithmException {
		this(null);
	}
	//
	protected WSRequestConfigImpl(final WSRequestConfigImpl<T> reqConf2Clone)
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
		}
		//
		final RunTimeConfig localConfig = RunTimeConfig.getContext();
		if(uriSvcBasePath == null) {
			uriSvcBasePath = localConfig.getString(KEY_CONF_SVC_BASEPATH);
		}
		if(authToken == null) {
			setAuthToken(new WSAuthTokenImpl<>(this, localConfig.getString(KEY_CONF_AUTH_TOKEN)));
		}
		if(container == null) {
			setContainer(new WSContainerImpl<>(this, localConfig.getString(KEY_CONF_CONTAINER)));
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
			LOG.debug(Markers.MSG, "Swift access id is <null>, not refreshing the container path");
			return;
		}
		if(container == null) {
			LOG.debug(Markers.MSG, "Swift container is <null>, not refreshing the container path");
			return;
		}
		uriSvcBaseContainerPath = String
			.format(FMT_URI_CONTAINER_PATH, uriSvcBasePath, nameSpace, container.getName());
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
	public final WSRequestConfigImpl<T> setAuthToken(final WSAuthTokenImpl<T> authToken)
	throws IllegalArgumentException {
		if(authToken == null) {
			throw new IllegalArgumentException("Setting <null> auth token is illegal");
		}
		this.authToken = authToken;
		return this;
	}
	//
	public final WSContainerImpl<T> getContainer() {
		return container;
	}
	//
	public final WSRequestConfigImpl<T> setContainer(final WSContainerImpl<T> container)
	throws IllegalArgumentException, IllegalStateException {
		if(container == null) {
			throw new IllegalArgumentException("Setting <null> container is illegal");
		}
		this.container = container;
		refreshContainerPath();
		return this;
	}
	//
	@Override
	public final String getNameSpace() {
		return nameSpace;
	}
	//
	@Override
	public final WSRequestConfigImpl<T> setNameSpace(final String nameSpace) {
		this.nameSpace = nameSpace;
		refreshContainerPath();
		return this;
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public WSRequestConfigImpl<T> clone() {
		WSRequestConfigImpl<T> copy = null;
		try {
			copy = new WSRequestConfigImpl<>(this);
		} catch(final NoSuchAlgorithmException e) {
			LOG.fatal(Markers.ERR, "No such algorithm: \"{}\"", signMethod);
		}
		return copy;
	}
	//
	@Override
	public WSRequestConfigImpl<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		if(runTimeConfig.containsKey(KEY_CONF_SVC_BASEPATH)) {
			uriSvcBasePath = runTimeConfig.getString(KEY_CONF_SVC_BASEPATH);
		} else {
			LOG.error(Markers.ERR, "Swift base uri path is not specified");
		}
		//
		if(runTimeConfig.containsKey(KEY_CONF_AUTH_TOKEN)) {
			authToken = new WSAuthTokenImpl<>(this, runTimeConfig.getString(KEY_CONF_AUTH_TOKEN));
		} else {
			LOG.error(Markers.ERR, "Swift auth token is not specified");
		}
		//
		if(runTimeConfig.containsKey(KEY_CONF_CONTAINER)) {
			container = new WSContainerImpl<>(this, runTimeConfig.getString(KEY_CONF_CONTAINER));
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
		final String containerName = String.class.cast(in.readObject());
		LOG.debug(Markers.MSG, "The container name to use: \"{}\"", containerName);
		setContainer(new WSContainerImpl<T>(this, containerName));
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(uriSvcBasePath);
		if(container != null) {
			out.writeObject(container.getName());
		} else {
			out.writeObject(null);
		}
	}
	//
	@Override
	protected final void applyURI(final MutableWSRequest httpRequest, final WSObject dataItem)
	throws IllegalArgumentException {
		if(uriSvcBaseContainerPath == null) {
			LOG.warn(Markers.ERR, "Illegal URI template: <null>");
		}
		if(dataItem == null) {
			throw new IllegalArgumentException("Illegal data item: <null>");
		}
		httpRequest.setUriPath(
			String.format(
				FMT_URI_OBJECT_PATH, uriSvcBaseContainerPath, dataItem.getId()
			)
		);
	}
	//
	private Header headerAuthToken = null;
	//
	@Override
	protected final void applyAuthHeader(final MutableWSRequest httpRequest) {
		final String authTokenValue = authToken == null ? null : authToken.getValue();
		if(authTokenValue != null) {
			if(!httpRequest.containsHeader(KEY_X_AUTH_TOKEN)) {
				if(headerAuthToken == null || headerAuthToken.getValue() != authTokenValue) {
					headerAuthToken = new BasicHeader(KEY_X_AUTH_TOKEN, authTokenValue);
				}
				httpRequest.setHeader(headerAuthToken);
			}
		}
	}
	//
	@Override
	public final String getCanonical(final MutableWSRequest httpRequest) {
		// TODO swift specific things
		return null;
	}
	//
	@Override
	public final void configureStorage(final LoadExecutor<T> client)
	throws IllegalStateException {
		// configure an auth token
		if(authToken == null) {
			throw new IllegalStateException("No auth token specified");
		}
		authToken.create(client);
		final String authTokenValue = authToken.getValue();
		if(authTokenValue == null) {
			throw new IllegalStateException("No auth token was created");
		}
		sharedHeadersMap.put(KEY_X_AUTH_TOKEN, authTokenValue);
		runTimeConfig.set(KEY_CONF_AUTH_TOKEN, authTokenValue);
		// configure a container
		if(container == null) {
			throw new IllegalStateException("Bucket is not specified");
		}
		final String containerName = container.getName();
		if(container.exists(client)) {
			LOG.debug(Markers.MSG, "Bucket \"{}\" already exists", containerName);
		} else {
			container.create(client);
			if(container.exists(client)) {
				runTimeConfig.set(KEY_CONF_CONTAINER, containerName);
			} else {
				throw new IllegalStateException(
					String.format("Container \"%s\" still doesn't exist", containerName)
				);
			}
		}

	}
	//
	@Override
	public final Producer<T> getAnyDataProducer(
		final long maxCount, final LoadExecutor<T> loadExecutor
	) {
		return null; // TODO swift specific things
	}
	//
}
