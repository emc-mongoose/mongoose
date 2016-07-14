package com.emc.mongoose.monitor;

import com.emc.mongoose.common.concurrent.LifeCycleBase;
import com.emc.mongoose.common.io.IoTask;
import com.emc.mongoose.common.item.Item;
import com.emc.mongoose.common.load.Driver;
import com.emc.mongoose.common.load.Generator;
import com.emc.mongoose.common.load.Monitor;
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 Created by kurila on 12.07.16.
 */
public class MonitorMock<I extends Item, O extends IoTask<I>>
extends LifeCycleBase
implements Monitor<I, O> {

	private final static Logger LOG = LogManager.getLogger();

	private final List<Generator<I, O>> generators;
	private final ConcurrentMap<String, Driver<I, O>> drivers = new ConcurrentHashMap<>();

	public MonitorMock(final List<Generator<I, O>> generators) {
		this.generators = generators;
		for(final Generator<I, O> generator : generators) {
			generator.registerMonitor(this);
		}
	}

	private final AtomicLong taskCounter = new AtomicLong(0);

	@Override
	public void ioTaskCompleted(final O ioTask) {
		taskCounter.incrementAndGet();
	}

	@Override
	public int ioTaskCompletedBatch(final List<O> ioTasks, final int from, final int to) {
		taskCounter.addAndGet(to - from);
		return to - from;
	}

	@Override
	public final void registerDriver(final Driver<I, O> driver)
	throws IllegalStateException {
		if(null == drivers.putIfAbsent(driver.toString(), driver)) {
			LOG.info(
				Markers.MSG, "Monitor {}: driver {} registered", toString(), driver.toString()
			);
		} else {
			throw new IllegalStateException("Driver already registered");
		}
	}

	@Override
	protected void doStart() {
		for(final Generator<I, O> nextGenerator : generators) {
			nextGenerator.start();
		}
		final Thread t = new Thread(
			() -> {
				final Thread currentThread = Thread.currentThread();
				long lastTimeCount = 0;
				long lastTimeCountDelta;
				try {
					while(!currentThread.isInterrupted()) {
						lastTimeCountDelta = taskCounter.get() - lastTimeCount;
						lastTimeCount = taskCounter.get();
						System.out.println(
							"count = " + lastTimeCount + ", rate = " +
							(double) lastTimeCountDelta / 10 + " [op/s]"
						);
						TimeUnit.SECONDS.sleep(10);
					}
				} catch(final InterruptedException ignored) {
				}
			}
		);
		t.setDaemon(true);
		t.start();
	}

	@Override
	protected void doShutdown() {
		for(final Generator<I, O> nextGenerator : generators) {
			nextGenerator.shutdown();
		}
	}

	@Override
	protected void doInterrupt() {
		for(final Generator<I, O> nextGenerator : generators) {
			nextGenerator.interrupt();
		}
	}

	@Override
	public boolean await()
	throws InterruptedException {
		return await(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		boolean allDriversFinished = true;
		Driver<I, O> nextDriver;
		do {
			for(final String driverName : drivers.keySet()) {
				nextDriver = drivers.get(driverName);
				if(!nextDriver.isInterrupted()) {
					allDriversFinished = false;
					break;
				}
				Thread.yield();
			}
		} while(!allDriversFinished);
		return false;
	}

	@Override
	public void close()
	throws IOException {
		if(!isInterrupted()) {
			interrupt();
		}
		generators.clear();
		drivers.clear();
	}
}
