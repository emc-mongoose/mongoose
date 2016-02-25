package com.emc.mongoose.client.impl.load.builder;
//
import com.emc.mongoose.client.api.load.builder.FileLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.FileLoadClient;
//
import com.emc.mongoose.client.impl.load.executor.BasicFileLoadClient;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
//
import com.emc.mongoose.core.impl.io.conf.BasicFileIOConfig;
//
import com.emc.mongoose.server.api.load.builder.FileLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.FileLoadSvc;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 26.11.15.
 */
public class BasicFileLoadBuilderClient<
	T extends FileItem, W extends FileLoadSvc<T>, U extends FileLoadClient<T, W>
> extends DataLoadBuilderClientBase<T, W, U, FileLoadBuilderSvc<T, W>>
implements FileLoadBuilderClient<T, W, U> {
	//
	public BasicFileLoadBuilderClient()
	throws IOException {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	public BasicFileLoadBuilderClient(final AppConfig appConfig)
	throws IOException {
		super(appConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected FileIOConfig<T, ? extends Directory<T>> getDefaultIOConfig() {
		return new BasicFileIOConfig<>();
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected FileLoadBuilderSvc<T, W> resolve(final String serverAddr)
	throws IOException {
		FileLoadBuilderSvc<T, W> rlb;
		final String svcUri = "//" + serverAddr + '/' +
			getClass().getName().replace("client", "server").replace("Client", "Svc");
		rlb = (FileLoadBuilderSvc<T, W>) ServiceUtil.getRemoteSvc(svcUri);
		rlb = (FileLoadBuilderSvc<T, W>) ServiceUtil.getRemoteSvc(svcUri + rlb.fork());
		return rlb;
	}
	//
	@Override
	public void invokePreConditions()
	throws IllegalStateException, RemoteException {
		FileLoadBuilderSvc<T, W> nextBuilder;
		for(final String addr : loadSvcMap.keySet()) {
			nextBuilder = loadSvcMap.get(addr);
			nextBuilder.invokePreConditions();
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected U buildActually()
	throws RemoteException, DuplicateSvcNameException {
		final Map<String, W> remoteLoadMap = new ConcurrentHashMap<>();
		//
		FileLoadBuilderSvc<T, W> nextBuilder;
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
		return (U) new BasicFileLoadClient<>(
			appConfig, (FileIOConfig<T, ? extends Directory<T>>) ioConfig, storageNodeAddrs,
			appConfig.getLoadThreads(), itemSrc, maxCount, remoteLoadMap
		);
	}
}
