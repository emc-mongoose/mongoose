package com.emc.mongoose.storage.driver.net.http.swift;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.storage.driver.base.StorageDriverFactory;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;

public class SwiftStorageDriverFactory<
	I extends Item, O extends IoTask<I>, T extends SwiftStorageDriver<I, O>
>
implements StorageDriverFactory<I, O, T> {

	private static final String NAME = "swift";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public T create(
		final String stepId, final DataInput contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException, InterruptedException {
		return (T) new SwiftStorageDriver<>(
			stepId, contentSrc, loadConfig, storageConfig, verifyFlag
		);
	}
}

