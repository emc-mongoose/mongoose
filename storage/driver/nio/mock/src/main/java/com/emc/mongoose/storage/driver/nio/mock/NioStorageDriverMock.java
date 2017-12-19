package com.emc.mongoose.storage.driver.nio.mock;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.storage.Credential;
import com.emc.mongoose.storage.driver.nio.base.NioStorageDriverBase;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.github.akurilov.commons.math.Random;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;

public class NioStorageDriverMock<I extends Item, O extends IoTask<I>>
extends NioStorageDriverBase<I, O> {

	private final Random rnd = new Random();

	public NioStorageDriverMock(
		final String testSteoName, final DataInput dataInput, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException {
		super(testSteoName, dataInput, loadConfig, storageConfig, verifyFlag);
	}

	@Override
	protected void invokeNio(final O ioTask) {

	}

	@Override
	protected String requestNewPath(final String path) {
		return path;
	}

	@Override
	protected String requestNewAuthToken(final Credential credential) {
		return Long.toHexString(rnd.nextLong());
	}

	@Override
	public List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix,
		final int idRadix, final I lastPrevItem, final int count
	) throws IOException {
		return null;
	}

	@Override
	public void adjustIoBuffers(
		final long avgTransferSize, final IoType ioType
	) throws RemoteException {
	}
}
