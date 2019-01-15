package com.emc.mongoose.storage.driver;

import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.Item;
import com.github.akurilov.confuse.Config;

public interface StorageDriverFactory<
	I extends Item, O extends Operation<I>, T extends StorageDriver<I, O>
>
extends Extension {

	T create(
		final String stepId, final DataInput dataInput, final Config storageConfig, final boolean verifyFlag,
		final int batchSize
	) throws OmgShootMyFootException, InterruptedException;
}
