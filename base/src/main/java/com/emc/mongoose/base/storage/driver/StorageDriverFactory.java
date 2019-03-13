package com.emc.mongoose.base.storage.driver;

import com.emc.mongoose.base.data.DataInput;
import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.base.config.IllegalConfigurationException;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.github.akurilov.confuse.Config;

public interface StorageDriverFactory<I extends Item, O extends Operation<I>, T extends StorageDriver<I, O>>
				extends Extension {

	T create(
					final String stepId,
					final DataInput dataInput,
					final Config storageConfig,
					final boolean verifyFlag,
					final int batchSize)
					throws IllegalConfigurationException, InterruptedException;
}
