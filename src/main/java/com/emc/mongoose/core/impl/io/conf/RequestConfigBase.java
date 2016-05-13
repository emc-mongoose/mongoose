package com.emc.mongoose.core.impl.io.conf;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.io.conf.RequestConfig;
// mongoose-core-impl.jar
import com.emc.mongoose.core.api.item.token.Token;
import com.emc.mongoose.core.impl.item.token.BasicToken;
import org.apache.commons.lang.StringUtils;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
/**
 Created by kurila on 06.06.14.
 The most common implementation of the shared request configuration.
 */
public abstract class RequestConfigBase<T extends DataItem, C extends Container<T>>
extends IoConfigBase<T, C>
implements RequestConfig<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//protected final static String FMT_URI_ADDR = "%s://%s:%s";
	//
	protected String api, secret, userName;
	protected volatile int port;
	protected Token authToken;
	protected boolean sslFlag;
	//
	@SuppressWarnings("unchecked")
	protected RequestConfigBase() {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	protected RequestConfigBase(final AppConfig appConfig) {
		super(appConfig);
	}
	//
	protected RequestConfigBase(final RequestConfigBase<T, C> reqConf2Clone) {
		super(reqConf2Clone);
		if(reqConf2Clone != null) {
			setAPI(reqConf2Clone.getAPI());
			setUserName(reqConf2Clone.getUserName());
			setPort(reqConf2Clone.getPort());
			setAuthToken(reqConf2Clone.getAuthToken());
			secret = reqConf2Clone.getSecret();
			setSslFlag(reqConf2Clone.getSslFlag());
			LOG.debug(
				Markers.MSG, "Forked conf conf #{} from #{}", hashCode(), reqConf2Clone.hashCode()
			);
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public RequestConfigBase<T, C> clone()
	throws CloneNotSupportedException {
		final RequestConfigBase<T, C> requestConfigBranch = (RequestConfigBase<T, C>) super.clone();
		requestConfigBranch
			.setAPI(api)
			.setUserName(userName)
			.setPort(port)
			.setAuthToken(authToken)
			.setSslFlag(sslFlag);
		requestConfigBranch.secret = secret;
		LOG.debug(
			Markers.MSG, "Forked conf conf #{} from #{}", requestConfigBranch.hashCode(), hashCode()
		);
		return requestConfigBranch;
	}
	//
	@Override
	public final String getAPI() {
		return api;
	}
	@Override
	public RequestConfigBase<T, C> setAPI(final String api) {
		this.api = api;
		return this;
	}
	//
	@Override
	public final boolean getSslFlag() {
		return sslFlag;
	}
	@Override
	public final RequestConfigBase<T, C> setSslFlag(final boolean sslFlag) {
		this.sslFlag = sslFlag;
		return this;
	}
	//
	@Override
	public final int getPort() {
		return port;
	}
	@Override
	public final RequestConfigBase<T, C> setPort(final int port)
	throws IllegalArgumentException {
		LOG.trace(Markers.MSG, "Using storage port: {}", port);
		if(port > 0 || port < 0x10000) {
			this.port = port;
		} else {
			throw new IllegalArgumentException("Port number value should be > 0");
		}
		return this;
	}
	//
	@Override
	public final String getUserName() {
		return userName;
	}
	@Override
	public RequestConfigBase<T, C> setUserName(final String userName) {
		this.userName = userName;
		return this;
	}
	//
	@Override
	public final String getSecret() {
		return secret;
	}
	@Override
	public RequestConfigBase<T, C> setSecret(final String secret) {
		this.secret = secret;
		return this;
	}
	//
	@Override
	public Token getAuthToken() {
		return authToken;
	}
	@Override
	public RequestConfigBase<T, C> setAuthToken(final Token authToken) {
		this.authToken = authToken;
		return this;
	}
	//
	@Override
	public RequestConfigBase<T, C> setAppConfig(final AppConfig appConfig) {
		super.setAppConfig(appConfig);
		final String api = appConfig.getStorageHttpApi();
		setAPI(api);
		setPort(appConfig.getStoragePort());
		setUserName(appConfig.getAuthId());
		setSecret(appConfig.getAuthSecret());
		final String tokenValue = appConfig.getAuthToken();
		setAuthToken(tokenValue == null ? null : new BasicToken(tokenValue));
		setNameSpace(appConfig.getStorageHttpNamespace());
		setSslFlag(appConfig.getNetworkSsl());
		return this;
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		LOG.trace(Markers.MSG, "Written I/O base configuration \"" + nameSpace + "\"");
		out.writeObject(getAPI());
		LOG.trace(Markers.MSG, "Written API type \"" + api + "\"");
		out.writeInt(getPort());
		LOG.trace(Markers.MSG, "Written port num \"" + port + "\"");
		out.writeObject(getUserName());
		LOG.trace(Markers.MSG, "Written user name \"" + userName + "\"");
		out.writeObject(getSecret());
		LOG.trace(Markers.MSG, "Written secret key \"" + secret + "\"");
		out.writeObject(getAuthToken());
		LOG.trace(Markers.MSG, "Written auth token \"" + authToken + "\"");
		out.writeBoolean(getSslFlag());
		LOG.trace(Markers.MSG, "Written SSL flag \"" + sslFlag + "\"");
	}
	//
	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setAPI((String) in.readObject());
		LOG.trace(Markers.MSG, "Got API {}", api);
		setPort(in.readInt());
		LOG.trace(Markers.MSG, "Got port {}", port);
		setUserName((String) in.readObject());
		LOG.trace(Markers.MSG, "Got user name {}", userName);
		setSecret((String) in.readObject());
		LOG.trace(Markers.MSG, "Got secret {}", secret);
		setAuthToken((Token) in.readObject());
		LOG.trace(Markers.MSG, "Got auth token {}", authToken);
		setSslFlag(in.readBoolean());
		LOG.trace(Markers.MSG, "Got SSL flag {}", sslFlag);
	}
	//
	@Override
	public final String toString() {
		return StringUtils.capitalize(getAPI()) + '-' +
			StringUtils.capitalize(loadType.name().toLowerCase())/* +
			((addr==null || addr.length()==0) ? "" : "@"+addr)*/;
	}
}
