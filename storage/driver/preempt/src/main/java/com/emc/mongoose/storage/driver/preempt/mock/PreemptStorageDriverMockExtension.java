package com.emc.mongoose.storage.driver.preempt.mock;

import com.emc.mongoose.env.ExtensionBase;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.storage.driver.StorageDriverFactory;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PreemptStorageDriverMockExtension<
	I extends Item, O extends Operation<I>, T extends PreemptStorageDriverMock<I, O>
>
extends ExtensionBase
implements StorageDriverFactory<I, O, T> {

	private static final List<String> RES_INSTALL_FILES = Collections.unmodifiableList(
		Arrays.asList(
		)
	);

	@Override
	public String id() {
		return "preempt-mock";
	}

	@Override @SuppressWarnings("unchecked")
	public T create(
		final String stepId, final DataInput dataInput, final Config storageConfig, final boolean verifyFlag,
		final int batchSize
	) throws OmgShootMyFootException, InterruptedException {
		return (T) new PreemptStorageDriverMock<>(stepId, dataInput, storageConfig, verifyFlag);
	}

	@Override
	public final SchemaProvider schemaProvider() {
		return null;
	}

	@Override
	protected final String defaultsFileName() {
		return null;
	}

	@Override
	protected final List<String> resourceFilesToInstall() {
		return RES_INSTALL_FILES;
	}
}
