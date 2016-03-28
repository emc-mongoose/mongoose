package com.emc.mongoose.client.impl.load.builder;
//
import com.emc.mongoose.client.api.load.builder.DirectoryLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.DirectoryLoadClient;
import com.emc.mongoose.client.impl.load.executor.BasicDirectoryLoadClient;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
//
import com.emc.mongoose.core.impl.io.conf.BasicFileIOConfig;
//
import com.emc.mongoose.server.api.load.builder.DirectoryLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.DirectoryLoadSvc;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 26.11.15.
 */
public class BasicDirectoryLoadBuilderClient<
	T extends FileItem, C extends Directory<T>, W extends DirectoryLoadSvc<T, C>,
	U extends DirectoryLoadClient<T, C, W>
> extends ContainerLoadBuilderClientBase<T, C, W, U, DirectoryLoadBuilderSvc<T, C, W>>
implements DirectoryLoadBuilderClient<T, C, W, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicDirectoryLoadBuilderClient()
	throws IOException {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	public BasicDirectoryLoadBuilderClient(final AppConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected FileIOConfig<T, C> getDefaultIoConfig() {
		return new BasicFileIOConfig();
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected DirectoryLoadBuilderSvc<T, C, W> resolve(final String serverAddr)
	throws IOException {
		DirectoryLoadBuilderSvc<T, C, W> rlb;
		final String svcUri = "//" + serverAddr + '/' +
			getClass().getName().replace("client", "server").replace("Client", "Svc");
		rlb = (DirectoryLoadBuilderSvc<T, C, W>) ServiceUtil.getRemoteSvc(svcUri);
		rlb = (DirectoryLoadBuilderSvc<T, C, W>) ServiceUtil.getRemoteSvc(svcUri + rlb.fork());
		return rlb;
	}
	//
	@Override
	protected ItemSrc<C> getDefaultItemSrc() {
		return null;
	}
	//
	@Override
	public final void invokePreConditions()
	throws IllegalStateException, RemoteException {
		DirectoryLoadBuilderSvc<T, C, W> nextBuilder;
		for(final String addr : loadSvcMap.keySet()) {
			nextBuilder = loadSvcMap.get(addr);
			nextBuilder.invokePreConditions();
		}
	}
	//
	@Override  @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws RemoteException {
		//
		final Map<String, W> remoteLoadMap = new ConcurrentHashMap<>();
		//
		DirectoryLoadBuilderSvc<T, C, W> nextBuilder;
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
		return (U) new BasicDirectoryLoadClient<>(
			appConfig, (FileIOConfig) ioConfig, storageNodeAddrs, appConfig.getLoadThreads(),
			itemSrc, maxCount, rateLimit, remoteLoadMap
		);
	}
}
