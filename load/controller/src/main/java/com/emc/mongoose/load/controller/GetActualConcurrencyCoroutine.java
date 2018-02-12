package com.emc.mongoose.load.controller;

import com.github.akurilov.coroutines.CoroutinesProcessor;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.ui.log.LogUtil;
import com.github.akurilov.coroutines.ExclusiveCoroutineBase;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 Created by andrey on 22.08.17.
 */
public class GetActualConcurrencyCoroutine
extends ExclusiveCoroutineBase {

	private final StorageDriver storageDriver;

	private volatile int lastValue = 0;

	public GetActualConcurrencyCoroutine(
		final CoroutinesProcessor coroutinesProcessor,
		final StorageDriver storageDriver
	) {
		super(coroutinesProcessor);
		this.storageDriver = storageDriver;
	}

	@Override
	protected final void invokeTimedExclusively(final long startTimeNanos) {
		try {
			lastValue = storageDriver.getActiveTaskCount();
		} catch(final RemoteException e) {
			LogUtil.exception(
				Level.DEBUG, e, "Failed to invoke the remote storage driver's method"
			);
		}
	}

	public int getActualConcurrencySum() {
		return lastValue;
	}

	@Override
	protected final void doClose()
	throws IOException {
	}
}
