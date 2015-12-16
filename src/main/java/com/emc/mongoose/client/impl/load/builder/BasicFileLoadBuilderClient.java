package com.emc.mongoose.client.impl.load.builder;
import com.emc.mongoose.client.api.load.builder.FileLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.FileLoadClient;
import com.emc.mongoose.client.impl.load.executor.BasicFileLoadClient;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.impl.io.conf.BasicFileIOConfig;
import com.emc.mongoose.server.api.load.builder.FileLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.FileLoadSvc;

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
		this(RunTimeConfig.getContext());
	}
	//
	public BasicFileLoadBuilderClient(final RunTimeConfig rtConfig)
	throws IOException {
		super(rtConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected FileIOConfig<T, ? extends Directory<T>> getDefaultIOConfig() {
		return new BasicFileIOConfig<>();
	}
	//
	@Override
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
		final String loadTypeStr = ioConfig.getLoadType().name().toLowerCase();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		//
		return (U) new BasicFileLoadClient<>(
			rtConfig, (FileIOConfig<T, ? extends Directory<T>>) ioConfig, storageNodeAddrs,
			rtConfig.getConnCountPerNodeFor(loadTypeStr), rtConfig.getWorkerCountFor(loadTypeStr),
			itemSrc, maxCount, remoteLoadMap
		);
	}
}
