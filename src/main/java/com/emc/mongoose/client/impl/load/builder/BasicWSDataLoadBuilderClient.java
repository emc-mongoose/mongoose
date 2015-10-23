package com.emc.mongoose.client.impl.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.builder.WSDataLoadBuilderSvc;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtil;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
// mongoose-client.jar
import com.emc.mongoose.client.impl.load.executor.BasicWSDataLoadClient;
import com.emc.mongoose.client.api.load.builder.WSDataLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.WSDataLoadClient;
//
import com.emc.mongoose.server.api.load.executor.WSDataLoadSvc;
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
public final class BasicWSDataLoadBuilderClient<
	T extends WSObject, W extends WSDataLoadSvc<T>, U extends WSDataLoadClient<T, W>
> extends DataLoadBuilderClientBase<T, W, U, WSDataLoadBuilderSvc<T, W>>
implements WSDataLoadBuilderClient<T, W, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSDataLoadBuilderClient()
	throws IOException {
		super();
	}
	//
	public BasicWSDataLoadBuilderClient(final RunTimeConfig runTimeConfig)
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
	protected WSDataLoadBuilderSvc<T, W> resolve(final String serverAddr)
	throws IOException {
		WSDataLoadBuilderSvc<T, W> rlb;
		final Service remoteSvc = ServiceUtil.getRemoteSvc(
			"//" + serverAddr + '/'
				+ getClass().getName()
					.replace("client", "server").replace("Client", "Svc")
		);
		if(remoteSvc == null) {
			throw new IOException("No remote load builder was resolved from " + serverAddr);
		} else if(remoteSvc instanceof WSDataLoadBuilderSvc) {
			rlb = (WSDataLoadBuilderSvc<T, W>) remoteSvc;
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
		reqConf.configureStorage(storageNodeAddrs);
	}
	//
	@Override  @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws RemoteException {
		//
		final Map<String, W> remoteLoadMap = new ConcurrentHashMap<>();
		//
		WSDataLoadBuilderSvc<T, W> nextBuilder;
		W nextLoad;
		//
		if(itemSrc == null) {
			itemSrc = getDefaultItemSource(); // affects load service builders
		}
		//
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setRequestConfig(reqConf); // should upload req conf right before instancing
			nextLoad = (W) ServiceUtil.getRemoteSvc(
				String.format("//%s/%s", addr, nextBuilder.buildRemotely())
			);
			remoteLoadMap.put(addr, nextLoad);
		}
		//
		final String loadTypeStr = reqConf.getLoadType().name().toLowerCase();
		//
		return (U) new BasicWSDataLoadClient<>(
			rtConfig, (WSRequestConfig) reqConf, storageNodeAddrs,
			rtConfig.getConnCountPerNodeFor(loadTypeStr), rtConfig.getWorkerCountFor(loadTypeStr),
			itemSrc, maxCount, remoteLoadMap
		);
	}
}
