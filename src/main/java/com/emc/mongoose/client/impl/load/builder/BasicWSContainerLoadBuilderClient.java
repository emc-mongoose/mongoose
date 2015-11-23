package com.emc.mongoose.client.impl.load.builder;
//
import com.emc.mongoose.client.api.load.executor.WSContainerLoadClient;
//
import com.emc.mongoose.client.impl.load.executor.BasicWSContainerLoadClient;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
//
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
//
import com.emc.mongoose.server.api.load.builder.WSContainerLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.WSContainerLoadSvc;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 21.10.15.
 */
public class BasicWSContainerLoadBuilderClient<
	T extends WSObject,
	C extends Container<T>,
	W extends WSContainerLoadSvc<T, C>,
	U extends WSContainerLoadClient<T, C, W>
> extends ContainerLoadBuilderClientBase<T, C, W, U, WSContainerLoadBuilderSvc<T, C, W>> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSContainerLoadBuilderClient()
	throws IOException {
		super();
	}
	//
	public BasicWSContainerLoadBuilderClient(final RunTimeConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected WSRequestConfig getDefaultIOConfig() {
		return WSRequestConfigBase.getInstance();
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected WSContainerLoadBuilderSvc<T, C, W> resolve(final String serverAddr)
	throws IOException {
		WSContainerLoadBuilderSvc<T, C, W> rlb;
		final Service remoteSvc = ServiceUtil.getRemoteSvc(
			"//" + serverAddr + '/'
				+ getClass().getName()
					.replace("client", "server").replace("Client", "Svc")
		);
		if(remoteSvc == null) {
			throw new IOException("No remote load builder was resolved from " + serverAddr);
		} else if(remoteSvc instanceof WSContainerLoadBuilderSvc) {
			rlb = (WSContainerLoadBuilderSvc<T, C, W>) remoteSvc;
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
	protected ItemSrc<C> getDefaultItemSource() {
		return null;
	}
	//
	@Override
	protected final void invokePreConditions()
	throws IllegalStateException {
		//  do nothing
		//  ioConfig.configureStorage(storageNodeAddrs);
	}
	//
	@Override  @SuppressWarnings("unchecked")
	protected final U buildActually()
		throws RemoteException {
		//
		final Map<String, WSContainerLoadSvc<T, C>> remoteLoadMap = new ConcurrentHashMap<>();
		//
		WSContainerLoadBuilderSvc<T, C, W> nextBuilder;
		W nextLoad;
		//
		if(itemSrc == null) {
			itemSrc = getDefaultItemSource(); // affects load service builders
		}
		//
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setIOConfig(ioConfig); // should upload req conf right before instancing
			nextLoad = (W) ServiceUtil.getRemoteSvc(
				String.format("//%s/%s", addr, nextBuilder.buildRemotely())
			);
			remoteLoadMap.put(addr, nextLoad);
		}
		//
		final String loadTypeStr = ioConfig.getLoadType().name().toLowerCase();
		//
		return (U) new BasicWSContainerLoadClient<>(
			rtConfig, (WSRequestConfig) ioConfig, storageNodeAddrs,
			rtConfig.getConnCountPerNodeFor(loadTypeStr), rtConfig.getWorkerCountFor(loadTypeStr),
			itemSrc, maxCount, remoteLoadMap
		);
	}
}
