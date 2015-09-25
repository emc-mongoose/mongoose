package com.emc.mongoose.client.impl.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.api.data.WSObject;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.builder.WSLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.LoadSvc;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtil;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
// mongoose-client.jar
import com.emc.mongoose.client.impl.load.executor.BasicWSLoadClient;
import com.emc.mongoose.client.api.load.builder.WSLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.WSLoadClient;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 08.05.14.
 */
public final class BasicWSLoadBuilderClient<T extends WSObject, U extends WSLoadClient<T>>
extends LoadBuilderClientBase<T, U>
implements WSLoadBuilderClient<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSLoadBuilderClient()
	throws IOException {
		super();
	}
	//
	public BasicWSLoadBuilderClient(final RunTimeConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected WSRequestConfig<T> getDefaultRequestConfig() {
		return WSRequestConfigBase.getInstance();
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected WSLoadBuilderSvc<T, U> resolve(final String serverAddr)
	throws IOException {
		WSLoadBuilderSvc<T, U> rlb;
		final Service remoteSvc = ServiceUtil.getRemoteSvc(
			"//" + serverAddr + '/' + getClass().getPackage().getName().replace("client", "server")
		);
		if(remoteSvc == null) {
			throw new IOException("No remote load builder was resolved from " + serverAddr);
		} else if(remoteSvc instanceof WSLoadBuilderSvc) {
			rlb = (WSLoadBuilderSvc<T, U>) remoteSvc;
		} else {
			throw new IOException(
				"Illegal class " + remoteSvc.getClass().getCanonicalName() +
				" of the instance resolved from " + serverAddr
			);
		}
		return rlb;
	}
	//
	@Override
	protected final void invokePreConditions()
	throws IllegalStateException {
		reqConf.configureStorage(nodeAddrs);
	}
	//
	@Override  @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws RemoteException {
		//
		final Map<String, LoadSvc<T>> remoteLoadMap = new ConcurrentHashMap<>();
		//
		LoadBuilderSvc<T, U> nextBuilder;
		LoadSvc<T> nextLoad;
		//
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setRequestConfig(reqConf); // should upload req conf right before instancing
			nextLoad = (LoadSvc<T>) ServiceUtil.getRemoteSvc(
				String.format("//%s/%s", addr, nextBuilder.buildRemotely())
			);
			remoteLoadMap.put(addr, nextLoad);
		}
		//
		final String loadTypeStr = reqConf.getLoadType().name().toLowerCase();
		//
		return (U) new BasicWSLoadClient<>(
			rtConfig, (WSRequestConfig<T>) reqConf, nodeAddrs,
			rtConfig.getConnCountPerNodeFor(loadTypeStr), rtConfig.getWorkerCountFor(loadTypeStr),
			itemSrc == null ? reqConf.getContainerListInput(maxCount, nodeAddrs[0]) : itemSrc,
			maxCount, remoteLoadMap
		);
	}
}
