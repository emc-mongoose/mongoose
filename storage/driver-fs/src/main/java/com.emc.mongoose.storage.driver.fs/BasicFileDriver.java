package com.emc.mongoose.storage.driver.fs;

import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.util.LoadType;

import java.nio.file.OpenOption;
import java.util.HashSet;
import java.util.Set;

import static com.emc.mongoose.ui.config.Config.IoConfig.BufferConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;

/**
 Created by kurila on 19.07.16.
 */
public class BasicFileDriver<I extends DataItem, O extends DataIoTask<I>>
extends FsDriverBase<I, O> {
	
	public BasicFileDriver(final LoadConfig loadConfig, final BufferConfig ioBufferConfig) {
		super(loadConfig, ioBufferConfig);
	}

	@Override
	protected void executeIoTask(final O ioTask) {
		final LoadType ioType = ioTask.getLoadType();
		final I fileItem = ioTask.getItem();
		final String dstDir, srcDir;
		final Set<OpenOption> openOptions = new HashSet<>();
		switch(ioType) {
			case CREATE:
				createFile(fileItem, srcDir, dstDir, openOptions);
				break;
			case READ:
				readFile(fileItem, srcDir, dstDir, openOptions);
				break;
			case UPDATE:
				updateFile(fileItem, srcDir, dstDir, openOptions);
				break;
			case DELETE:
				deleteFile(fileItem, srcDir, dstDir, openOptions);
				break;
		}
	}
}
