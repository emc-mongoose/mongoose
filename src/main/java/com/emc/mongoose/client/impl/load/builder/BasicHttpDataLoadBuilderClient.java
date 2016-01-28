package com.emc.mongoose.client.impl.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.builder.HttpDataLoadBuilderSvc;
// mongoose-common.jar
import com.emc.mongoose.common.net.ServiceUtil;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.io.conf.HttpRequestConfigBase;
// mongoose-client.jar
import com.emc.mongoose.client.impl.load.executor.BasicHttpDataLoadClient;
import com.emc.mongoose.client.api.load.builder.HttpDataLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.HttpDataLoadClient;
//
import com.emc.mongoose.server.api.load.executor.HttpDataLoadSvc;
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
public final class BasicHttpDataLoadBuilderClient<
	T extends HttpDataItem, W extends HttpDataLoadSvc<T>, U extends HttpDataLoadClient<T, W>
> extends DataLoadBuilderClientBase<T, W, U, HttpDataLoadBuilderSvc<T, W>>
implements HttpDataLoadBuilderClient<T, W, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicHttpDataLoadBuilderClient()
	throws IOException {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	public BasicHttpDataLoadBuilderClient(final AppConfig appConfig)
	throws IOException {
		super(runTimeConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected HttpRequestConfig<T, ? extends Container<T>> getDefaultIOConfig() {
		return HttpRequestConfigBase.getInstance();
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected HttpDataLoadBuilderSvc<T, W> resolve(final String serverAddr)
	throws IOException {
		HttpDataLoadBuilderSvc<T, W> rlb;
		final String svcUri = "//" + serverAddr + '/' +
			getClass().getName().replace("client", "server").replace("Client", "Svc");
		rlb = (HttpDataLoadBuilderSvc<T, W>) ServiceUtil.getRemoteSvc(svcUri);
		rlb = (HttpDataLoadBuilderSvc<T, W>) ServiceUtil.getRemoteSvc(svcUri + rlb.fork());
		return rlb;
	}
	//
	@Override
	public final void invokePreConditions()
	throws IllegalStateException {
		((HttpRequestConfig) ioConfig).configureStorage(storageNodeAddrs);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws RemoteException {
		//
		final Map<String, W> remoteLoadMap = new ConcurrentHashMap<>();
		//
		HttpDataLoadBuilderSvc<T, W> nextBuilder;
		W nextLoad;
		//
		if(itemSrc == null) {
			itemSrc = getDefaultItemSource(); // affects load service builders
		}
		//
		for(final String addr : loadSvcMap.keySet()) {
			nextBuilder = loadSvcMap.get(addr);
			nextBuilder.setIOConfig(ioConfig); // should upload req conf right before instancing
			nextLoad = (W) ServiceUtil.getRemoteSvc(
				String.format("//%s/%s", addr, nextBuilder.buildRemotely())
			);
			remoteLoadMap.put(addr, nextLoad);
		}
		//
		final String loadTypeStr = ioConfig.getLoadType().name().toLowerCase();
		//
		return (U) new BasicHttpDataLoadClient<>(
			appConfig, (HttpRequestConfig) ioConfig, storageNodeAddrs,
			appConfig.getConnCountPerNodeFor(loadTypeStr), appConfig.getWorkerCountFor(loadTypeStr),
			itemSrc, maxCount, remoteLoadMap
		);
	}
}
