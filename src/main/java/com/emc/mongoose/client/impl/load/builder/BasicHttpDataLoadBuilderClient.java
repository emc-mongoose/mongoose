package com.emc.mongoose.client.impl.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.client.impl.load.executor.MixedHttpDataLoadClient;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.item.base.ItemSrc;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
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
		super(appConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected HttpRequestConfig<T, ? extends Container<T>> getDefaultIoConfig() {
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
			itemSrc = getDefaultItemSrc(); // affects load service builders
		}
		//
		for(final String addr : loadSvcMap.keySet()) {
			nextBuilder = loadSvcMap.get(addr);
			nextBuilder.setIoConfig(ioConfig); // should upload req conf right before instancing
			nextLoad = (W) ServiceUtil.getRemoteSvc(
				String.format("//%s/%s", addr, nextBuilder.buildRemotely())
			);
			remoteLoadMap.put(addr, nextLoad);
		}
		//
		final LoadType loadType = ioConfig.getLoadType();
		if(LoadType.MIXED.equals(loadType)) {
			final Map<LoadType, Integer> loadTypeWeightMap = LoadType.getMixedLoadWeights(
				(List<String>) appConfig.getProperty(AppConfig.KEY_LOAD_TYPE)
			);
			final Map<LoadType, ItemSrc<T>> itemSrcMap = new HashMap<>();
			for(final LoadType nextLoadType : loadTypeWeightMap.keySet()) {
				try {
					itemSrcMap.put(
						nextLoadType,
						LoadType.WRITE.equals(nextLoadType) ? getNewItemSrc() : itemSrc
					);
				} catch(final NoSuchMethodException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to build new item src");
				}
			}
			return (U) new MixedHttpDataLoadClient<>(
				appConfig, (HttpRequestConfig) ioConfig, storageNodeAddrs, threadCount,
				maxCount, rateLimit, remoteLoadMap, loadTypeWeightMap, itemSrcMap
			);
		} else {
			return (U) new BasicHttpDataLoadClient<>(
				appConfig, (HttpRequestConfig) ioConfig, storageNodeAddrs, threadCount,
				itemSrc, maxCount, rateLimit, remoteLoadMap
			);
		}
	}
}
