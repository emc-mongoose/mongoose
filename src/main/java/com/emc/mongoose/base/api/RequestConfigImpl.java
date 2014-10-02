package com.emc.mongoose.base.api;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.DataSource;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.base.data.impl.UniformDataSource;
import com.emc.mongoose.util.logging.Markers;
import org.apache.commons.lang.StringUtils;
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
public class RequestConfigImpl<T extends DataItem>
implements RequestConfig<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected String addr, api, secret, userName;
	protected int port;
	protected Request.Type loadType;
	protected DataSource<T> dataSrc = UniformDataSource.DEFAULT;
	protected volatile boolean retryFlag = RunTimeConfig.getBoolean("run.request.retries");
	//
	@Override
	public final String getAPI() {
		return api;
	}
	@Override
	public RequestConfigImpl<T> setAPI(final String api) {
		this.api = api;
		return this;
	}
	//
	@Override
	public String getAddr() {
		return addr;
	}
	@Override
	public RequestConfigImpl<T> setAddr(final String addr) {
		this.addr = addr;
		return this;
	}
	//
	@Override
	public final Request.Type getLoadType() {
		return loadType;
	}
	@Override
	public RequestConfigImpl<T> setLoadType(final Request.Type loadType) {
		LOG.debug(Markers.MSG, "Setting load type {}", loadType);
		this.loadType = loadType;
		return this;
	}
	//
	@Override
	public int getPort() {
		return port;
	}
	@Override
	public RequestConfigImpl<T> setPort(final int port)
	throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Using storage port: {}", port);
		if(port>0 || port<0x10000) {
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
	public RequestConfigImpl<T> setUserName(final String userName) {
		this.userName = userName;
		return this;
	}
	//
	@Override
	public final String getSecret() {
		return secret;
	}
	@Override
	public RequestConfigImpl<T> setSecret(final String secret) {
		this.secret = secret;
		return this;
	}
	//
	@Override
	public final DataSource<T> getDataSource() {
		return dataSrc;
	}
	@Override
	public RequestConfigImpl<T> setDataSource(final DataSource<T> dataSrc) {
		this.dataSrc = dataSrc;
		return this;
	}
	//
	@Override
	public final boolean getRetries() {
		return retryFlag;
	}
	@Override
	public RequestConfigImpl<T> setRetries(final boolean retryFlag) {
		this.retryFlag = retryFlag;
		return this;
	}
	//
	@Override
	public RequestConfigImpl<T> setProperties(final RunTimeConfig props) {
		setAPI(RunTimeConfig.getString("storage.api"));
		LOG.debug(Markers.MSG, "Using API: \"{}\"", api);
		setPort(RunTimeConfig.getInt("api." + api + ".port"));
		setUserName(RunTimeConfig.getString("auth.id"));
		setSecret(RunTimeConfig.getString("auth.secret"));
		setRetries(RunTimeConfig.getBoolean("run.request.retries"));
		return this;
	}
	//
	@Override
	@SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
	public RequestConfigImpl<T> clone() {
		return new RequestConfigImpl<T>()
			.setAPI(getAPI())
			.setAddr(getAddr())
			.setLoadType(getLoadType())
			.setPort(getPort())
			.setUserName(getUserName())
			.setSecret(getSecret())
			.setRetries(getRetries());
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeObject(getAPI());
		out.writeObject(getAddr());
		out.writeObject(getLoadType());
		out.writeInt(getPort());
		out.writeObject(getUserName());
		out.writeObject(getSecret());
		out.writeObject(getDataSource());
		out.writeBoolean(getRetries());
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		setAPI(String.class.cast(in.readObject()));
		setAddr(String.class.cast(in.readObject()));
		setLoadType(Request.Type.class.cast(in.readObject()));
		setPort(in.readInt());
		setUserName(String.class.cast(in.readObject()));
		setSecret(String.class.cast(in.readObject()));
		setDataSource(UniformDataSource.class.cast(in.readObject()));
		setRetries(Boolean.class.cast(in.readBoolean()));
	}
	//
	@Override
	public final String toString() {
		return StringUtils.capitalize(getAPI()) + '.' +
			StringUtils.capitalize(loadType.name().toLowerCase()) +
			((addr==null || addr.length()==0) ? "" : "@"+addr);
	}
	//
}
