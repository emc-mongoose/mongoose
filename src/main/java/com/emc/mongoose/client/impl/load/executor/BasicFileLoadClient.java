package com.emc.mongoose.client.impl.load.executor;
//
import com.emc.mongoose.client.api.load.executor.FileLoadClient;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
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
		final AppConfig appConfig, final FileIOConfig<T, ? extends Directory<T>> ioConfig,
		final String[] addrs, final int threadCount, final ItemSrc<T> itemSrc,
		final long maxCount, final float rateLimit, final Map<String, W> remoteLoadMap
	) throws RemoteException {
		super(appConfig, ioConfig, addrs, threadCount, itemSrc, maxCount, rateLimit, remoteLoadMap);
	}
	//
	@Override
	protected IOTask<T> getIOTask(final T item, final String nextNodeAddr) {
		return new BasicFileIOTask<>(
			item, (FileIOConfig<T, ? extends Directory<T>>) ioConfigCopy
		);
	}
}
