package com.emc.mongoose.storage.driver.nio.mock;

import com.emc.mongoose.model.exception.OmgShootMyFootException;
import com.emc.mongoose.model.data.DataInput;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.storage.driver.StorageDriverFactory;

import com.github.akurilov.confuse.Config;

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
		final String stepId, final DataInput dataInput, final Config loadConfig,
		final Config storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException, InterruptedException {
		return (T) new NioStorageDriverMock<I, O>(
			stepId, dataInput, loadConfig, storageConfig, verifyFlag
		);
	}
}
