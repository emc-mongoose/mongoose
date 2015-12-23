package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.DirectoryLoadBuilder;
import com.emc.mongoose.core.api.load.executor.DirectoryLoadExecutor;
//
import com.emc.mongoose.core.impl.io.conf.BasicFileIOConfig;
import com.emc.mongoose.core.impl.load.executor.BasicDirectoryLoadExecutor;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
	public BasicDirectoryLoadBuilder(final RunTimeConfig rtConfig)
	throws RemoteException {
		super(rtConfig);
	}
	//
	@Override
	protected FileIOConfig<T, C> getDefaultIOConfig() {
		return new BasicFileIOConfig<>();
	}
	//
	@Override
	public void invokePreConditions()
	throws IllegalStateException {
		// create parent directories
		final String parentDirectories = ioConfig.getNamePrefix();
		if(parentDirectories != null && !parentDirectories.isEmpty()) {
			try {
				Files.createDirectories(Paths.get(parentDirectories));
			} catch(final IOException e) {
				throw new IllegalStateException(
					"Failed to create target directories @ \"" + parentDirectories + "\""
				);
			}
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected U buildActually() {
		final IOTask.Type loadType = ioConfig.getLoadType();
		final int threadCount = loadTypeConnPerNode.get(loadType);
		return (U) new BasicDirectoryLoadExecutor<>(
			RunTimeConfig.getContext(), (FileIOConfig<T, C>) ioConfig, null, 0, threadCount,
			itemSrc == null ? getDefaultItemSource() : itemSrc,
			maxCount, manualTaskSleepMicroSecs, rateLimit
		);
	}
}
