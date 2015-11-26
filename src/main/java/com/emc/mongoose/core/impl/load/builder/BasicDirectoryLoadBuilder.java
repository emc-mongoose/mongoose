package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.io.req.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.DirectoryLoadExecutor;
//
import com.emc.mongoose.core.impl.io.req.BasicFileIOConfig;
import com.emc.mongoose.core.impl.load.executor.BasicDirectoryLoadExecutor;
//
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
/**
 Created by kurila on 26.11.15.
 */
public class BasicDirectoryLoadBuilder<
	T extends FileItem,
	C extends Directory<T>,
	U extends DirectoryLoadExecutor<T, C>
> extends ContainerLoadBuilderBase<T, C, U> {
	//
	public BasicDirectoryLoadBuilder(final RunTimeConfig rtConfig) {
		super(rtConfig);
	}
	//
	@Override
	protected IOConfig<T, C> getDefaultRequestConfig() {
		return new BasicFileIOConfig<>();
	}
	//
	@Override
	public void invokePreConditions()
	throws IllegalStateException {
		// check the filesystem
		final File parentPath = Paths.get("").toFile();
		if(!parentPath.isDirectory()) {
			throw new IllegalStateException(
				"\"" + parentPath.getAbsolutePath() + "\" is not a directory"
			);
		}
		final IOTask.Type loadType = ioConfig.getLoadType();
		switch(loadType) {
			case CREATE:
			case DELETE:
				if(!parentPath.canWrite()) {
					throw new IllegalStateException(
						"Parent directory \"" + parentPath.getAbsolutePath() + " is not writable"
					);
				}
				break;
			case READ:
				if(!parentPath.canRead()) {
					throw new IllegalStateException(
						"Parent directory \"" + parentPath.getAbsolutePath() + " is not readable"
					);
				}
				break;
			case UPDATE:
			case APPEND:
				throw new IllegalStateException(
					loadType.name() + " operation is not supported for directory items"
				);
		}
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
	@Override
	protected U buildActually() {
		final IOTask.Type loadType = ioConfig.getLoadType();
		final int threadCount = loadTypeConnPerNode.get(loadType);
		return (U) new BasicDirectoryLoadExecutor<>(
			RunTimeConfig.getContext(), (IOConfig<T, C>) ioConfig, null, 0, threadCount,
			itemSrc == null ? getDefaultItemSource() : itemSrc,
			maxCount, manualTaskSleepMicroSecs, rateLimit
		);
	}
}
