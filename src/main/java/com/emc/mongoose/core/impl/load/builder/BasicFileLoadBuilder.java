package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.io.req.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.FileLoadExecutor;
//
import com.emc.mongoose.core.impl.io.req.BasicFileIOConfig;
import com.emc.mongoose.core.impl.load.executor.BasicFileLoadExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
/**
 Created by kurila on 26.11.15.
 */
public class BasicFileLoadBuilder<T extends FileItem, U extends FileLoadExecutor<T>>
extends DataLoadBuilderBase<T, U> {
	//
	public BasicFileLoadBuilder(final RunTimeConfig rtConfig) {
		super(rtConfig);
	}
	//
	@Override
	protected IOConfig<T, ? extends Directory<T>> getDefaultRequestConfig() {
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
			case UPDATE:
			case APPEND:
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
	@Override @SuppressWarnings("unchecked")
	protected U buildActually() {
		if(minObjSize > maxObjSize) {
			throw new IllegalStateException(
				String.format(
					"Min object size (%s) shouldn't be more than max (%s)",
					SizeUtil.formatSize(minObjSize), SizeUtil.formatSize(maxObjSize)
				)
			);
		}
		final IOTask.Type loadType = ioConfig.getLoadType();
		final int threadCount = loadTypeConnPerNode.get(loadType);
		return (U) new BasicFileLoadExecutor<>(
			RunTimeConfig.getContext(), ioConfig, null, 0, threadCount,
			itemSrc == null ? getDefaultItemSource() : itemSrc,
			maxCount, minObjSize, maxObjSize, objSizeBias,
			manualTaskSleepMicroSecs, rateLimit, updatesPerItem
		);
	}
}
