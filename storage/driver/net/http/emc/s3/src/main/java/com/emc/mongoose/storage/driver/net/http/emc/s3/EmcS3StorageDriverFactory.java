package com.emc.mongoose.storage.driver.net.http.emc.s3;

import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.storage.driver.base.StorageDriverFactory;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;

public class EmcS3StorageDriverFactory<
	I extends Item, O extends IoTask<I>, T extends EmcS3StorageDriver<I, O>
>
implements StorageDriverFactory<I, O, T> {

	private static final String NAME = "emcs3";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public T create(
		final String stepId, final DataInput contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws UserShootHisFootException, InterruptedException {
		return (T) new EmcS3StorageDriver<>(
			stepId, contentSrc, loadConfig, storageConfig, verifyFlag
		);
	}
}

