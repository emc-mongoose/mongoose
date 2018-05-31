package com.emc.mongoose.storage.driver.mock;

import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.env.ExtensionBase;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.storage.driver.StorageDriverFactory;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 Created by andrey on 19.09.17.
 */
public final class DummyStorageDriverMockFactory<
	I extends Item, O extends IoTask<I>, T extends DummyStorageDriverMock<I, O>
>
extends ExtensionBase
implements StorageDriverFactory<I, O, T> {

	private static final List<String> RES_INSTALL_FILES = Collections.unmodifiableList(
		Arrays.asList(
		)
	);

	@Override
	public final String id() {
		return "dummy-mock";
	}

	@Override @SuppressWarnings("unchecked")
	public final T create(
		final String stepId, final DataInput dataInput, final Config loadConfig,
		final Config storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException {
		return (T) new DummyStorageDriverMock<>(
			stepId, dataInput, loadConfig, storageConfig, verifyFlag
		);
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
