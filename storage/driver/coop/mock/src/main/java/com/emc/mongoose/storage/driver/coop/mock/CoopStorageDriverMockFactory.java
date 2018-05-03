package com.emc.mongoose.storage.driver.coop.mock;

import com.emc.mongoose.model.exception.OmgShootMyFootException;
import com.emc.mongoose.model.data.DataInput;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.storage.driver.base.StorageDriverFactory;
import com.emc.mongoose.config.load.LoadConfig;
import com.emc.mongoose.config.storage.StorageConfig;

public class CoopStorageDriverMockFactory<
	I extends Item, O extends IoTask<I>, T extends CoopStorageDriverMock
>
implements StorageDriverFactory  {

	@Override
	public final String getName() {
		return "coop-mock";
	}

	@Override @SuppressWarnings("unchecked")
	public T create(
		final String stepId, final DataInput dataInput, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException, InterruptedException {
		return (T) new CoopStorageDriverMock<I, O>(
			stepId, dataInput, loadConfig, storageConfig, verifyFlag
		);
	}
}
