package com.emc.mongoose.storage.driver.nio.fs;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.storage.driver.base.StorageDriverFactory;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;

public class FileStorageDriverFactory<
	I extends DataItem, O extends DataIoTask<I>, T extends BasicFileStorageDriver<I, O>
>
implements StorageDriverFactory<I, O, T> {

	private static final String NAME = "fs";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public T create(
		final String stepId, final DataInput contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException, InterruptedException {
		return (T) new BasicFileStorageDriver<>(
			stepId, contentSrc, loadConfig, storageConfig, verifyFlag
		);
	}
}

