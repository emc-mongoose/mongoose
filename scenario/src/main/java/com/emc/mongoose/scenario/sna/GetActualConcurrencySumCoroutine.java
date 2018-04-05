package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.ui.log.LogUtil;

import com.github.akurilov.concurrent.coroutines.CoroutinesExecutor;
import com.github.akurilov.concurrent.coroutines.ExclusiveCoroutineBase;

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

	private final List<? extends StepService> stepSvcs;
	private final LongAdder tmpSum = new LongAdder();

	private volatile Iterator<? extends StepService> it;
	private volatile int lastValue = 0;

	public GetActualConcurrencySumCoroutine(
		final CoroutinesExecutor executor, final List<? extends StepService> stepSvcs
	) {
		super(executor);
		this.stepSvcs = stepSvcs;
		this.it = stepSvcs.iterator();
	}

	@Override
	protected final void invokeTimedExclusively(final long startTimeNanos) {
		try {
			if(!it.hasNext()) {
				lastValue = (int) tmpSum.sumThenReset();
				it = stepSvcs.iterator();
			}
			tmpSum.add(it.next().actualConcurrency());
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
	}
}
