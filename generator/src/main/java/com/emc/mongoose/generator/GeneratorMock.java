package com.emc.mongoose.generator;

import com.emc.mongoose.common.concurrent.LifeCycleBase;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.IoTask;
import com.emc.mongoose.common.item.Item;
import com.emc.mongoose.common.load.Driver;
import com.emc.mongoose.common.load.Generator;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 11.07.16.
 */
public class GeneratorMock<I extends Item, O extends IoTask<I>>
extends LifeCycleBase
implements Generator<I, O> {

	private final List<Driver<I, O>> drivers;
	private final Input<I> input;

	public GeneratorMock(final List<Driver<I, O>> drivers, final Input<I> input) {
		this.drivers = drivers;
		this.input = input;
	}

	@Override
	protected void doStart()
	throws Exception {
	}

	@Override
	protected void doShutdown()
	throws Exception {
	}

	@Override
	protected void doInterrupt()
	throws Exception {
	}

	@Override
	public void start()
	throws IllegalStateException {
	}

	@Override
	public void shutdown()
	throws IllegalStateException {
	}

	@Override
	public boolean await()
	throws InterruptedException {
		return false;
	}

	@Override
	public boolean await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException {
		return false;
	}

	@Override
	public void interrupt() {
	}
}
