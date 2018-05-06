package com.emc.mongoose.storage.driver.mock;

import com.emc.mongoose.model.exception.OmgShootMyFootException;
import com.emc.mongoose.model.data.DataInput;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.storage.driver.StorageDriverFactory;
import com.emc.mongoose.config.load.LoadConfig;
import com.emc.mongoose.config.storage.StorageConfig;

/**
 Created by andrey on 19.09.17.
 */
public final class DummyStorageDriverMockFactory<
	I extends Item, O extends IoTask<I>, T extends DummyStorageDriverMock<I, O>
>
implements StorageDriverFactory<I, O, T> {

	@Override
	public final String getName() {
		return "dummy-mock";
	}

	@Override @SuppressWarnings("unchecked")
	public final T create(
		final String stepId, final DataInput dataInput, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException {
		return (T) new DummyStorageDriverMock<>(
			stepId, dataInput, loadConfig, storageConfig, verifyFlag
		);
	}
}
