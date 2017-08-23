package com.emc.mongoose.load.controller;

import com.github.akurilov.coroutines.CoroutinesProcessor;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.ui.log.LogUtil;
import com.github.akurilov.coroutines.ExclusiveCoroutineBase;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by andrey on 22.08.17.
 */
public class GetActualConcurrencySumCoroutine
extends ExclusiveCoroutineBase {

	private final List<StorageDriver> storageDrivers;
	private final LongAdder tmpSum = new LongAdder();

	private volatile Iterator<StorageDriver> it;
	private volatile int lastValue = 0;

	public GetActualConcurrencySumCoroutine(
		final CoroutinesProcessor coroutinesProcessor, final List<StorageDriver> storageDrivers
	) {
		super(coroutinesProcessor);
		this.storageDrivers = storageDrivers;
		this.it = storageDrivers.iterator();
	}

	@Override
	protected final void invokeTimedExclusively(final long startTimeNanos) {
		try {
			if(!it.hasNext()) {
				lastValue = (int) tmpSum.sumThenReset();
				it = storageDrivers.iterator();
			}
			tmpSum.add(it.next().getActiveTaskCount());
		} catch(final NoSuchElementException e) {
			LogUtil.exception(Level.DEBUG, e, "Storage driver list is empty");
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
		storageDrivers.clear();
	}
}
