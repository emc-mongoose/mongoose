package com.emc.mongoose.load.generator;

import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.supply.BatchSupplier;
import com.emc.mongoose.supply.ConstantStringSupplier;
import com.emc.mongoose.supply.RangePatternDefinedSupplier;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.item.io.task.IoTaskBuilder;
import com.emc.mongoose.item.io.task.data.BasicDataIoTaskBuilder;
import com.emc.mongoose.item.io.task.data.DataIoTaskBuilder;
import com.emc.mongoose.item.io.task.path.BasicPathIoTaskBuilder;
import com.emc.mongoose.item.io.task.token.BasicTokenIoTaskBuilder;
import com.emc.mongoose.item.BasicDataItemFactory;
import com.emc.mongoose.item.CsvFileItemInput;
import com.emc.mongoose.item.DataItem;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.ItemFactory;
import com.emc.mongoose.item.ItemNameSupplier;
import com.emc.mongoose.item.ItemNamingType;
import com.emc.mongoose.item.ItemType;
import com.emc.mongoose.item.NewDataItemInput;
import com.emc.mongoose.item.NewItemInput;
import com.emc.mongoose.item.StorageItemInput;
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
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.file.BinFileInput;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.commons.concurrent.throttle.IndexThrottle;
import com.github.akurilov.commons.concurrent.throttle.Throttle;
import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.Level;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 Created by andrey on 12.11.16.
 */
public class BasicLoadGeneratorBuilder<
	I extends Item, O extends IoTask<I>, T extends BasicLoadGenerator<I, O>
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
	private Throttle rateThrottle = null;
	private IndexThrottle weightThrottle = null;
	
	@Override
	public BasicLoadGeneratorBuilder<I, O, T> itemConfig(final Config itemConfig) {
		this.itemConfig = itemConfig;
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<I, O, T> loadConfig(final Config loadConfig) {
		this.loadConfig = loadConfig;
		this.batchSize = loadConfig.intVal("batch-size");
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<I, O, T> limitConfig(final Config limitConfig) {
		this.limitConfig = limitConfig;
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<I, O, T> itemType(final ItemType itemType) {
		this.itemType = itemType;
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<I, O, T> itemFactory(final ItemFactory<I> itemFactory) {
		this.itemFactory = itemFactory;
		return this;
	}
	
	@Override
	public BasicLoadGeneratorBuilder<I, O, T> authConfig(final Config authConfig) {
		this.authConfig = authConfig;
		return this;
	}
	
	@Override
	public BasicLoadGeneratorBuilder<I, O, T> storageDriver(
		final StorageDriver<I, O> storageDriver
	) {
		this.storageDriver = storageDriver;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public BasicLoadGeneratorBuilder<I, O, T> itemInput(final Input<I> itemInput) {
		this.itemInput = itemInput;
		// chain transfer buffer is not resettable
		if(!(itemInput instanceof TransferConvertBuffer)) {
			sizeEstimate = estimateTransferSize(
				null, IoType.valueOf(loadConfig.stringVal("type").toUpperCase()),
				(Input<DataItem>) itemInput
			);
		}
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<I, O, T> originIndex(final int originIndex) {
		this.originIndex = originIndex;
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<I, O, T> rateThrottle(final Throttle rateThrottle) {
		this.rateThrottle = rateThrottle;
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<I, O, T> weightThrottle(final IndexThrottle weightThrottle) {
		this.weightThrottle = weightThrottle;
		return this;
	}

	@SuppressWarnings("unchecked")
	public T build()
	throws OmgShootMyFootException {

		// prepare
		final IoTaskBuilder<I, O> ioTaskBuilder;
		if(limitConfig == null) {
			throw new OmgShootMyFootException("Test step limit config is not set");
		}
		final long countLimit = limitConfig.longVal("count");
		final SizeInBytes sizeLimit = new SizeInBytes(limitConfig.stringVal("size"));
		if(loadConfig == null) {
			throw new OmgShootMyFootException("Load config is not set");
		}
		final Config generatorConfig = loadConfig.configVal("generator");
		final boolean shuffleFlag = generatorConfig.boolVal("shuffle");
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
		// init the I/O task builder
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
			ioTaskBuilder = (IoTaskBuilder<I, O>) new BasicDataIoTaskBuilder(originIndex)
				.setFixedRanges(fixedRanges)
				.setRandomRangesCount(rangesConfig.intVal("random"))
				.setSizeThreshold(SizeInBytes.toFixedSize(rangesConfig.stringVal("threshold")));
		} else if(ItemType.PATH.equals(itemType)){
			ioTaskBuilder = (IoTaskBuilder<I, O>) new BasicPathIoTaskBuilder(originIndex);
		} else {
			ioTaskBuilder = (IoTaskBuilder<I, O>) new BasicTokenIoTaskBuilder(originIndex);
		}

		// determine the operations type
		final IoType ioType = IoType.valueOf(loadConfig.stringVal("type").toUpperCase());
		ioTaskBuilder.setIoType(ioType);

		// determine the input path
		String itemInputPath = inputConfig.stringVal("path");
		if(itemInputPath != null && itemInputPath.indexOf('/') != 0) {
			itemInputPath = '/' + itemInputPath;
		}
		ioTaskBuilder.setInputPath(itemInputPath);

		// determine the output path
		final BatchSupplier<String> outputPathSupplier;
		if(IoType.CREATE.equals(ioType) && ItemType.DATA.equals(itemType)) {
			outputPathSupplier = getOutputPathSupplier();
		} else {
			outputPathSupplier = null;
		}
		ioTaskBuilder.setOutputPathSupplier(outputPathSupplier);

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
		ioTaskBuilder.setUidSupplier(uidSupplier);

		final String authFile = authConfig.stringVal("file");
		if(authFile != null && !authFile.isEmpty()) {
			final Map<String, String> credentials = loadCredentials(authFile, (long) M);
			ioTaskBuilder.setCredentialsMap(credentials);
		} else {

			final BatchSupplier<String> secretSupplier;
			final String secret = authConfig.stringVal("secret");
			if(secret == null) {
				secretSupplier = null;
			} else {
				secretSupplier = new ConstantStringSupplier(secret);
			}
			
			ioTaskBuilder.setSecretSupplier(secretSupplier);
		}

		// init the items input
		final String itemInputFile = inputConfig.stringVal("file");
		if(itemInput == null) {
			itemInput = itemInput(ioType, itemInputFile, itemInputPath);
			if(itemInput == null) {
				throw new OmgShootMyFootException("No item input available");
			}
			if(ItemType.DATA.equals(itemType)) {
				sizeEstimate = estimateTransferSize(
					(DataIoTaskBuilder) ioTaskBuilder, ioTaskBuilder.getIoType(),
					(Input<DataItem>) itemInput
				);
			} else {
				sizeEstimate = BUFF_SIZE_MIN;
			}
		}

		// intercept the items input for the copy ranges support
		final String itemDataRangesConcatConfig = rangesConfig.stringVal("concat");
		if(itemDataRangesConcatConfig != null) {
			final Range srcItemsCountRange = new Range(itemDataRangesConcatConfig);
			if(
				IoType.CREATE.equals(ioType)
					&& ItemType.DATA.equals(itemType)
					&& !(itemInput instanceof NewItemInput)
			) {
				final long srcItemsCountMin = srcItemsCountRange.getBeg();
				final long srcItemsCountMax = srcItemsCountRange.getEnd();
				if(srcItemsCountMin < 0) {
					throw new OmgShootMyFootException(
						"Source data items count min value should be more than 0"
					);
				}
				if(srcItemsCountMax == 0 || srcItemsCountMax < srcItemsCountMin) {
					throw new OmgShootMyFootException(
						"Source data items count max value should be more than 0 and not less than "
							+ "min value"
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
						"Available source items count " + srcItemsCount + " is less than configured"
							+ " min " + srcItemsCountMin
					);
				}
				if(srcItemsCount < srcItemsCountMax) {
					throw new OmgShootMyFootException(
						"Available source items count " + srcItemsCount + " is less than configured"
							+ " max " + srcItemsCountMax
					);
				}

				// it's safe to cast to int here because the values will not be more than
				// srcItemsCount which is not more than the integer limit
				((DataIoTaskBuilder) ioTaskBuilder).setSrcItemsCount(
					(int) srcItemsCountMin, (int) srcItemsCountMax
				);
				((DataIoTaskBuilder) ioTaskBuilder).setSrcItemsForConcat(srcItemsBuff);
				itemInput = newItemInput();
			}
		}

		// adjust the storage drivers for the estimated transfer size
		if(storageDriver == null) {
			throw new OmgShootMyFootException("Storage driver is not set");
		}
		if(sizeEstimate > 0 && ItemType.DATA.equals(itemType)) {
			storageDriver.adjustIoBuffers(sizeEstimate, ioType);
		}

		final Config recycleConfig = generatorConfig.configVal("recycle");
		final int
			recycleLimit = recycleConfig.boolVal("enabled") ? recycleConfig.intVal("limit") : 0;

		return (T) new BasicLoadGenerator<>(
			itemInput, ioTaskBuilder, storageDriver, rateThrottle, weightThrottle, batchSize,
			countLimit, sizeLimit, recycleLimit, shuffleFlag
		);
	}
	
	private static long estimateTransferSize(
		final DataIoTaskBuilder dataIoTaskBuilder, final IoType ioType,
		final Input<DataItem> itemInput
	) {
		long sizeThreshold = 0;
		int randomRangesCount = 0;
		List<Range> fixedRanges = null;
		if(dataIoTaskBuilder != null) {
			sizeThreshold = dataIoTaskBuilder.getSizeThreshold();
			randomRangesCount = dataIoTaskBuilder.getRandomRangesCount();
			fixedRanges = dataIoTaskBuilder.getFixedRanges();
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
		
		switch(ioType) {
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
		if(!path.startsWith("/")) {
			path = "/" + path;
		}
		if(-1 == path.indexOf(PATTERN_CHAR)) {
			pathSupplier = new ConstantStringSupplier(path);
		} else {
			pathSupplier = new RangePatternDefinedSupplier(path);
		}
		return pathSupplier;
	}

	@SuppressWarnings("unchecked")
	private Input<I> itemInput(
		final IoType ioType, final String itemInputFile, final String itemInputPath
	) throws OmgShootMyFootException {
		
		if(itemInputFile == null || itemInputFile.isEmpty()) {
			if(itemInputPath == null || itemInputPath.isEmpty()) {
				if(IoType.CREATE.equals(ioType) || IoType.NOOP.equals(ioType)) {
					itemInput = newItemInput();
				} else {
					throw new OmgShootMyFootException(
						"No input (file either path) is specified for non-create generator"
					);
				}
			} else {
				final Config namingConfig = itemConfig.configVal("naming");
				final String namingPrefix = namingConfig.stringVal("prefix");
				final int namingRadix = namingConfig.intVal("radix");
				itemInput = new StorageItemInput<>(
					storageDriver, batchSize, itemFactory, itemInputPath, namingPrefix, namingRadix
				);
			}
		} else {
			final Path itemInputFilePath = Paths.get(itemInputFile);
			try {
				if(itemInputFile.endsWith(".csv")) {
					try {
						itemInput = new CsvFileItemInput<>(itemInputFilePath, itemFactory);
					} catch(final NoSuchMethodException e){
						throw new RuntimeException(e);
					}
				} else {
					itemInput = new BinFileInput<>(itemInputFilePath);
				}
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "Failed to use the item input file \"{}\"", itemInputFile
				);
			}
		}

		return itemInput;
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
		if(itemFactory instanceof BasicDataItemFactory) {
			final SizeInBytes size = new SizeInBytes(itemConfig.stringVal("data-size"));
			itemInput = (Input<I>) new NewDataItemInput(itemFactory, itemNameInput, size);
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
	) {
		final LongAdder loadedCount = new LongAdder();
		final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
			2, new LogContextThreadFactory("loadSrcItemsWorker", true)
		);
		final Semaphore loadFinishSemaphore = new Semaphore(1);
		try {
			loadFinishSemaphore.acquire();
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
						loadFinishSemaphore.release();
					}

				}
			);
			executor.scheduleAtFixedRate(
				() -> Loggers.MSG.info("Loaded {} items from the input...", loadedCount.sum()),
				0, 10, TimeUnit.SECONDS
			);
			loadFinishSemaphore.acquire();
		} catch(final InterruptedException e) {
			throw new CancellationException(e.getMessage());
		} finally {
			executor.shutdownNow();
		}

		return loadedCount.intValue();
	}
}
