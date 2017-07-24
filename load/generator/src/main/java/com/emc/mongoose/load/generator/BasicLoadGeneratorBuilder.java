package com.emc.mongoose.load.generator;

import com.emc.mongoose.api.common.ByteRange;
import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.common.supply.BatchSupplier;
import com.emc.mongoose.api.common.supply.ConstantStringSupplier;
import com.emc.mongoose.api.common.io.Input;
import com.emc.mongoose.api.common.supply.RangePatternDefinedSupplier;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.io.task.IoTaskBuilder;
import com.emc.mongoose.api.model.io.task.data.BasicDataIoTaskBuilder;
import com.emc.mongoose.api.model.io.task.data.DataIoTaskBuilder;
import com.emc.mongoose.api.model.io.task.path.BasicPathIoTaskBuilder;
import com.emc.mongoose.api.model.io.task.token.BasicTokenIoTaskBuilder;
import com.emc.mongoose.api.model.item.BasicDataItemFactory;
import com.emc.mongoose.api.model.item.ItemNameSupplier;
import com.emc.mongoose.api.model.item.CsvFileItemInput;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.api.model.item.ChainTransferBuffer;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemNamingType;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.item.NewDataItemInput;
import com.emc.mongoose.api.model.io.IoType;
import static com.emc.mongoose.api.common.supply.PatternDefinedSupplier.PATTERN_CHAR;
import static com.emc.mongoose.api.model.item.DataItem.getRangeCount;
import static com.emc.mongoose.api.model.storage.StorageDriver.BUFF_SIZE_MIN;
import com.emc.mongoose.api.model.item.NewItemInput;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.data.ranges.RangesConfig;
import com.emc.mongoose.ui.config.item.input.InputConfig;
import com.emc.mongoose.ui.config.item.naming.NamingConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.auth.AuthConfig;
import com.emc.mongoose.ui.config.test.step.limit.LimitConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.Level;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 Created by andrey on 12.11.16.
 */
public class BasicLoadGeneratorBuilder<
	I extends Item, O extends IoTask<I>, T extends BasicLoadGenerator<I, O>
>
implements LoadGeneratorBuilder<I, O, T> {
	
	private ItemConfig itemConfig;
	private LoadConfig loadConfig;
	private LimitConfig limitConfig;
	private ItemType itemType;
	private ItemFactory<I> itemFactory;
	private AuthConfig authConfig;
	private List<StorageDriver<I, O>> storageDrivers;
	private Input<I> itemInput = null;
	private long sizeEstimate = 0;
	private int batchSize;
	
	@Override
	public BasicLoadGeneratorBuilder<I, O, T> setItemConfig(final ItemConfig itemConfig) {
		this.itemConfig = itemConfig;
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<I, O, T> setLoadConfig(final LoadConfig loadConfig) {
		this.loadConfig = loadConfig;
		this.batchSize = loadConfig.getBatchConfig().getSize();
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<I, O, T> setLimitConfig(final LimitConfig limitConfig) {
		this.limitConfig = limitConfig;
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<I, O, T> setItemType(final ItemType itemType) {
		this.itemType = itemType;
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<I, O, T> setItemFactory(final ItemFactory<I> itemFactory) {
		this.itemFactory = itemFactory;
		return this;
	}
	
	@Override
	public BasicLoadGeneratorBuilder<I, O, T> setAuthConfig(final AuthConfig authConfig) {
		this.authConfig = authConfig;
		return this;
	}
	
	@Override
	public BasicLoadGeneratorBuilder<I, O, T> setStorageDrivers(
		final List<StorageDriver<I, O>> storageDrivers
	) {
		this.storageDrivers = storageDrivers;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public BasicLoadGeneratorBuilder<I, O, T> setItemInput(final Input<I> itemInput) {
		this.itemInput = itemInput;
		// chain transfer buffer is not resettable
		if(!(itemInput instanceof ChainTransferBuffer)) {
			sizeEstimate = estimateTransferSize(
				null, IoType.valueOf(loadConfig.getType().toUpperCase()), (Input<DataItem>) itemInput
			);
		}
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public T build()
	throws UserShootHisFootException {
		
		final IoType ioType = IoType.valueOf(loadConfig.getType().toUpperCase());
		final IoTaskBuilder<I, O> ioTaskBuilder;
		final long countLimit = limitConfig.getCount();
		final SizeInBytes sizeLimit = limitConfig.getSize();
		final boolean shuffleFlag = loadConfig.getGeneratorConfig().getShuffle();

		final InputConfig inputConfig = itemConfig.getInputConfig();
		
		final BatchSupplier<String> outputPathSupplier;
		if(IoType.CREATE.equals(ioType) && ItemType.DATA.equals(itemType)) {
			outputPathSupplier = getOutputPathSupplier();
		} else {
			outputPathSupplier = null;
		}
		
		if(ItemType.DATA.equals(itemType)) {
			final RangesConfig rangesConfig = itemConfig.getDataConfig().getRangesConfig();
			final List<String> fixedRangesConfig = rangesConfig.getFixed();
			final List<ByteRange> fixedRanges;
			if(fixedRangesConfig != null) {
				fixedRanges = fixedRangesConfig
					.stream()
					.map(ByteRange::new)
					.collect(Collectors.toList());
			} else {
				fixedRanges = Collections.EMPTY_LIST;
			}
			ioTaskBuilder = (IoTaskBuilder<I, O>) new BasicDataIoTaskBuilder()
				.setFixedRanges(fixedRanges)
				.setRandomRangesCount(rangesConfig.getRandom())
				.setSizeThreshold(rangesConfig.getThreshold().get());
		} else if(ItemType.PATH.equals(itemType)){
			ioTaskBuilder = (IoTaskBuilder<I, O>) new BasicPathIoTaskBuilder();
		} else {
			ioTaskBuilder = (IoTaskBuilder<I, O>) new BasicTokenIoTaskBuilder();
		}
		
		String itemInputPath = inputConfig.getPath();
		if(itemInputPath != null && itemInputPath.indexOf('/') != 0) {
			itemInputPath = '/' + itemInputPath;
		}
		
		final BatchSupplier<String> uidSupplier;
		final String uid = authConfig.getUid();
		if(uid == null) {
			uidSupplier = null;
		} else if(-1 != uid.indexOf(PATTERN_CHAR)) {
			uidSupplier = new RangePatternDefinedSupplier(uid);
		} else {
			uidSupplier = new ConstantStringSupplier(uid);
		}

		final String authFile = authConfig.getFile();
		if(authFile != null && !authFile.isEmpty()) {
			final Map<String, String> credentials = loadCredentials(
				authFile, loadConfig.getQueueConfig().getSize()
			);
			ioTaskBuilder.setCredentialsMap(credentials);
		} else {

			final BatchSupplier<String> secretSupplier;
			final String secret = authConfig.getSecret();
			if(secret == null) {
				secretSupplier = null;
			} else {
				secretSupplier = new ConstantStringSupplier(secret);
			}
			
			ioTaskBuilder.setSecretSupplier(secretSupplier);
		}
		
		ioTaskBuilder
			.setIoType(IoType.valueOf(loadConfig.getType().toUpperCase()))
			.setInputPath(itemInputPath)
			.setOutputPathSupplier(outputPathSupplier)
			.setUidSupplier(uidSupplier);

		final String itemInputFile = inputConfig.getFile();
		if(itemInput == null) {
			itemInput = getItemInput(ioType, itemInputFile, itemInputPath);
			if(itemInput == null) {
				throw new UserShootHisFootException("No item input available");
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

		if(sizeEstimate != 0 && ItemType.DATA.equals(itemType)) {
			for(final StorageDriver<I, O> storageDriver : storageDrivers) {
				try {
					storageDriver.adjustIoBuffers(sizeEstimate, ioType);
				} catch(final RemoteException e) {
					LogUtil.exception(
						Level.WARN, e, "Failed to adjust the storage driver buffer sizes"
					);
				}
			}
		}

		final int recycleQueueSize = loadConfig.getCircular() ?
			loadConfig.getQueueConfig().getSize() : 0;

		return (T) new BasicLoadGenerator<>(
			itemInput, batchSize, sizeEstimate, ioTaskBuilder, countLimit, sizeLimit,
			recycleQueueSize, shuffleFlag
		);
	}
	
	private static long estimateTransferSize(
		final DataIoTaskBuilder dataIoTaskBuilder, final IoType ioType,
		final Input<DataItem> itemInput
	) {
		long sizeThreshold = 0;
		int randomRangesCount = 0;
		List<ByteRange> fixedRanges = null;
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
				if(randomRangesCount > 0) {
					return itemSize * randomRangesCount / getRangeCount(itemSize);
				} else if(fixedRanges != null && !fixedRanges.isEmpty()) {
					long sizeSum = 0;
					long rangeSize;
					for(final ByteRange byteRange : fixedRanges) {
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
	throws UserShootHisFootException {
		final BatchSupplier<String> pathSupplier;
		String path = itemConfig.getOutputConfig().getPath();
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
	private Input<I> getItemInput(
		final IoType ioType, final String itemInputFile, final String itemInputPath
	) throws UserShootHisFootException {
		
		if(itemInputFile == null || itemInputFile.isEmpty()) {

			final NamingConfig namingConfig = itemConfig.getNamingConfig();
			final ItemNamingType namingType = ItemNamingType.valueOf(
				namingConfig.getType().toUpperCase()
			);
			final String namingPrefix = namingConfig.getPrefix();
			final int namingLength = namingConfig.getLength();
			final int namingRadix = namingConfig.getRadix();
			final long namingOffset = namingConfig.getOffset();

			if(itemInputPath == null || itemInputPath.isEmpty()) {
				if(IoType.CREATE.equals(ioType) || IoType.NOOP.equals(ioType)) {
					final ItemNameSupplier itemNameInput = new ItemNameSupplier(
						namingType, namingPrefix, namingLength, namingRadix, namingOffset
					);
					if(itemFactory instanceof BasicDataItemFactory) {
						final SizeInBytes size = itemConfig.getDataConfig().getSize();
						itemInput = (Input<I>) new NewDataItemInput(
							itemFactory, itemNameInput, size
						);
					} else {
						itemInput = new NewItemInput<>(itemFactory, itemNameInput);
					}
				} else {
					throw new UserShootHisFootException(
						"No input (file either path) is specified for non-create generator"
					);
				}
			} else {
				itemInput = new StorageItemInput<>(
					storageDrivers.get(0), batchSize, itemFactory, itemInputPath, namingPrefix,
					namingRadix
				);
			}
		} else {
			try {
				itemInput = new CsvFileItemInput<>(Paths.get(itemInputFile), itemFactory);
			} catch(final NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "Failed to use the item input file \"{}\"", itemInputFile
				);
			}
		}

		return itemInput;
	}
	
	private static Map<String, String> loadCredentials(final String file, final long countLimit)
	throws UserShootHisFootException {
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
}
