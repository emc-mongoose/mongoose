package com.emc.mongoose.client.impl.load.executor;
//
import com.emc.mongoose.client.api.load.executor.DirectoryLoadClient;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.req.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
//
import com.emc.mongoose.core.impl.io.task.BasicDirectoryIOTask;
//
import com.emc.mongoose.server.api.load.executor.DirectoryLoadSvc;
//
import java.rmi.RemoteException;
import java.util.Map;
/**
 Created by kurila on 26.11.15.
 */
public class BasicDirectoryLoadClient<
	T extends FileItem, C extends Directory<T>, W extends DirectoryLoadSvc<T, C>
> extends LoadClientBase<C, W> implements DirectoryLoadClient<T, C, W> {
	//
	public BasicDirectoryLoadClient(
		final RunTimeConfig rtConfig, final IOConfig<T, ? extends Directory<T>> ioConfig,
		final String[] addrs, final int connCountPerNode, final int threadCount,
		final ItemSrc<C> itemSrc, final long maxCount, final Map<String, W> remoteLoadMap
	) throws RemoteException {
		super(
			rtConfig, ioConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			remoteLoadMap
		);
	}
	//
	@Override
	protected IOTask<C> getIOTask(final C item, final String nextNodeAddr) {
		return new BasicDirectoryIOTask<>(item, (IOConfig<T, C>) ioConfigCopy);
	}
}
