package com.emc.mongoose.client.impl.load.executor;
//
import com.emc.mongoose.client.api.load.executor.FileLoadClient;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.io.Input;
//
import com.emc.mongoose.core.api.v1.item.container.Directory;
import com.emc.mongoose.core.api.v1.item.data.FileItem;
import com.emc.mongoose.core.api.v1.io.conf.FileIoConfig;
import com.emc.mongoose.core.api.v1.io.task.IoTask;
//
import com.emc.mongoose.core.impl.v1.io.task.BasicFileIoTask;
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
		final AppConfig appConfig, final FileIoConfig<T, ? extends Directory<T>> ioConfig,
		final int threadCount, final Input<T> itemInput, final long maxCount, final float rateLimit,
		final Map<String, W> remoteLoadMap
	) throws RemoteException {
		super(appConfig, ioConfig, null, threadCount, itemInput, maxCount, rateLimit, remoteLoadMap);
	}
	//
	protected BasicFileLoadClient(
		final AppConfig appConfig, final FileIoConfig<T, ? extends Directory<T>> ioConfig,
		final int threadCount, final Input<T> itemInput, final long maxCount, final float rateLimit,
		final Map<String, W> remoteLoadMap, final int instanceNum
	) throws RemoteException {
		super(
			appConfig, ioConfig, null, threadCount, itemInput, maxCount, rateLimit, remoteLoadMap,
			instanceNum
		);
	}
	//
	@Override
	protected IoTask<T> getIOTask(final T item, final String nextNodeAddr) {
		return new BasicFileIoTask<>(
			item, (FileIoConfig<T, ? extends Directory<T>>) ioConfigCopy
		);
	}
}
