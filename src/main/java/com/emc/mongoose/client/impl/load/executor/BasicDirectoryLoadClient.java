package com.emc.mongoose.client.impl.load.executor;
//
import com.emc.mongoose.client.api.load.executor.DirectoryLoadClient;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.core.api.v1.item.container.Directory;
import com.emc.mongoose.core.api.v1.item.data.FileItem;
import com.emc.mongoose.core.api.v1.io.conf.FileIoConfig;
import com.emc.mongoose.core.api.v1.io.task.IoTask;
//
import com.emc.mongoose.core.impl.v1.io.task.BasicDirectoryIoTask;
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
		final AppConfig appConfig, final FileIoConfig<T, C> ioConfig, final int threadCount,
		final Input<C> itemInput, final long maxCount, final float rateLimit,
		final Map<String, W> remoteLoadMap
	) throws RemoteException {
		super(appConfig, ioConfig, null, threadCount, itemInput, maxCount, rateLimit, remoteLoadMap);
	}
	//
	@Override
	protected IoTask<C> getIOTask(final C item, final String nextNodeAddr) {
		return new BasicDirectoryIoTask<>(item, (FileIoConfig<T, C>) ioConfigCopy);
	}
}
