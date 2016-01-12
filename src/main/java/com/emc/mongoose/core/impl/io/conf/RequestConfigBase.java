package com.emc.mongoose.core.impl.io.conf;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.io.conf.RequestConfig;
// mongoose-core-impl.jar
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
extends IOConfigBase<T, C>
implements RequestConfig<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//protected final static String FMT_URI_ADDR = "%s://%s:%s";
	protected volatile int port = 0;
	//
	protected RequestConfigBase() {
		super();
	}
	//
	protected RequestConfigBase(final RequestConfigBase<T, C> reqConf2Clone) {
		super(reqConf2Clone);
		if(reqConf2Clone != null) {
			setPort(reqConf2Clone.getPort());
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
		requestConfigBranch.setPort(port);
		LOG.debug(
			Markers.MSG, "Forked conf conf #{} from #{}", requestConfigBranch.hashCode(), hashCode()
		);
		return requestConfigBranch;
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
	public final RequestConfigBase<T, C> setPort(final int port)
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
	public RequestConfigBase<T, C> setRunTimeConfig(final RunTimeConfig runTimeConfig) {
		super.setRunTimeConfig(runTimeConfig);
		final String api = runTimeConfig.getApiName();
		setPort(this.runTimeConfig.getApiTypePort(api));
		setNameSpace(this.runTimeConfig.getStorageNameSpace());
		setBuffSize((int) this.runTimeConfig.getIOBufferSizeMin());
		return this;
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		LOG.trace(Markers.MSG, "Written I/O base configuration \"" + nameSpace + "\"");
		out.writeInt(getPort());
		LOG.trace(Markers.MSG, "Written port num \"" + port + "\"");
	}
	//
	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setPort(in.readInt());
		LOG.trace(Markers.MSG, "Got port {}", port);
	}
	//
	@Override
	public String toString() {
		return super.toString() + ":" + port;
	}
}
