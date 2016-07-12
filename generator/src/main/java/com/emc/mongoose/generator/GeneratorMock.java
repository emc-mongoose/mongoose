package com.emc.mongoose.generator;

import com.emc.mongoose.common.concurrent.LifeCycleBase;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.IoTask;
import com.emc.mongoose.common.item.Item;
import com.emc.mongoose.common.load.Driver;
import com.emc.mongoose.common.load.Generator;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 11.07.16.
 */
public class GeneratorMock<I extends Item, O extends IoTask<I>>
extends LifeCycleBase
implements Generator<I, O> {

	private final static Logger LOG = LogManager.getLogger();

	private final List<Driver<I, O>> drivers;
	private final Input<I> itemInput;
	private final Thread worker;
	//private final int batchSize;
	//private final boolean isShuffling;

	private long producedItemsCount = 0;

	private final class GeneratorTask
	implements Runnable {

		@Override
		public final void run() {
			if(itemInput == null) {
				LOG.debug(Markers.MSG, "No item source for the producing, exiting");
				return;
			}
			int n = 0, m = 0;
			/*try {
				List<I> buff;
				while(!state.get().equals(State.INTERRUPTED)) {
					try {
						buff = new ArrayList<>(batchSize);
						n = (int) Math.min(itemInput.get(buff, batchSize), countLimit - producedItemsCount);
						if(isShuffling) {
							Collections.shuffle(buff, rnd);
						}
						if(isInterrupted) {
							break;
						}
						if(n > 0 && rateThrottle.requestContinueFor(null, n)) {
							for(m = 0; m < n && !isInterrupted; ) {
								m += itemOutput.put(buff, m, n);
								LockSupport.parkNanos(1);
							}
							producedItemsCount += n;
						} else {
							if(isInterrupted) {
								break;
							}
						}
						// CIRCULARITY FEATURE:
						// produce only <maxItemQueueSize> items in order to make it possible to enqueue
						// them infinitely
						if(isCircular && producedItemsCount >= maxItemQueueSize) {
							break;
						}
					} catch(
						final EOFException | InterruptedException | ClosedByInterruptException |
							IllegalStateException e
						) {
						break;
					} catch(final IOException e) {
						LogUtil.exception(
							LOG, Level.DEBUG, e, "Failed to transfer the data items, count = {}, " +
								"batch size = {}, batch offset = {}", producedItemsCount, n, m
						);
					}
				}
			} finally {
				LOG.debug(
					Markers.MSG, "{}: produced {} items from \"{}\" for the \"{}\"",
					getName(), producedItemsCount, itemInput, itemOutput
				);
				try {
					itemInput.close();
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to close the item source \"{}\"", itemInput
					);
				}
			}*/
		}
	}

	public GeneratorMock(final List<Driver<I, O>> drivers, final Input<I> itemInput) {
		this.drivers = drivers;
		this.itemInput = itemInput;
		worker = new Thread(new GeneratorTask(), "generator");
		worker.setDaemon(true);
	}

	@Override
	protected void doStart() {
		for(final Driver<I, O> nextDriver : drivers) {
			nextDriver.start();
		}
		worker.start();
	}

	@Override
	protected void doShutdown() {
		doInterrupt();
	}

	@Override
	protected void doInterrupt() {
		worker.interrupt();
		for(final Driver<I, O> nextDriver : drivers) {
			nextDriver.shutdown();
		}
	}

	@Override
	public boolean await()
	throws InterruptedException {
		worker.join();
		return true;
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		timeUnit.timedJoin(worker, timeout);
		return true;
	}
}
