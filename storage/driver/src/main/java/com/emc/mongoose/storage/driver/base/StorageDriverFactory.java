package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;

public interface StorageDriverFactory<
	I extends Item, O extends IoTask<I>, T extends StorageDriver<I, O>
> {

	String getName();

	T create(
		final String stepId, final DataInput contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws UserShootHisFootException, InterruptedException;
}
