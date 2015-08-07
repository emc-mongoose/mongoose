package com.emc.mongoose.core.impl.io.req;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataSource;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.RequestConfig;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
//
import org.apache.commons.lang.StringUtils;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 Created by kurila on 06.06.14.
 The most common implementation of the shared request configuration.
 */
public abstract class RequestConfigBase<T extends DataItem>
implements RequestConfig<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//protected final static String FMT_URI_ADDR = "%s://%s:%s";
	//
	protected String
		api, secret, userName;
	protected IOTask.Type
		loadType;
	protected DataSource
		dataSrc;
	protected volatile boolean
		verifyContentFlag, anyDataProducerEnabled;
	private final AtomicBoolean closeFlag = new AtomicBoolean(false);
	protected volatile RunTimeConfig runTimeConfig;
	protected volatile String
		/*addr, */nameSpace, scheme/*, uriTemplate*/;
	protected volatile int
		port;
	protected int buffSize;
	protected int reqSleepMilliSec;
	//
	@SuppressWarnings("unchecked")
	protected RequestConfigBase() {
		runTimeConfig = RunTimeConfig.getContext();
		api = runTimeConfig.getApiName();
		secret = runTimeConfig.getAuthSecret();
		userName = runTimeConfig.getAuthId();
		loadType = IOTask.Type.CREATE;
		dataSrc = UniformDataSource.DEFAULT;
		verifyContentFlag = runTimeConfig.getReadVerifyContent();
		anyDataProducerEnabled = true;
		scheme = runTimeConfig.getStorageProto();
		port = runTimeConfig.getApiTypePort(api);
		nameSpace = runTimeConfig.getStorageNameSpace();
		buffSize = (int) runTimeConfig.getDataBufferSize();
		reqSleepMilliSec = runTimeConfig.getLoadLimitReqSleepMilliSec();
	}
	//
	protected RequestConfigBase(final RequestConfig<T> reqConf2Clone) {
		this();
		if(reqConf2Clone != null) {
			setDataSource(reqConf2Clone.getDataSource());
			setVerifyContentFlag(reqConf2Clone.getVerifyContentFlag());
			setAnyDataProducerEnabled(reqConf2Clone.getAnyDataProducerEnabled());
			setAPI(reqConf2Clone.getAPI());
			setUserName(reqConf2Clone.getUserName());
			setPort(reqConf2Clone.getPort());
			setScheme(reqConf2Clone.getScheme());
			setLoadType(reqConf2Clone.getLoadType());
			setNameSpace(reqConf2Clone.getNameSpace());
			secret = reqConf2Clone.getSecret();
			setBuffSize(reqConf2Clone.getBuffSize());
			setReqSleepMilliSec(reqConf2Clone.getReqSleepMilliSec());
			LOG.debug(
				Markers.MSG, "Forked req conf #{} from #{}", hashCode(), reqConf2Clone.hashCode()
			);
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public RequestConfigBase<T> clone()
	throws CloneNotSupportedException {
		final RequestConfigBase<T> requestConfigBranch = (RequestConfigBase<T>) super.clone();
		requestConfigBranch
			.setDataSource(dataSrc)
			.setVerifyContentFlag(verifyContentFlag)
			.setAnyDataProducerEnabled(anyDataProducerEnabled)
			.setAPI(api)
			.setUserName(userName)
			.setPort(port)
			.setScheme(scheme)
			.setLoadType(loadType)
			.setNameSpace(nameSpace)
			.setBuffSize(buffSize)
			.setReqSleepMilliSec(reqSleepMilliSec);
		requestConfigBranch.secret = secret;
		LOG.debug(
			Markers.MSG, "Forked req conf #{} from #{}", requestConfigBranch.hashCode(), hashCode()
		);
		return requestConfigBranch;
	}
	//
	@Override
	public final String getAPI() {
		return api;
	}
	@Override
	public RequestConfigBase<T> setAPI(final String api) {
		this.api = api;
		return this;
	}
	//
	@Override
	public final IOTask.Type getLoadType() {
		return loadType;
	}
	@Override
	public RequestConfigBase<T> setLoadType(final IOTask.Type loadType) {
		LOG.trace(Markers.MSG, "Setting load type {}", loadType);
		this.loadType = loadType;
		return this;
	}
	//
	@Override
	public final String getScheme() {
		return scheme;
	}
	@Override
	public final RequestConfigBase<T> setScheme(final String scheme) {
		this.scheme = scheme;
		return this;
	}
	/*
	@Override
	public final String getAddr() {
		return addr;
	}
	@Override
	public final RequestConfigBase<T> setAddr(final String addr) {
		if(addr == null) {
			throw new IllegalArgumentException("Attempted to set <null> address");
		} else if(addr.contains(":")) {
			final String[] hostAndPort = addr.split(":", 2);
			setPort(Integer.parseInt(hostAndPort[1]));
			this.addr = hostAndPort[0];
		} else {
			this.addr = addr;
		}
		uriTemplate = String.format(
			FMT_URI_ADDR,
			scheme == null ? "%s" : scheme, addr,
			(port > 0 && port < 0x10000) ? Integer.toString(port) : "%s"
		);
		return this;
	}*/
	//
	@Override
	public final int getPort() {
		return port;
	}
	@Override
	public final RequestConfigBase<T> setPort(final int port)
	throws IllegalArgumentException {
		LOG.trace(Markers.MSG, "Using storage port: {}", port);
		if(port > 0 || port < 0x10000) {
			this.port = port;
			/*uriTemplate = String.format(
				FMT_URI_ADDR,
				scheme == null ? "%s" : scheme, addr == null ? "%s" : addr,
				Integer.toString(port)
			);*/
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
	public RequestConfigBase<T> setUserName(final String userName) {
		this.userName = userName;
		return this;
	}
	//
	@Override
	public final String getSecret() {
		return secret;
	}
	@Override
	public RequestConfigBase<T> setSecret(final String secret) {
		this.secret = secret;
		return this;
	}
	//
	@Override
	public final String getNameSpace() {
		return nameSpace;
	}
	//
	@Override
	public RequestConfigBase<T> setNameSpace(final String nameSpace) {
		this.nameSpace = nameSpace;
		return this;
	}
	//
	@Override
	public final DataSource getDataSource() {
		return dataSrc;
	}
	@Override
	public RequestConfigBase<T> setDataSource(final DataSource dataSrc) {
		this.dataSrc = dataSrc;
		return this;
	}
	//
	@Override
	public final boolean getVerifyContentFlag() {
		return verifyContentFlag;
	}
	//
	@Override
	public final RequestConfigBase<T> setVerifyContentFlag(final boolean verifyContentFlag) {
		this.verifyContentFlag = verifyContentFlag;
		return this;
	}
	//
	@Override
	public final boolean getAnyDataProducerEnabled() {
		return anyDataProducerEnabled;
	}
	//
	@Override
	public final RequestConfigBase<T> setAnyDataProducerEnabled(final boolean enabledFlag) {
		this.anyDataProducerEnabled = enabledFlag;
		return this;
	}
	//
	@Override
	public final int getReqSleepMilliSec() {
		return reqSleepMilliSec;
	}
	@Override
	public final RequestConfigBase<T> setReqSleepMilliSec(final int reqSleepMilliSec)
	throws IllegalArgumentException {
		if(reqSleepMilliSec < 0) {
			throw new IllegalArgumentException("Request sleep time shouldn't have a negative value");
		}
		this.reqSleepMilliSec = reqSleepMilliSec;
		return this;
	}
	//
	@Override
	public RequestConfigBase<T> setProperties(final RunTimeConfig runTimeConfig) {
		this.runTimeConfig = runTimeConfig;
		//
		final String api = runTimeConfig.getApiName();
		setAPI(api);
		setPort(this.runTimeConfig.getApiTypePort(api));
		setUserName(this.runTimeConfig.getAuthId());
		setSecret(this.runTimeConfig.getAuthSecret());
		setNameSpace(this.runTimeConfig.getStorageNameSpace());
		setBuffSize((int)this.runTimeConfig.getDataBufferSize());
		setReqSleepMilliSec(this.runTimeConfig.getLoadLimitReqSleepMilliSec());
		return this;
	}
	//
	@Override
	public final int getBuffSize() {
		return buffSize;
	}
	//
	@Override
	public final RequestConfigBase<T> setBuffSize(final int buffSize) {
		this.buffSize = buffSize;
		return this;
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeObject(getAPI());
		out.writeObject(getLoadType());
		out.writeObject(getScheme());
		out.writeInt(getPort());
		out.writeObject(getUserName());
		out.writeObject(getSecret());
		out.writeObject(getNameSpace());
		out.writeObject(getDataSource());
		out.writeBoolean(getAnyDataProducerEnabled());
		out.writeBoolean(getVerifyContentFlag());
		out.writeInt(getReqSleepMilliSec());
	}
	//
	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		setAPI(String.class.cast(in.readObject()));
		LOG.trace(Markers.MSG, "Got API {}", api);
		setLoadType(IOTask.Type.class.cast(in.readObject()));
		LOG.trace(Markers.MSG, "Got load type {}", loadType);
		setScheme(String.class.cast(in.readObject()));
		LOG.trace(Markers.MSG, "Got scheme {}", scheme);
		setPort(in.readInt());
		LOG.trace(Markers.MSG, "Got port {}", port);
		setUserName(String.class.cast(in.readObject()));
		LOG.trace(Markers.MSG, "Got user name {}", userName);
		setSecret(String.class.cast(in.readObject()));
		LOG.trace(Markers.MSG, "Got secret {}", secret);
		setNameSpace(String.class.cast(in.readObject()));
		LOG.trace(Markers.MSG, "Got namespace {}", secret);
		setDataSource(DataSource.class.cast(in.readObject()));
		LOG.trace(Markers.MSG, "Got data source {}", dataSrc);
		setAnyDataProducerEnabled(Boolean.class.cast(in.readBoolean()));
		LOG.trace(Markers.MSG, "Got any producer enabled flag {}", anyDataProducerEnabled);
		setVerifyContentFlag(in.readBoolean());
		LOG.trace(Markers.MSG, "Got verify content flag {}", verifyContentFlag);
		setReqSleepMilliSec(in.readInt());
		LOG.trace(Markers.MSG, "Got requests sleep time {}", reqSleepMilliSec);
	}
	//
	@Override
	public final String toString() {
		return StringUtils.capitalize(getAPI()) + '.' +
			StringUtils.capitalize(loadType.name().toLowerCase())/* +
			((addr==null || addr.length()==0) ? "" : "@"+addr)*/;
	}
	//
	@Override
	public void close()
	throws IOException {
		if(closeFlag.compareAndSet(false, true)) {
			LOG.debug(Markers.MSG, "Request config instance #{} marked as closed", hashCode());
		}
	}
	//
	@Override
	public final boolean isClosed() {
		return closeFlag.get();
	}
	//
	@Override
	protected void finalize()
	throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}
}
