package com.emc.mongoose.storage.driver.nio.mock;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.storage.driver.base.StorageDriverFactory;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;

public final class NioStorageDriverMockFactory<
	I extends Item, O extends IoTask<I>, T extends NioStorageDriverMock<I, O>
>
implements StorageDriverFactory<I, O, T> {

	@Override
	public final String getName() {
		return "nio-mock";
	}

	@Override @SuppressWarnings("unchecked")
	public T create(
		final String stepId, final DataInput dataInput, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException, InterruptedException {
		return (T) new NioStorageDriverMock<I, O>(
			stepId, dataInput, loadConfig, storageConfig, verifyFlag
		);
	}
}
