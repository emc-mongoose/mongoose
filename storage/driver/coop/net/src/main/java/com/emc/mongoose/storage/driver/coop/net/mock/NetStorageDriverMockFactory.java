package com.emc.mongoose.storage.driver.coop.net.mock;

import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.env.ExtensionBase;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.storage.driver.StorageDriverFactory;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NetStorageDriverMockFactory<I extends Item, O extends Operation<I>, T extends NetStorageDriverMock<I, O>>
extends ExtensionBase
implements StorageDriverFactory<I, O, T> {

	private static final List<String> RES_INSTALL_FILES = Collections.unmodifiableList(
		Arrays.asList(
		)
	);

	@Override
	public T create(
		final String stepId, final DataInput dataInput, final Config storageConfig, final boolean verifyFlag,
		final int batchSize
	) throws OmgShootMyFootException, InterruptedException {
		return (T) new NetStorageDriverMock<I, O>(stepId, dataInput, storageConfig, verifyFlag, batchSize);
	}

	@Override
	public final String id() {
		return "net-mock";
	}

	@Override
	public SchemaProvider schemaProvider() {
		return null;
	}

	@Override
	protected String defaultsFileName() {
		return null;
	}

	@Override
	protected List<String> resourceFilesToInstall() {
		return RES_INSTALL_FILES;
	}
}
