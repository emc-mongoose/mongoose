package com.emc.mongoose.load.generator;

import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.io.ItemInputFactory;
import com.emc.mongoose.supply.BatchSupplier;
import com.emc.mongoose.supply.ConstantStringSupplier;
import com.emc.mongoose.supply.RangePatternDefinedSupplier;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.op.OperationsBuilder;
import com.emc.mongoose.item.op.data.DataOperationsBuilderImpl;
import com.emc.mongoose.item.op.data.DataOperationsBuilder;
import com.emc.mongoose.item.op.path.PathOperationsBuilderImpl;
import com.emc.mongoose.item.op.token.TokenOperationsBuilderImpl;
import com.emc.mongoose.item.DataItemFactoryImpl;
import com.emc.mongoose.item.DataItem;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.ItemFactory;
import com.emc.mongoose.item.ItemNameSupplier;
import com.emc.mongoose.item.ItemNamingType;
import com.emc.mongoose.item.ItemType;
import com.emc.mongoose.item.io.NewDataItemInput;
import com.emc.mongoose.item.io.NewItemInput;
import com.emc.mongoose.item.TransferConvertBuffer;
import com.emc.mongoose.storage.driver.StorageDriver;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.logging.LogContextThreadFactory;
import static com.emc.mongoose.Constants.M;
import static com.emc.mongoose.supply.PatternDefinedSupplier.PATTERN_CHAR;
import static com.emc.mongoose.item.DataItem.rangeCount;
import static com.emc.mongoose.storage.driver.StorageDriver.BUFF_SIZE_MIN;

import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.concurrent.throttle.IndexThrottle;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.commons.concurrent.throttle.Throttle;
import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.Level;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 Created by andrey on 12.11.16.
 */
public class LoadGeneratorBuilderImpl<
	I extends Item, O extends Operation<I>, T extends LoadGeneratorImpl<I, O>
>
implements LoadGeneratorBuilder<I, O, T> {

	private Config itemConfig = null;
	private Config loadConfig = null;
	private Config limitConfig = null;
	private ItemType itemType = null;
	private ItemFactory<I> itemFactory = null;
	private Config authConfig = null;
	private StorageDriver<I, O> storageDriver = null;
	private Input<I> itemInput = null;
	private long sizeEstimate = -1;
	private int batchSize = -1;
	private int originIndex = -1;
	private final List<Object> throttles = new ArrayList<>();
	
	@Override
	public LoadGeneratorBuilderImpl<I, O, T> itemConfig(final Config itemConfig) {
		this.itemConfig = itemConfig;
		return this;
	}

	@Override
	public LoadGeneratorBuilderImpl<I, O, T> loadConfig(final Config loadConfig) {
		this.loadConfig = loadConfig;
		this.batchSize = loadConfig.intVal("batch-size");
		return this;
	}

	@Override
	public LoadGeneratorBuilderImpl<I, O, T> limitConfig(final Config limitConfig) {
		this.limitConfig = limitConfig;
		return this;
	}

	@Override
	public LoadGeneratorBuilderImpl<I, O, T> itemType(final ItemType itemType) {
		this.itemType = itemType;
		return this;
	}

	@Override
	public LoadGeneratorBuilderImpl<I, O, T> itemFactory(final ItemFactory<I> itemFactory) {
		this.itemFactory = itemFactory;
		return this;
	}
	
	@Override
	public LoadGeneratorBuilderImpl<I, O, T> authConfig(final Config authConfig) {
		this.authConfig = authConfig;
		return this;
	}
	
	@Override
	public LoadGeneratorBuilderImpl<I, O, T> storageDriver(final StorageDriver<I, O> storageDriver) {
		this.storageDriver = storageDriver;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public LoadGeneratorBuilderImpl<I, O, T> itemInput(final Input<I> itemInput)
	throws InterruptRunException {
		this.itemInput = itemInput;
		// pipeline transfer buffer is not resettable
		if(!(itemInput instanceof TransferConvertBuffer)) {
			sizeEstimate = estimateTransferSize(
				null, OpType.valueOf(loadConfig.stringVal("op-type").toUpperCase()), (Input<DataItem>) itemInput
			);
		}
		return this;
	}

	@Override
	public LoadGeneratorBuilderImpl<I, O, T> originIndex(final int originIndex) {
		this.originIndex = originIndex;
		return this;
	}

	@Override
	public LoadGeneratorBuilderImpl<I, O, T> addThrottle(final Throttle throttle) {
		throttles.add(throttle);
		return this;
	}

	@Override
	public LoadGeneratorBuilderImpl<I, O, T> addThrottle(final IndexThrottle throttle) {
		throttles.add(throttle);
		return this;
	}

	@SuppressWarnings("unchecked")
	public T build()
	throws InterruptRunException, OmgShootMyFootException {

		// prepare
		final OperationsBuilder<I, O> opsBuilder;
		if(limitConfig == null) {
			throw new OmgShootMyFootException("Test step limit config is not set");
		}
		final long countLimit = limitConfig.longVal("count");
		if(loadConfig == null) {
			throw new OmgShootMyFootException("Load config is not set");
		}
		final Config opConfig = loadConfig.configVal("op");
		final boolean shuffleFlag = opConfig.boolVal("shuffle");
		if(itemConfig == null) {
			throw new OmgShootMyFootException("Item config is not set");
		}
		final Config inputConfig = itemConfig.configVal("input");
		final Config rangesConfig = itemConfig.configVal("data-ranges");

		if(itemType == null) {
			throw new OmgShootMyFootException("Item type is not set");
		}
		if(originIndex < 0) {
			throw new OmgShootMyFootException("No origin index is set");
		}
		// init the op builder
		if(ItemType.DATA.equals(itemType)) {
			final List<String> fixedRangesConfig = rangesConfig.listVal("fixed");
			final List<Range> fixedRanges;
			if(fixedRangesConfig == null) {
				fixedRanges = Collections.EMPTY_LIST;
			} else {
				fixedRanges = fixedRangesConfig
					.stream()
					.map(Range::new)
					.collect(Collectors.toList());
			}
			final long sizeThreshold;
			final Object sizeThresholdRaw = rangesConfig.val("threshold");
			if(sizeThresholdRaw instanceof String) {
				sizeThreshold = SizeInBytes.toFixedSize((String) sizeThresholdRaw);
			} else {
				sizeThreshold = TypeUtil.typeConvert(sizeThresholdRaw, long.class);
			}
			opsBuilder = (OperationsBuilder<I, O>) new DataOperationsBuilderImpl(originIndex)
				.fixedRanges(fixedRanges)
				.randomRangesCount(rangesConfig.intVal("random"))
				.sizeThreshold(sizeThreshold);
		} else if(ItemType.PATH.equals(itemType)){
			opsBuilder = (OperationsBuilder<I, O>) new PathOperationsBuilderImpl(originIndex);
		} else {
			opsBuilder = (OperationsBuilder<I, O>) new TokenOperationsBuilderImpl(originIndex);
		}

		// determine the operations type
		final OpType opType = OpType.valueOf(opConfig.stringVal("type").toUpperCase());
		opsBuilder.opType(opType);

		// determine the input path
		String itemInputPath = inputConfig.stringVal("path");
		if(itemInputPath != null && itemInputPath.indexOf('/') != 0) {
			itemInputPath = '/' + itemInputPath;
		}
		opsBuilder.inputPath(itemInputPath);

		// determine the output path
		final BatchSupplier<String> outputPathSupplier;
		if(OpType.CREATE.equals(opType) && ItemType.DATA.equals(itemType)) {
			outputPathSupplier = getOutputPathSupplier();
		} else {
			outputPathSupplier = null;
		}
		opsBuilder.outputPathSupplier(outputPathSupplier);

		// init the credentials, multi-user case support
		final BatchSupplier<String> uidSupplier;
		if(authConfig == null) {
			throw new OmgShootMyFootException("Storage auth config is not set");
		}
		final String uid = authConfig.stringVal("uid");
		if(uid == null) {
			uidSupplier = null;
		} else if(-1 != uid.indexOf(PATTERN_CHAR)) {
			uidSupplier = new RangePatternDefinedSupplier(uid);
		} else {
			uidSupplier = new ConstantStringSupplier(uid);
		}
		opsBuilder.uidSupplier(uidSupplier);

		final String authFile = authConfig.stringVal("file");
		if(authFile != null && !authFile.isEmpty()) {
			final Map<String, String> credentials = loadCredentials(authFile, (long) M);
			opsBuilder.credentialsMap(credentials);
		} else {
			final BatchSupplier<String> secretSupplier;
			final String secret = authConfig.stringVal("secret");
			if(secret == null) {
				secretSupplier = null;
			} else {
				secretSupplier = new ConstantStringSupplier(secret);
			}
			
			opsBuilder.secretSupplier(secretSupplier);
		}

		// init the items input
		final String itemInputFile = inputConfig.stringVal("file");
		if(itemInput == null) {
			if(
				(itemInputFile == null || itemInputFile.isEmpty()) && (itemInputPath == null || itemInputPath.isEmpty())
			) {
				itemInput = newItemInput();
			} else {
				itemInput = ItemInputFactory.createItemInput(itemConfig, batchSize, storageDriver);
			}
			if(itemInput == null) {
				throw new OmgShootMyFootException("No item input available");
			}
			if(ItemType.DATA.equals(itemType)) {
				sizeEstimate = estimateTransferSize(
					(DataOperationsBuilder) opsBuilder, opsBuilder.opType(), (Input<DataItem>) itemInput
				);
			} else {
				sizeEstimate = BUFF_SIZE_MIN;
			}
		}

		// check for the copy mode
		if(OpType.CREATE.equals(opType) && ItemType.DATA.equals(itemType) && !(itemInput instanceof NewItemInput)) {
			// intercept the items input for the storage side concatenation support
			final String itemDataRangesConcatConfig = rangesConfig.stringVal("concat");
			if(itemDataRangesConcatConfig != null) {
				final Range srcItemsCountRange = new Range(itemDataRangesConcatConfig);
				final long srcItemsCountMin = srcItemsCountRange.getBeg();
				final long srcItemsCountMax = srcItemsCountRange.getEnd();
				if(srcItemsCountMin < 0) {
					throw new OmgShootMyFootException("Source data items count min value should be more than 0");
				}
				if(srcItemsCountMax == 0 || srcItemsCountMax < srcItemsCountMin) {
					throw new OmgShootMyFootException(
						"Source data items count max value should be more than 0 and not less than min value"
					);
				}
				final List<I> srcItemsBuff = new ArrayList<>((int) M);
				final int srcItemsCount;
				try {
					srcItemsCount = loadSrcItems(itemInput, srcItemsBuff, (int) M);
				} finally {
					try {
						itemInput.close();
					} catch(final IOException ignored) {
					}
				}

				// shoot the foot
				if(srcItemsCount == 0) {
					throw new OmgShootMyFootException(
						"Available source items count " + srcItemsCount + " should be more than 0"
					);
				}
				if(srcItemsCount < srcItemsCountMin) {
					throw new OmgShootMyFootException(
						"Available source items count " + srcItemsCount + " is less than configured min "
							+ srcItemsCountMin
					);
				}
				if(srcItemsCount < srcItemsCountMax) {
					throw new OmgShootMyFootException(
						"Available source items count " + srcItemsCount + " is less than configured max "
							+ srcItemsCountMax
					);
				}

				// it's safe to cast to int here because the values will not be more than
				// srcItemsCount which is not more than the integer limit
				((DataOperationsBuilder) opsBuilder).srcItemsCount((int) srcItemsCountMin, (int) srcItemsCountMax);
				((DataOperationsBuilder) opsBuilder).srcItemsForConcat(srcItemsBuff);
				itemInput = newItemInput();
			}
		}

		// adjust the storage drivers for the estimated transfer size
		if(storageDriver == null) {
			throw new OmgShootMyFootException("Storage driver is not set");
		}
		if(sizeEstimate > 0 && ItemType.DATA.equals(itemType)) {
			storageDriver.adjustIoBuffers(sizeEstimate, opType);
		}

		final boolean recycleFlag = opConfig.boolVal("recycle");
		final boolean retryFlag = opConfig.boolVal("retry");
		final int recycleLimit = opConfig.intVal("limit-recycle");
		if(recycleLimit < 1) {
			throw new OmgShootMyFootException("Recycle limit should be > 0");
		}

		return (T) new LoadGeneratorImpl<>(
			itemInput, opsBuilder, throttles, storageDriver, batchSize, countLimit, recycleLimit,
			(recycleFlag || retryFlag), shuffleFlag
		);
	}
	
	private static long estimateTransferSize(
		final DataOperationsBuilder dataOpBuilder, final OpType opType, final Input<DataItem> itemInput
	) throws InterruptRunException {
		long sizeThreshold = 0;
		int randomRangesCount = 0;
		List<Range> fixedRanges = null;
		if(dataOpBuilder != null) {
			sizeThreshold = dataOpBuilder.sizeThreshold();
			randomRangesCount = dataOpBuilder.randomRangesCount();
			fixedRanges = dataOpBuilder.fixedRanges();
		}
		
		long itemSize = 0;
		final int maxCount = 0x100;
		final List<DataItem> items = new ArrayList<>(maxCount);
		int n = 0;
		try {
			while(n < maxCount) {
				n += itemInput.get(items, maxCount - n);
			}
		} catch(final EOFException ignored) {
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to estimate the average data item size");
		} finally {
			try {
				itemInput.reset();
			} catch(final IOException e) {
				LogUtil.exception(Level.WARN, e, "Failed reset the items input");
			}
		}
		
		long sumSize = 0;
		long minSize = Long.MAX_VALUE;
		long maxSize = Long.MIN_VALUE;
		long nextSize;
		if(n > 0) {
			try {
				for(int i = 0; i < n; i++) {
					nextSize = items.get(i).size();
					sumSize += nextSize;
					if(nextSize < minSize) {
						minSize = nextSize;
					}
					if(nextSize > maxSize) {
						maxSize = nextSize;
					}
				}
			} catch(final IOException e) {
				throw new AssertionError(e);
			}
			itemSize = minSize == maxSize ? sumSize / n : (minSize + maxSize) / 2;
		}
		
		switch(opType) {
			case CREATE:
				return Math.min(itemSize, sizeThreshold);
			case READ:
			case UPDATE:
				if(itemSize > 0 && randomRangesCount > 0) {
					return itemSize * randomRangesCount / rangeCount(itemSize);
				} else if(fixedRanges != null && !fixedRanges.isEmpty()) {
					long sizeSum = 0;
					long rangeSize;
					for(final Range byteRange : fixedRanges) {
						rangeSize = byteRange.getSize();
						if(rangeSize == -1) {
							rangeSize = byteRange.getEnd() - byteRange.getBeg() + 1;
						}
						if(rangeSize > 0) {
							sizeSum += rangeSize;
						}
					}
					return sizeSum;
				} else {
					return itemSize;
				}
			default:
				return 0;
		}
	}

	private BatchSupplier<String> getOutputPathSupplier()
	throws OmgShootMyFootException {
		final BatchSupplier<String> pathSupplier;
		String path = itemConfig.stringVal("output-path");
		if(path == null || path.isEmpty()) {
			path = LogUtil.getDateTimeStamp();
		}
		if(-1 == path.indexOf(PATTERN_CHAR)) {
			pathSupplier = new ConstantStringSupplier(path);
		} else {
			pathSupplier = new RangePatternDefinedSupplier(path);
		}
		return pathSupplier;
	}

	private Input<I> newItemInput()
	throws OmgShootMyFootException {
		final Config namingConfig = itemConfig.configVal("naming");
		final ItemNamingType namingType = ItemNamingType.valueOf(
			namingConfig.stringVal("type").toUpperCase()
		);
		final String namingPrefix = namingConfig.stringVal("prefix");
		final int namingLength = namingConfig.intVal("length");
		final int namingRadix = namingConfig.intVal("radix");
		final long namingOffset = namingConfig.longVal("offset");
		final ItemNameSupplier itemNameInput = new ItemNameSupplier(
			namingType, namingPrefix, namingLength, namingRadix, namingOffset
		);
		if(itemFactory == null) {
			throw new OmgShootMyFootException("Item factory is not set");
		}
		if(itemFactory instanceof DataItemFactoryImpl) {
			final SizeInBytes itemDataSize;
			final Object itemDataSizeRaw = itemConfig.val("data-size");
			if(itemDataSizeRaw instanceof String) {
				itemDataSize = new SizeInBytes((String) itemDataSizeRaw);
			} else {
				itemDataSize = new SizeInBytes(TypeUtil.typeConvert(itemDataSizeRaw, long.class));
			}
			itemInput = (Input<I>) new NewDataItemInput(itemFactory, itemNameInput, itemDataSize);
		} else {
			itemInput = new NewItemInput<>(itemFactory, itemNameInput);
		}
		return itemInput;
	}
	
	private static Map<String, String> loadCredentials(final String file, final long countLimit)
	throws OmgShootMyFootException {
		final Map<String, String> credentials = new HashMap<>();
		try(final BufferedReader br = Files.newBufferedReader(Paths.get(file))) {
			String line;
			String parts[];
			int firstCommaPos;
			long count = 0;
			while(null != (line = br.readLine()) && count < countLimit) {
				firstCommaPos = line.indexOf(',');
				if(-1 == firstCommaPos) {
					Loggers.ERR.warn("Invalid credentials line: \"{}\"", line);
				} else {
					parts = line.split(",", 2);
					credentials.put(parts[0], parts[1]);
					count ++;
				}
			}
			Loggers.MSG.info(
				"Loaded {} credential pairs from the file \"{}\"", credentials.size(), file
			);
		} catch(final IOException e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to load the credentials from the file \"{}\"", file
			);
		}
		return credentials;
	}

	private static <I extends Item> int loadSrcItems(
		final Input<I> itemInput, final List<I> itemBuff, final int countLimit
	) throws InterruptRunException {
		final LongAdder loadedCount = new LongAdder();
		final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
			2, new LogContextThreadFactory("loadSrcItemsWorker", true)
		);
		final CountDownLatch finishLatch = new CountDownLatch(1);
		try {
			executor.submit(
				() -> {
					int n = 0;
					int m;
					try {
						while(n < countLimit) {
							m = itemInput.get(itemBuff, countLimit - n);
							if(m < 0) {
								Loggers.MSG.info("Loaded {} items, limit reached", n);
								break;
							} else {
								loadedCount.add(m);
								n += m;
							}
						}
					} catch(final EOFException e) {
						Loggers.MSG.info("Loaded {} items, end of items input", n);
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "Loaded {} items, I/O failure occurred", n
						);
					} finally {
						finishLatch.countDown();
					}
				}
			);
			executor.scheduleAtFixedRate(
				() -> Loggers.MSG.info("Loaded {} items from the input...", loadedCount.sum()), 0, 10, TimeUnit.SECONDS
			);
			finishLatch.await();
		} catch(final InterruptedException e) {
			throw new InterruptRunException(e);
		} finally {
			executor.shutdownNow();
		}

		return loadedCount.intValue();
	}
}
