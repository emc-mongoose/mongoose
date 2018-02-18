package com.emc.mongoose.load.controller;

import com.github.akurilov.coroutines.CoroutinesProcessor;
import com.github.akurilov.coroutines.ExclusiveCoroutineBase;

import com.emc.mongoose.api.model.storage.StorageDriver;

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
		lastValue = storageDriver.getActiveTaskCount();
	}

	public int getActualConcurrencySum() {
		return lastValue;
	}

	@Override
	protected final void doClose() {
	}
}
