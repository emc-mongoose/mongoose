package com.emc.mongoose.load.generator;

import com.emc.mongoose.common.concurrent.DaemonBase;
import com.emc.mongoose.common.concurrent.Throttle;
import com.emc.mongoose.model.api.io.Output;
import com.emc.mongoose.model.impl.item.CsvFileItemInput;
import com.emc.mongoose.model.api.ItemNamingType;
import com.emc.mongoose.model.api.LoadType;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import static com.emc.mongoose.ui.config.Config.ItemConfig.NamingConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;

import com.emc.mongoose.model.api.data.DataRangesConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.io.task.IoTaskBuilder;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.ItemFactory;
import com.emc.mongoose.model.api.storage.StorageDriver;
import com.emc.mongoose.model.api.load.LoadGenerator;
import com.emc.mongoose.model.impl.io.RangePatternDefinedInput;
import com.emc.mongoose.model.impl.item.BasicMutableDataItemFactory;
import com.emc.mongoose.model.impl.item.BasicItemNameInput;
import com.emc.mongoose.model.impl.item.NewDataItemInput;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 Created by kurila on 11.07.16.
 */
public class BasicLoadGenerator<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements LoadGenerator<I, O>, Output<I> {

	private final static Logger LOG = LogManager.getLogger();
	private final static int BATCH_SIZE = 0x1000;

	private final List<StorageDriver<I, O>> drivers;
	private final LoadType ioType;
	private final Input<I> itemInput;
	private final Thread worker;
	private final long countLimit;
	private final int maxItemQueueSize;
	private final boolean isShuffling = false;
	private final boolean isCircular;
	private final Throttle<I> rateThrottle;
	private final IoTaskBuilder<I, O> ioTaskBuilder;
	private final String dstContainer;
	private final String runId;
	private final DataRangesConfig rangesConfig;

	private long producedItemsCount = 0;

	public BasicLoadGenerator(
		final String runId, final List<StorageDriver<I, O>> drivers,
		final ItemFactory<I> itemFactory, final IoTaskBuilder<I, O> ioTaskBuilder,
		final ItemConfig itemConfig, final LoadConfig loadConfig
	) throws UserShootHisFootException {

		this.runId = runId;
		this.drivers = drivers;
		final LimitConfig limitConfig = loadConfig.getLimitConfig();

		try {
			final long l = limitConfig.getCount();
			this.countLimit = l > 0 ? l : Long.MAX_VALUE;
			this.maxItemQueueSize = loadConfig.getQueueConfig().getSize();
			this.isCircular = loadConfig.getCircular();
			this.rateThrottle = new RateThrottle<>(limitConfig.getRate());
			final String t = itemConfig.getOutputConfig().getContainer();
			if(t == null || t.isEmpty()) {
				dstContainer = runId;
			} else {
				dstContainer = t;
			}
			final Input<String> pathInput = new RangePatternDefinedInput(dstContainer);

			final NamingConfig namingConfig = itemConfig.getNamingConfig();
			final ItemNamingType namingType = ItemNamingType.valueOf(
				namingConfig.getType().toUpperCase()
			);
			final String namingPrefix = namingConfig.getPrefix();
			final int namingLength = namingConfig.getLength();
			final int namingRadix = namingConfig.getRadix();
			final long namingOffset = namingConfig.getOffset();
			final BasicItemNameInput namingInput = new BasicItemNameInput(
				namingType, namingPrefix, namingLength, namingRadix, namingOffset
			);
			rangesConfig = itemConfig.getDataConfig().getRanges();

			this.ioType = LoadType.valueOf(loadConfig.getType().toUpperCase());
			switch(ioType) {
				case CREATE:
					// TODO copy mode
					if(itemFactory instanceof BasicMutableDataItemFactory) {
						this.itemInput = new NewDataItemInput(
							itemFactory, pathInput, namingInput,
							itemConfig.getDataConfig().getSize()
						);
					} else {
						this.itemInput = null; // TODO
					}
					break;
				case READ:
				case UPDATE:
				case DELETE:
					final String itemInputFile = itemConfig.getInputConfig().getFile();
					if(itemInputFile != null && !itemInputFile.isEmpty()) {
						this.itemInput = new CsvFileItemInput<>(
							Paths.get(itemInputFile), itemFactory
						);
					} else {
						// TODO use container input
						this.itemInput = null;
					}
					break;
				default:
					throw new UserShootHisFootException();
			}
		} catch(final Exception e) {
			throw new UserShootHisFootException(e);
		}

		this.ioTaskBuilder = ioTaskBuilder;

		worker = new Thread(new GeneratorTask(), "generator");
		worker.setDaemon(true);
	}

	private final class GeneratorTask
		implements Runnable {

		private final Random rnd = new Random();

		@Override
		public final void run() {
			if(itemInput == null) {
				LOG.debug(Markers.MSG, "No item source for the producing, exiting");
				return;
			}
			int n = 0, m = 0;
			long remaining;
			try {
				List<I> buff;
				while(!isInterrupted()) {
					remaining = countLimit - producedItemsCount;
					if(remaining <= 0) {
						break;
					}
					try {
						buff = new ArrayList<>(BATCH_SIZE);
						n = itemInput.get(buff, BATCH_SIZE);
						if(n > remaining) {
							n = (int) remaining;
						}
						if(isShuffling) {
							Collections.shuffle(buff, rnd);
						}
						if(isInterrupted()) {
							break;
						}
						if(n > 0 && rateThrottle.waitPassFor(null, n)) {
							for(m = 0; m < n && !isInterrupted(); ) {
								m += put(buff, m, n);
							}
							producedItemsCount += n;
						} else {
							if(isInterrupted()) {
								break;
							}
						}
						// CIRCULARITY FEATURE:
						// produce only <maxItemQueueSize> items in order to make it possible to
						// enqueue them infinitely
						if(isCircular && producedItemsCount >= maxItemQueueSize) {
							break;
						}
					} catch(
						final EOFException | InterruptedException | ClosedByInterruptException |
							IllegalStateException | InterruptedIOException e
						) {
						break;
					} catch(final Exception e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to read the data items, count = {}, " +
							"batch size = {}, batch offset = {}", producedItemsCount, n, m
						);
					}
				}
			} finally {
				LOG.debug(
					Markers.MSG, "{}: produced {} items from \"{}\" for the \"{}\"",
					Thread.currentThread().getName(), producedItemsCount, itemInput.toString(), this
				);
				try {
					itemInput.close();
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to close the item source \"{}\"",
						itemInput.toString()
					);
				}
				try {
					shutdown();
				} catch(final IllegalStateException ignored) {
				}
			}
		}
	}

	@Override
	public final void put(final I item)
	throws IOException {
		final O nextIoTask = ioTaskBuilder.getInstance(item, dstContainer);
		final StorageDriver<I, O> nextDriver = getNextDriver();
		nextDriver.put(nextIoTask);
	}
	
	@Override
	public final int put(final List<I> buffer, final int from, final int to)
	throws IOException {
		if(to > from) {
			final List<O> ioTasks = new ArrayList<>(to - from);
			for(int i = from; i < to; i ++) {
				ioTasks.add(ioTaskBuilder.getInstance(buffer.get(i), dstContainer));
			}
			final StorageDriver<I, O> nextDriver = getNextDriver();
			nextDriver.put(ioTasks, 0, ioTasks.size());
		}
		return to - from;
	}
	
	@Override
	public final int put(final List<I> buffer)
	throws IOException {
		final int n = buffer.size();
		final List<O> ioTasks = new ArrayList<>(n);
		for(final I nextItem : buffer) {
			ioTasks.add(ioTaskBuilder.getInstance(nextItem, dstContainer));
		}
		final StorageDriver<I, O> nextDriver = getNextDriver();
		nextDriver.put(ioTasks, 0, n);
		return n;
	}
	
	@Override
	public final Input<I> getInput()
	throws IOException {
		return itemInput;
	}

	private final AtomicLong rrc = new AtomicLong(0);
	protected StorageDriver<I, O> getNextDriver() {
		if(drivers.isEmpty()) {
			return null;
		} else {
			return drivers.get((int)(rrc.incrementAndGet() % drivers.size()));
		}
	}

	@Override
	protected void doStart()
	throws IllegalStateException {
		worker.start();
	}

	@Override
	protected void doShutdown() {
		interrupt();
	}

	@Override
	protected void doInterrupt() {
		worker.interrupt();
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		timeUnit.timedJoin(worker, timeout);
		return true;
	}

	@Override
	protected void doClose()
	throws IOException {
		if(itemInput != null) {
			itemInput.close();
		}
	}
}
