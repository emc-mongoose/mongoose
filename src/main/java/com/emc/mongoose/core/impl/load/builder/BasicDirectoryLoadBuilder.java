package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIoConfig;
import com.emc.mongoose.core.api.load.builder.DirectoryLoadBuilder;
import com.emc.mongoose.core.api.load.executor.DirectoryLoadExecutor;
//
import com.emc.mongoose.core.impl.io.conf.BasicFileIoConfig;
import com.emc.mongoose.core.impl.load.executor.BasicDirectoryLoadExecutor;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 26.11.15.
 */
public class BasicDirectoryLoadBuilder<
	T extends FileItem,
	C extends Directory<T>,
	U extends DirectoryLoadExecutor<T, C>
>
extends ContainerLoadBuilderBase<T, C, U>
implements DirectoryLoadBuilder<T, C, U> {
	//
	public BasicDirectoryLoadBuilder(final AppConfig appConfig)
	throws RemoteException {
		super(appConfig);
	}
	//
	@Override
	protected FileIoConfig<T, C> getDefaultIoConfig() {
		return new BasicFileIoConfig<>();
	}
	//
	@Override
	public void invokePreConditions()
	throws IllegalStateException {
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected U buildActually() {
		return (U) new BasicDirectoryLoadExecutor<>(
			appConfig, (FileIoConfig<T, C>) ioConfig, threadCount,
			itemInput == null ? getDefaultItemInput() : itemInput, maxCount, rateLimit
		);
	}
}
