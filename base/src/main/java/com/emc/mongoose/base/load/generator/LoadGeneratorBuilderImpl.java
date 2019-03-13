package com.emc.mongoose.base.load.generator;

import static com.emc.mongoose.base.Constants.M;
import static com.emc.mongoose.base.config.el.Language.withLanguage;
import static com.emc.mongoose.base.item.DataItem.rangeCount;
import static com.emc.mongoose.base.storage.driver.StorageDriver.BUFF_SIZE_MIN;
import static com.github.akurilov.commons.io.el.ExpressionInput.ASYNC_MARKER;
import static com.github.akurilov.commons.io.el.ExpressionInput.INIT_MARKER;
import static com.github.akurilov.commons.io.el.ExpressionInput.SYNC_MARKER;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.emc.mongoose.base.config.ConstantValueInputImpl;
import com.emc.mongoose.base.config.el.CompositeExpressionInputBuilder;
import com.emc.mongoose.base.config.IllegalConfigurationException;
import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.DataItemFactoryImpl;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.ItemFactory;
import com.emc.mongoose.base.item.ItemType;
import com.emc.mongoose.base.item.TransferConvertBuffer;
import com.emc.mongoose.base.item.io.ItemInputFactory;
import com.emc.mongoose.base.item.naming.ItemNameInput;
import com.emc.mongoose.base.item.naming.ItemNameInput.ItemNamingType;
import com.emc.mongoose.base.item.io.NewDataItemInput;
import com.emc.mongoose.base.item.io.NewItemInput;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.OperationsBuilder;
import com.emc.mongoose.base.item.op.data.DataOperationsBuilder;
import com.emc.mongoose.base.item.op.data.DataOperationsBuilderImpl;
import com.emc.mongoose.base.item.op.path.PathOperationsBuilderImpl;
import com.emc.mongoose.base.item.op.token.TokenOperationsBuilderImpl;
import com.emc.mongoose.base.logging.LogContextThreadFactory;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.storage.Credential;
import com.emc.mongoose.base.storage.driver.StorageDriver;
import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.concurrent.throttle.IndexThrottle;
import com.github.akurilov.commons.concurrent.throttle.Throttle;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;

/** Created by andrey on 12.11.16. */
public class LoadGeneratorBuilderImpl<I extends Item, O extends Operation<I>, T extends LoadGeneratorImpl<I, O>>
				implements LoadGeneratorBuilder<I, O, T> {

	private Config itemConfig = null;
	private Config loadConfig = null;
	private ItemType itemType = null;
	private ItemFactory<I> itemFactory = null;
	private Config authConfig = null;
	private Output<O> opOutput = null;
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
	public LoadGeneratorBuilderImpl<I, O, T> loadOperationsOutput(final Output<O> opOutput) {
		this.opOutput = opOutput;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public LoadGeneratorBuilderImpl<I, O, T> itemInput(final Input<I> itemInput) {
		this.itemInput = itemInput;
		// pipeline transfer buffer is not resettable
		if (!(itemInput instanceof TransferConvertBuffer)) {
			sizeEstimate = estimateTransferSize(
							null,
							OpType.valueOf(loadConfig.stringVal("op-type").toUpperCase()),
							(Input<DataItem>) itemInput);
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
	public T build() throws IllegalConfigurationException {
		// prepare
		final OperationsBuilder<I, O> opsBuilder;
		if (loadConfig == null) {
			throw new IllegalConfigurationException("Load config is not set");
		}
		final var opConfig = loadConfig.configVal("op");
		final var countLimit = opConfig.longVal("limit-count");
		final var shuffleFlag = opConfig.boolVal("shuffle");
		if (itemConfig == null) {
			throw new IllegalConfigurationException("Item config is not set");
		}
		final var inputConfig = itemConfig.configVal("input");
		final var rangesConfig = itemConfig.configVal("data-ranges");
		if (itemType == null) {
			throw new IllegalConfigurationException("Item type is not set");
		}
		if (originIndex < 0) {
			throw new IllegalConfigurationException("No origin index is set");
		}
		// init the op builder
		if (ItemType.DATA.equals(itemType)) {
			final var fixedRangesConfig = rangesConfig.<String> listVal("fixed");
			final List<Range> fixedRanges;
			if (fixedRangesConfig == null) {
				fixedRanges = Collections.EMPTY_LIST;
			} else {
				fixedRanges = fixedRangesConfig.stream().map(Range::new).collect(Collectors.toList());
			}
			final long sizeThreshold;
			final var sizeThresholdRaw = rangesConfig.val("threshold");
			if (sizeThresholdRaw instanceof String) {
				sizeThreshold = SizeInBytes.toFixedSize((String) sizeThresholdRaw);
			} else {
				sizeThreshold = TypeUtil.typeConvert(sizeThresholdRaw, long.class);
			}
			opsBuilder = (OperationsBuilder<I, O>) new DataOperationsBuilderImpl(originIndex)
							.fixedRanges(fixedRanges)
							.randomRangesCount(rangesConfig.intVal("random"))
							.sizeThreshold(sizeThreshold);
		} else if (ItemType.PATH.equals(itemType)) {
			opsBuilder = (OperationsBuilder<I, O>) new PathOperationsBuilderImpl(originIndex);
		} else {
			opsBuilder = (OperationsBuilder<I, O>) new TokenOperationsBuilderImpl(originIndex);
		}
		// determine the operations type
		final var opType = OpType.valueOf(opConfig.stringVal("type").toUpperCase());
		opsBuilder.opType(opType);
		// determine the input path
		var itemInputPath = inputConfig.stringVal("path");
		if (itemInputPath != null && itemInputPath.indexOf('/') != 0) {
			itemInputPath = '/' + itemInputPath;
		}
		opsBuilder.inputPath(itemInputPath);
		// determine the output path
		final Input<String> outputPathSupplier;
		if (OpType.CREATE.equals(opType) && ItemType.DATA.equals(itemType)) {
			outputPathSupplier = getOutputPathSupplier();
		} else {
			outputPathSupplier = null;
		}
		opsBuilder.outputPathInput(outputPathSupplier);
		// init the credentials, multi-user case support
		if (authConfig == null) {
			throw new IllegalConfigurationException("Storage auth config is not set");
		}
		final var authFile = authConfig.stringVal("file");
		if (authFile != null && !authFile.isEmpty()) {
			final var credentials = loadCredentialsByPath(authFile, (long) M);
			opsBuilder.credentialsByPath(credentials);
		} else {
			final var uid = authConfig.stringVal("uid");
			final var secret = authConfig.stringVal("secret");
			if (null == uid && null == secret) {
				opsBuilder.credentialInput(new ConstantValueInputImpl<>(Credential.NONE));
			} else {
				opsBuilder.credentialInput(
								new ConstantValueInputImpl<>(Credential.getInstance(uid, secret)));
			}
		}
		// init the items input
		final var itemInputFile = inputConfig.stringVal("file");
		if (itemInput == null) {
			if ((itemInputFile == null || itemInputFile.isEmpty())
							&& (itemInputPath == null || itemInputPath.isEmpty())) {
				itemInput = newItemInput();
			} else if (opOutput instanceof StorageDriver) {
				itemInput = ItemInputFactory.createItemInput(itemConfig, batchSize, (StorageDriver<I, O>) opOutput);
			}
			if (itemInput == null) {
				throw new IllegalConfigurationException("No item input available");
			}
			if (ItemType.DATA.equals(itemType)) {
				sizeEstimate = estimateTransferSize(
								(DataOperationsBuilder) opsBuilder,
								opsBuilder.opType(),
								(Input<DataItem>) itemInput);
			} else {
				sizeEstimate = BUFF_SIZE_MIN;
			}
		}
		// check for the copy mode
		if (OpType.CREATE.equals(opType)
						&& ItemType.DATA.equals(itemType)
						&& !(itemInput instanceof NewItemInput)) {
			// intercept the items input for the storage side concatenation support
			final var itemDataRangesConcatConfig = rangesConfig.stringVal("concat");
			if (itemDataRangesConcatConfig != null) {
				final var srcItemsCountRange = new Range(itemDataRangesConcatConfig);
				final var srcItemsCountMin = srcItemsCountRange.getBeg();
				final var srcItemsCountMax = srcItemsCountRange.getEnd();
				if (srcItemsCountMin < 0) {
					throw new IllegalConfigurationException(
									"Source data items count min value should be more than 0");
				}
				if (srcItemsCountMax == 0 || srcItemsCountMax < srcItemsCountMin) {
					throw new IllegalConfigurationException(
									"Source data items count max value should be more than 0 and not less than min value");
				}
				final List<I> srcItemsBuff = new ArrayList<>((int) M);
				final int srcItemsCount;
				try {
					srcItemsCount = loadSrcItems(itemInput, srcItemsBuff, (int) M);
				} finally {
					try {
						itemInput.close();
					} catch (final Exception ignored) {}
				}
				// shoot the foot
				if (srcItemsCount == 0) {
					throw new IllegalConfigurationException(
									"Available source items count " + srcItemsCount + " should be more than 0");
				}
				if (srcItemsCount < srcItemsCountMin) {
					throw new IllegalConfigurationException(
									"Available source items count "
													+ srcItemsCount
													+ " is less than configured min "
													+ srcItemsCountMin);
				}
				if (srcItemsCount < srcItemsCountMax) {
					throw new IllegalConfigurationException(
									"Available source items count "
													+ srcItemsCount
													+ " is less than configured max "
													+ srcItemsCountMax);
				}
				// it's safe to cast to int here because the values will not be more than
				// srcItemsCount which is not more than the integer limit
				((DataOperationsBuilder) opsBuilder)
								.srcItemsCount((int) srcItemsCountMin, (int) srcItemsCountMax);
				((DataOperationsBuilder) opsBuilder).srcItemsForConcat(srcItemsBuff);
				itemInput = newItemInput();
			}
		}
		// adjust the storage drivers for the estimated transfer size
		if (opOutput == null) {
			throw new IllegalConfigurationException("Load operations output is not set");
		}
		if (sizeEstimate > 0 && ItemType.DATA.equals(itemType) && opOutput instanceof StorageDriver) {
			((StorageDriver) opOutput).adjustIoBuffers(sizeEstimate, opType);
		}
		final var recycleFlag = opConfig.boolVal("recycle");
		final var retryFlag = opConfig.boolVal("retry");
		final var recycleLimit = opConfig.intVal("limit-recycle");
		if (recycleLimit < 1) {
			throw new IllegalConfigurationException("Recycle limit should be > 0");
		}
		return (T) new LoadGeneratorImpl<>(
						itemInput,
						opsBuilder,
						throttles,
						opOutput,
						batchSize,
						countLimit,
						recycleLimit,
						(recycleFlag || retryFlag),
						shuffleFlag);
	}

	private static long estimateTransferSize(
					final DataOperationsBuilder dataOpBuilder,
					final OpType opType,
					final Input<DataItem> itemInput) {
		var sizeThreshold = 0L;
		var randomRangesCount = 0;
		List<Range> fixedRanges = null;
		if (dataOpBuilder != null) {
			sizeThreshold = dataOpBuilder.sizeThreshold();
			randomRangesCount = dataOpBuilder.randomRangesCount();
			fixedRanges = dataOpBuilder.fixedRanges();
		}
		var itemSize = 0L;
		final var maxCount = 0x100;
		final var items = (List<DataItem>) new ArrayList<DataItem>(maxCount);
		var n = 0;
		try {
			while (n < maxCount) {
				n += itemInput.get(items, maxCount - n);
			}
		} catch (final Exception e) {
			if (e instanceof IOException) {
				if (!(e instanceof EOFException)) {
					LogUtil.exception(Level.WARN, e, "Failed to estimate the average data item size");
				}
			} else {
				throw e;
			}
		} finally {
			try {
				itemInput.reset();
			} catch (final Exception e) {
				if (e instanceof IOException) {
					LogUtil.exception(Level.WARN, e, "Failed reset the items input");
				} else {
					throw e;
				}
			}
		}
		var sumSize = 0L;
		var minSize = Long.MAX_VALUE;
		var maxSize = Long.MIN_VALUE;
		long nextSize;
		if (n > 0) {
			try {
				for (var i = 0; i < n; i++) {
					nextSize = items.get(i).size();
					sumSize += nextSize;
					if (nextSize < minSize) {
						minSize = nextSize;
					}
					if (nextSize > maxSize) {
						maxSize = nextSize;
					}
				}
			} catch (final IOException e) {
				throw new AssertionError(e);
			}
			itemSize = minSize == maxSize ? sumSize / n : (minSize + maxSize) / 2;
		}
		switch (opType) {
		case CREATE:
			return Math.min(itemSize, sizeThreshold);
		case READ:
		case UPDATE:
			if (itemSize > 0 && randomRangesCount > 0) {
				return itemSize * randomRangesCount / rangeCount(itemSize);
			} else if (fixedRanges != null && !fixedRanges.isEmpty()) {
				long sizeSum = 0;
				long rangeSize;
				for (final var byteRange : fixedRanges) {
					rangeSize = byteRange.getSize();
					if (rangeSize == -1) {
						rangeSize = byteRange.getEnd() - byteRange.getBeg() + 1;
					}
					if (rangeSize > 0) {
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

	private Input<String> getOutputPathSupplier() {
		final Input<String> pathInput;
		final var path = itemConfig.stringVal("output-path");
		if (path.contains(ASYNC_MARKER) || path.contains(SYNC_MARKER) || path.contains(INIT_MARKER)) {
			pathInput = CompositeExpressionInputBuilder.newInstance()
							.expression(path)
							.build();
		} else {
			pathInput = new ConstantValueInputImpl<>(path);
		}
		return pathInput;
	}

	private Input<I> newItemInput() throws IllegalConfigurationException {
		final var namingConfig = itemConfig.configVal("naming");
		final var length = namingConfig.intVal("length");
		final var seedRaw = namingConfig.val("seed");
		long seed = 0;
		try {
			seed = TypeUtil.typeConvert(seedRaw, long.class);
		} catch(final ClassCastException | NumberFormatException e) {
			if(seedRaw instanceof String) {
				try(
					final var in = withLanguage(ExpressionInput.builder())
						.expression((String) seedRaw)
						.<ExpressionInput<Long>>build()
				) {
					seed = in.get();
				} catch(final Exception ee) {
					LogUtil.exception(Level.WARN, e, "Item naming seed expression (\"{}\") failure", seedRaw);
				}
			} else {
				throw new IllegalStateException(
					"Item naming seed (" + seedRaw + ") should be an integer either an expression"
				);
			}
		}
		final var prefix = namingConfig.stringVal("prefix");
		final var radix = namingConfig.intVal("radix");
		final var step = namingConfig.intVal("step");
		final var type = ItemNamingType.valueOf(namingConfig.stringVal("type").toUpperCase());
		final var itemNameInput = ItemNameInput.Builder.newInstance()
						.length(length)
						.seed(seed)
						.prefix(prefix)
						.radix(radix)
						.step(step)
						.type(type)
						.build();
		if (itemFactory == null) {
			throw new IllegalConfigurationException("Item factory is not set");
		}
		if (itemFactory instanceof DataItemFactoryImpl) {
			final SizeInBytes itemDataSize;
			final var itemDataSizeRaw = itemConfig.val("data-size");
			if (itemDataSizeRaw instanceof String) {
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

	private static Map<String, Credential> loadCredentialsByPath(
					final String file, final long countLimit) {
		final var credByPath = (Map<String, Credential>) new HashMap<String, Credential>();
		try (final var br = Files.newBufferedReader(Paths.get(file))) {
			String line;
			String parts[];
			long count = 0;
			while (null != (line = br.readLine()) && count < countLimit) {
				parts = line.split(",", 3);
				credByPath.put(parts[0], Credential.getInstance(parts[1], parts[2]));
				count++;
			}
			Loggers.MSG.info("Loaded {} credential pairs from the file \"{}\"", credByPath.size(), file);
		} catch (final Exception e) {
			LogUtil.exception(Level.WARN, e, "Failed to load the credentials from the file \"{}\"", file);
		}
		return credByPath;
	}

	private static <I extends Item> int loadSrcItems(
					final Input<I> itemInput, final List<I> itemBuff, final int countLimit) {
		final var loadedCount = new LongAdder();
		final var executor = Executors.newScheduledThreadPool(
						2, new LogContextThreadFactory("loadSrcItemsWorker", true));
		final var finishLatch = new CountDownLatch(1);
		try {
			executor.submit(
							() -> {
								var n = 0;
								int m;
								try {
									while (n < countLimit) {
										m = itemInput.get(itemBuff, countLimit - n);
										if (m < 0) {
											Loggers.MSG.info("Loaded {} items, limit reached", n);
											break;
										} else {
											loadedCount.add(m);
											n += m;
										}
									}
								} catch (final Exception e) {
									if (e instanceof EOFException) {
										Loggers.MSG.info("Loaded {} items, end of items input", n);
									} else if (e instanceof IOException) {
										LogUtil.exception(Level.WARN, e, "Loaded {} items, I/O failure occurred", n);
									} else {
										throw e;
									}
								} finally {
									finishLatch.countDown();
								}
							});
			executor.scheduleAtFixedRate(
							() -> Loggers.MSG.info("Loaded {} items from the input...", loadedCount.sum()),
							0,
							10,
							TimeUnit.SECONDS);
			finishLatch.await();
		} catch (final InterruptedException e) {
			throwUnchecked(e);
		} finally {
			executor.shutdownNow();
		}
		return loadedCount.intValue();
	}
}
