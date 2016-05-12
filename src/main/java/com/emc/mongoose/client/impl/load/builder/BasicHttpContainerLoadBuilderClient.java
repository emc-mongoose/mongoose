package com.emc.mongoose.client.impl.load.builder;
//
import com.emc.mongoose.client.api.load.executor.HttpContainerLoadClient;
//
import com.emc.mongoose.client.impl.load.executor.BasicHttpContainerLoadClient;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
//
import com.emc.mongoose.core.impl.io.conf.HttpRequestConfigBase;
//
import com.emc.mongoose.server.api.load.builder.HttpContainerLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.HttpContainerLoadSvc;
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
public class BasicHttpContainerLoadBuilderClient<
	T extends HttpDataItem,
	C extends Container<T>,
	W extends HttpContainerLoadSvc<T, C>,
	U extends HttpContainerLoadClient<T, C, W>
> extends ContainerLoadBuilderClientBase<T, C, W, U, HttpContainerLoadBuilderSvc<T, C, W>> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicHttpContainerLoadBuilderClient()
	throws IOException {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	public BasicHttpContainerLoadBuilderClient(final AppConfig appConfig)
	throws IOException {
		super(appConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected HttpRequestConfig getIoConfig(final AppConfig appConfig) {
		return HttpRequestConfigBase.getInstance(appConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected HttpContainerLoadBuilderSvc<T, C, W> resolve(final String serverAddr)
	throws IOException {
		HttpContainerLoadBuilderSvc<T, C, W> rlb;
		final String svcUri = "//" + serverAddr + '/' +
			getClass().getName().replace("client", "server").replace("Client", "Svc");
		rlb = (HttpContainerLoadBuilderSvc<T, C, W>) ServiceUtil.getRemoteSvc(svcUri);
		rlb = (HttpContainerLoadBuilderSvc<T, C, W>) ServiceUtil.getRemoteSvc(svcUri + rlb.fork());
		return rlb;
	}
	//
	@Override
	protected Input<C> getContainerItemInput()
	throws CloneNotSupportedException {
		return null;
	}
	//
	@Override
	public final void invokePreConditions()
	throws IllegalStateException {
		//  do nothing
		//  ioConfig.configureStorage(storageNodeAddrs);
	}
	//
	@Override  @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws RemoteException {
		//
		final Map<String, W> remoteLoadMap = new ConcurrentHashMap<>();
		//
		HttpContainerLoadBuilderSvc<T, C, W> nextBuilder;
		W nextLoad;
		//
		itemInput = selectItemInput(); // affects load service builders
		for(final String addr : loadSvcMap.keySet()) {
			nextBuilder = loadSvcMap.get(addr);
			nextBuilder.setIoConfig(ioConfig); // should upload req conf right before instancing
			nextLoad = (W) ServiceUtil.getRemoteSvc(
				String.format("//%s/%s", addr, nextBuilder.buildRemotely())
			);
			remoteLoadMap.put(addr, nextLoad);
		}
		//
		return (U) new BasicHttpContainerLoadClient<>(
			appConfig, (HttpRequestConfig) ioConfig, storageNodeAddrs, appConfig.getLoadThreads(),
			itemInput, countLimit, sizeLimit, rateLimit, remoteLoadMap
		);
	}
}
