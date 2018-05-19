package com.emc.mongoose.storage.driver.coop.net.http.s3;

import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.storage.driver.StorageDriverFactory;

import com.github.akurilov.confuse.Config;

public class AmzS3StorageDriverFactory<
	I extends Item, O extends IoTask<I>, T extends AmzS3StorageDriver<I,O>
>
implements StorageDriverFactory<I, O, T> {

	private static final String NAME = "s3";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public T create(
		final String stepId, final DataInput dataInput, final Config loadConfig,
		final Config storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException, InterruptedException {
		return (T) new AmzS3StorageDriver<>(
			stepId, dataInput, loadConfig, storageConfig, verifyFlag
		);
	}
}
