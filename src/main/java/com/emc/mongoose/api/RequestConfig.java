package com.emc.mongoose.api;
//
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.data.UniformData;
import com.emc.mongoose.data.UniformDataSource;
import com.emc.mongoose.logging.Markers;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
/**
 Created by kurila on 06.06.14.
 */
public class RequestConfig<T extends UniformData>
implements Externalizable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static int REQUEST_TIMEOUT_MILLISEC = RunTimeConfig.getInt(
		"run.request.timeout.millisec"
	);
	//
	protected String addr, api, secret, userName;
	protected int port;
	protected Request.Type loadType;
	protected UniformDataSource dataSrc = UniformDataSource.DEFAULT;
	protected volatile boolean retryFlag = RunTimeConfig.getBoolean("run.request.retries");
	//
	public final String getAPI() {
		return api;
	}
	public final RequestConfig<T> setAPI(final String api) {
		this.api = api;
		return this;
	}
	//
	public String getAddr() {
		return addr;
	}
	public RequestConfig<T> setAddr(final String addr) {
		this.addr = addr;
		return this;
	}
	//
	public final Request.Type getLoadType() {
		return loadType;
	}
	public final RequestConfig<T> setLoadType(final Request.Type loadType) {
		LOG.debug(Markers.MSG, "Setting load type {}", loadType);
		this.loadType = loadType;
		return this;
	}
	//
	public int getPort() {
		return port;
	}
	public RequestConfig<T> setPort(final int port)
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
	public final String getUserName() {
		return userName;
	}
	public RequestConfig<T> setUserName(final String userName) {
		this.userName = userName;
		return this;
	}
	//
	public final String getSecret() {
		return secret;
	}
	public RequestConfig<T> setSecret(final String secret) {
		this.secret = secret;
		return this;
	}
	//
	public final UniformDataSource getDataSource() {
		return dataSrc;
	}
	public final RequestConfig<T> setDataSource(final UniformDataSource dataSrc) {
		this.dataSrc = dataSrc;
		return this;
	}
	//
	public final boolean getRetries() {
		return retryFlag;
	}
	public RequestConfig<T> setRetries(final boolean retryFlag) {
		this.retryFlag = retryFlag;
		return this;
	}
	//
	public RequestConfig<T> setProperties(final RunTimeConfig props) {
		setAPI(RunTimeConfig.getString("storage.api"));
		LOG.debug(Markers.MSG, "Using API: \"{}\"", api);
		setPort(RunTimeConfig.getInt("api." + api + ".port"));
		setUserName(RunTimeConfig.getString("auth.id"));
		setSecret(RunTimeConfig.getString("auth.secret"));
		setRetries(RunTimeConfig.getBoolean("run.request.retries"));
		return this;
	}
	//
	public RequestConfig<T> clone() {
		return new RequestConfig<T>()
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
