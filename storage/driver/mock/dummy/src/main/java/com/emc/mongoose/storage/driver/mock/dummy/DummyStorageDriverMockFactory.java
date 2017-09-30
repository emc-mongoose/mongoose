package com.emc.mongoose.storage.driver.mock.dummy;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.storage.driver.base.StorageDriverFactory;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;

/**
 Created by andrey on 19.09.17.
 */
public final class DummyStorageDriverMockFactory<
	I extends Item, O extends IoTask<I>, T extends DummyStorageDriverMock<I, O>
>
implements StorageDriverFactory<I, O, T> {

	@Override
	public final String getName() {
		return "mock-dummy";
	}

	@Override @SuppressWarnings("unchecked")
	public final T create(
		final String stepId, final DataInput contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException, InterruptedException {
		return (T) new DummyStorageDriverMock<>(
			stepId, contentSrc, loadConfig, storageConfig, verifyFlag
		);
	}
}
