package com.emc.mongoose.api.model.concurrent;

import com.emc.mongoose.api.common.concurrent.StoppableTaskBase;

import java.io.IOException;
import java.util.List;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.LongAdder;

/**
 Created by andrey on 26.07.17.
 */
public abstract class CoroutineBase
extends StoppableTaskBase
implements Coroutine {

	private final List<Coroutine> coroutineRegistry;
	//private final LongAdder invocationsCount = new LongAdder();
	//private final LongAdder invocationsDurationsSum = new LongAdder();

	protected CoroutineBase(final List<Coroutine> coroutineRegistry) {
		this.coroutineRegistry = coroutineRegistry;
	}

	@Override
	protected final void invoke() {
		//long t = System.nanoTime();
		invokeTimed();
		//t = System.nanoTime() - t;
		//invocationsCount.increment();
		//invocationsDurationsSum.add(t);
	}

	protected abstract void invokeTimed();

	@Override
	public final void close()
	throws IOException {
		coroutineRegistry.remove(this);
		super.close();
	}
}
