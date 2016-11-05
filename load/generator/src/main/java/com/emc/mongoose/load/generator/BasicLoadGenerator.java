package com.emc.mongoose.load.generator;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.concurrent.DaemonBase;
import com.emc.mongoose.model.io.Output;
import com.emc.mongoose.model.io.ConstantStringInput;
import com.emc.mongoose.model.item.CsvFileItemInput;
import com.emc.mongoose.model.item.ItemNamingType;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.model.load.LoadType;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import static com.emc.mongoose.ui.config.Config.ItemConfig.NamingConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.InputConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import com.emc.mongoose.model.io.Input;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.IoTaskBuilder;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.io.RangePatternDefinedInput;
import com.emc.mongoose.model.item.BasicMutableDataItemFactory;
import com.emc.mongoose.model.item.BasicItemNameInput;
import com.emc.mongoose.model.item.NewDataItemInput;

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

/**
 Created by kurila on 11.07.16.
 */
public class BasicLoadGenerator<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements LoadGenerator<I, O>, Output<I> {

	private static final Logger LOG = LogManager.getLogger();
	private static final int BATCH_SIZE = 0x1000;

	private volatile LoadMonitor<I, O> monitor;

	private final LoadType ioType;
	private final Input<I> itemInput;
	private final Input<String> dstPathInput;
	private final Thread worker;
	private final long countLimit;
	private final int maxItemQueueSize;
	private final boolean isShuffling = false;
	private final boolean isCircular;
	private final IoTaskBuilder<I, O> ioTaskBuilder;

	private long producedItemsCount = 0;

	@SuppressWarnings("unchecked")
	public BasicLoadGenerator(
		final ItemFactory<I> itemFactory, final IoTaskBuilder<I, O> ioTaskBuilder,
		final ItemConfig itemConfig, final LoadConfig loadConfig
	) throws UserShootHisFootException {

		final LimitConfig limitConfig = loadConfig.getLimitConfig();

		try {
			final long l = limitConfig.getCount();
			this.countLimit = l > 0 ? l : Long.MAX_VALUE;
			this.maxItemQueueSize = loadConfig.getQueueConfig().getSize();
			this.isCircular = loadConfig.getCircular();

			final NamingConfig namingConfig = itemConfig.getNamingConfig();
			final ItemNamingType namingType = ItemNamingType.valueOf(
				namingConfig.getType().toUpperCase()
			);
			final String namingPrefix = namingConfig.getPrefix();
			final int namingLength = namingConfig.getLength();
			final int namingRadix = namingConfig.getRadix();
			final long namingOffset = namingConfig.getOffset();
			final BasicItemNameInput itemNameInput = new BasicItemNameInput(
				namingType, namingPrefix, namingLength, namingRadix, namingOffset
			);

			final InputConfig inputConfig = itemConfig.getInputConfig();
			final String itemInputFile = inputConfig.getFile();
			final String itemInputPath = inputConfig.getPath();
			this.ioType = LoadType.valueOf(loadConfig.getType().toUpperCase());

			switch(ioType) {

				case CREATE:

					if(itemInputFile == null || itemInputFile.isEmpty()) {
						if(itemInputPath == null || itemInputPath.isEmpty()) {
							if(itemFactory instanceof BasicMutableDataItemFactory) {
								final SizeInBytes size = itemConfig.getDataConfig().getSize();
								this.itemInput = new NewDataItemInput(
									itemFactory, itemNameInput, size
								);
							} else {
								this.itemInput = null; // TODO
							}
						} else {
							// TODO path listing input
							this.itemInput = null;
						}
					} else {
						this.itemInput = new CsvFileItemInput<>(
							Paths.get(itemInputFile), itemFactory
						);
					}

					final String t = itemConfig.getOutputConfig().getPath();
					if(t == null || t.isEmpty()) {
						dstPathInput = new ConstantStringInput(LogUtil.getDateTimeStamp());
					} else {
						dstPathInput = new RangePatternDefinedInput(t);
					}

					break;

				case READ:
				case UPDATE:
				case DELETE:

					if(itemInputFile == null || itemInputFile.isEmpty()) {
						if(itemInputPath == null || itemInputPath.isEmpty()) {
							throw new UserShootHisFootException(
								"No input (file either path) is specified for non-create generator"
							);
						} else {
							// TODO path listing input
							this.itemInput = null;
						}
					} else {
						this.itemInput = new CsvFileItemInput<>(
							Paths.get(itemInputFile), itemFactory
						);
					}

					dstPathInput = null;

					break;

				default:
					throw new UserShootHisFootException();
			}
		} catch(final Exception e) {
			throw new UserShootHisFootException(e);
		}

		this.ioTaskBuilder = ioTaskBuilder;
		
		final String ioStr = ioType.toString();
		worker = new Thread(
			new GeneratorTask(),
			Character.toUpperCase(ioStr.charAt(0)) + ioStr.substring(1).toLowerCase() +
				(countLimit > 0 ? Long.toString(countLimit) : "")
		);
		worker.setDaemon(true);
	}

	@Override
	public final void setLoadMonitor(final LoadMonitor<I, O> monitor) {
		this.monitor = monitor;
	}

	private final class GeneratorTask
	implements Runnable {

		private final Random rnd = new Random();

		@Override
		public final void run() {

			if(monitor == null) {
				LOG.warn(Markers.ERR, "No load monitor set, exiting");
			}

			if(itemInput == null) {
				LOG.warn(Markers.MSG, "No item source for the producing, exiting");
				return;
			}

			int n = 0, m = 0;
			long remaining;
			try {
				List<I> buff;
				while(!worker.isInterrupted()) {
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
						if(worker.isInterrupted()) {
							break;
						}
						if(n > 0) {
							for(m = 0; m < n && !worker.isInterrupted(); ) {
								m += put(buff, m, n);
							}
							producedItemsCount += n;
						} else {
							if(worker.isInterrupted()) {
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
						final EOFException | ClosedByInterruptException | IllegalStateException |
							InterruptedIOException e
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
		final O nextIoTask = ioTaskBuilder.getInstance(item, dstPathInput.get());
		monitor.put(nextIoTask);
	}
	
	@Override
	public final int put(final List<I> buffer, final int from, final int to)
	throws IOException {
		final int n = to - from;
		if(n > 0) {
			final List<O> ioTasks;
			if(dstPathInput == null) {
				ioTasks = ioTaskBuilder.getInstances(buffer, from, to);
			} else if(dstPathInput instanceof ConstantStringInput) {
				final String dstPath = dstPathInput.get();
				ioTasks = ioTaskBuilder.getInstances(buffer, dstPath, from, to);
			} else {
				final List<String> dstPaths = new ArrayList<>(n);
				dstPathInput.get(dstPaths, n);
				ioTasks = ioTaskBuilder.getInstances(buffer, dstPaths, from, to);
			}
			return monitor.put(ioTasks, 0, ioTasks.size());
		} else {
			return 0;
		}
	}
	
	@Override
	public final int put(final List<I> buffer)
	throws IOException {
		return put(buffer, 0, buffer.size());
	}
	
	@Override
	public final Input<I> getInput()
	throws IOException {
		return itemInput;
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
	
	@Override
	public final String toString() {
		return worker.getName();
	}
}
