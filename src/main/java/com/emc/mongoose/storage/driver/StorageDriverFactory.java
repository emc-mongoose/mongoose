package com.emc.mongoose.storage.driver;

import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.item.Item;
import com.github.akurilov.confuse.Config;

public interface StorageDriverFactory<
	I extends Item, O extends IoTask<I>, T extends StorageDriver<I, O>
> {

	String getName();

	T create(
		final String stepId, final DataInput dataInput, final Config loadConfig,
		final Config storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException, InterruptedException;
}
