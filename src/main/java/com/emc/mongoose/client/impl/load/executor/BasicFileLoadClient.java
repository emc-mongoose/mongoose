package com.emc.mongoose.client.impl.load.executor;
//
import com.emc.mongoose.client.api.load.executor.FileLoadClient;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.req.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
//
import com.emc.mongoose.core.impl.io.task.BasicFileIOTask;
//
import com.emc.mongoose.server.api.load.executor.FileLoadSvc;
//
import java.rmi.RemoteException;
import java.util.Map;
/**
 Created by kurila on 26.11.15.
 */
public class BasicFileLoadClient<T extends FileItem, W extends FileLoadSvc<T>>
extends LoadClientBase<T, W>
implements FileLoadClient<T, W> {
	//
	public BasicFileLoadClient(
		final RunTimeConfig rtConfig, final IOConfig<T, ? extends Directory<T>> ioConfig,
		final String[] addrs, final int connCountPerNode, final int threadCount,
		final ItemSrc<T> itemSrc, final long maxCount, final Map<String, W> remoteLoadMap
	) throws RemoteException {
		super(
			rtConfig, ioConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			remoteLoadMap
		);
	}
	//
	@Override
	protected IOTask<T> getIOTask(final T item, final String nextNodeAddr) {
		return new BasicFileIOTask<>(
			item, (IOConfig<T, ? extends Directory<T>>) ioConfigCopy
		);
	}
}
