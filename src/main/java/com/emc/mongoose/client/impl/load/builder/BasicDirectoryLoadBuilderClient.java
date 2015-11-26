package com.emc.mongoose.client.impl.load.builder;
//
import com.emc.mongoose.client.api.load.builder.DirectoryLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.DirectoryLoadClient;
import com.emc.mongoose.client.impl.load.executor.BasicDirectoryLoadClient;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.req.IOConfig;
//
import com.emc.mongoose.core.impl.io.req.BasicFileIOConfig;
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
		super();
	}
	//
	public BasicDirectoryLoadBuilderClient(final RunTimeConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected IOConfig<T, C> getDefaultIOConfig() {
		return new BasicFileIOConfig();
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected DirectoryLoadBuilderSvc<T, C, W> resolve(final String serverAddr)
	throws IOException {
		DirectoryLoadBuilderSvc<T, C, W> rlb;
		final Service remoteSvc = ServiceUtil.getRemoteSvc(
			"//" + serverAddr + '/'
				+ getClass().getName()
				.replace("client", "server").replace("Client", "Svc")
		);
		if(remoteSvc == null) {
			throw new IOException("No remote load builder was resolved from " + serverAddr);
		} else if(remoteSvc instanceof DirectoryLoadBuilderSvc) {
			rlb = (DirectoryLoadBuilderSvc<T, C, W>) remoteSvc;
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
	public final void invokePreConditions()
	throws IllegalStateException, RemoteException {
		DirectoryLoadBuilderSvc<T, C, W> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
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
		return (U) new BasicDirectoryLoadClient<>(
			rtConfig, (IOConfig) ioConfig, storageNodeAddrs,
			rtConfig.getConnCountPerNodeFor(loadTypeStr), rtConfig.getWorkerCountFor(loadTypeStr),
			itemSrc, maxCount, remoteLoadMap
		);
	}
}
