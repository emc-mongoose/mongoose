package com.emc.mongoose.load.step.client;

import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.CsvFileItemInput;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.ItemFactory;
import com.emc.mongoose.item.ItemType;
import com.emc.mongoose.item.StorageItemInput;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.load.step.FileManager;
import com.emc.mongoose.load.step.FileManagerImpl;
import com.emc.mongoose.load.step.LoadStepManagerService;
import com.emc.mongoose.load.step.service.FileManagerService;
import com.emc.mongoose.load.step.service.LoadStepService;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.metrics.MetricsSnapshot;
import com.emc.mongoose.load.step.LoadStep;
import com.emc.mongoose.storage.driver.StorageDriver;
import com.emc.mongoose.svc.ServiceUtil;

import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.file.BinFileInput;
import com.github.akurilov.commons.system.SizeInBytes;

import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.Level;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

public interface LoadStepClient
extends LoadStep {

	static void resolveFileManagers(final List<String> nodeAddrs, final List<FileManager> fileMgrsDst) {
		// local
		fileMgrsDst.add(new FileManagerImpl());
		// remote
		nodeAddrs
			.stream()
			.map(FileManagerService::resolve)
			.forEachOrdered(fileMgrsDst::add);
	}

	static List<String> initIoTraceLogFileServices(final List<FileManager> fileMgrs, final String id) {
		return fileMgrs
			.stream()
			.map(
				fileMgr -> fileMgr instanceof FileManagerService ?
					FileManager.logFileName(Loggers.IO_TRACE.getName(), id) : null
			)
			.collect(Collectors.toList());
	}

	static void sliceCountLimit(final long countLimit, final int sliceCount, final List<Config> configSlices) {
		final long countLimitPerSlice = (long) Math.ceil(((double) countLimit) / sliceCount);
		long remainingCountLimit = countLimit;
		for(int i = 0; i < sliceCount; i ++) {
			final Config limitConfigSlice = configSlices.get(i).configVal("load-step-limit");
			if(remainingCountLimit > countLimitPerSlice) {
				Loggers.MSG.info("Config slice #{}: count limit = {}", i, countLimitPerSlice);
				limitConfigSlice.val("count", countLimitPerSlice);
				remainingCountLimit -= countLimitPerSlice;
			} else {
				Loggers.MSG.info("Config slice #{}: count limit = {}", i, remainingCountLimit);
				limitConfigSlice.val("count", remainingCountLimit);
				remainingCountLimit = 0;
			}
		}
	}

	static <I extends Item> Input<I> createItemInput(
		final Config config, final List<Extension> extensions, final int batchSize
	) {

		Input<I> itemInput = null;

		final Config itemConfig = config.configVal("item");
		final ItemType itemType = ItemType.valueOf(itemConfig.stringVal("type").toUpperCase());
		final ItemFactory<I> itemFactory = ItemType.getItemFactory(itemType);
		final Config itemInputConfig = itemConfig.configVal("input");
		final String itemInputFile = itemInputConfig.stringVal("file");

		if(itemInputFile != null && !itemInputFile.isEmpty()) {
			itemInput = createFileItemInput(itemFactory, itemInputFile);
		} else {
			final String itemInputPath = itemInputConfig.stringVal("path");
			if(itemInputPath != null && !itemInputPath.isEmpty()) {
				itemInput = createPathItemInput(
					itemConfig, config.configVal("load"), config.configVal("storage"), extensions, batchSize,
					itemFactory, itemInputPath
				);
			}
		}

		return itemInput;
	}

	static <I extends Item> Input<I> createFileItemInput(
		final ItemFactory<I> itemFactory, final String itemInputFile
	) {

		Input<I> fileItemInput = null;

		final Path itemInputFilePath = Paths.get(itemInputFile);
		try {
			if(itemInputFile.endsWith(".csv")) {
				try {
					fileItemInput = new CsvFileItemInput<>(itemInputFilePath, itemFactory);
				} catch(final NoSuchMethodException e) {
					throw new AssertionError(e);
				}
			} else {
				return new BinFileInput<>(itemInputFilePath);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to open the item input file \"{}\"", itemInputFile);
		}

		return fileItemInput;
	}

	static <I extends Item> Input<I> createPathItemInput(
		final Config itemConfig, final Config loadConfig, final Config storageConfig,
		final List<Extension> extensions, final int batchSize, final ItemFactory<I> itemFactory,
		final String itemInputPath
	) {

		Input<I> itemInput = null;

		final Config dataConfig = itemConfig.configVal("data");
		final Config dataInputConfig = dataConfig.configVal("input");
		final Config dataLayerConfig = dataInputConfig.configVal("layer");
		try {
			final DataInput dataInput = DataInput.instance(
				dataInputConfig.stringVal("file"), dataInputConfig.stringVal("seed"),
				new SizeInBytes(dataLayerConfig.stringVal("size")), dataLayerConfig.intVal("cache")
			);
			final StorageDriver<I, IoTask<I>> storageDriver = StorageDriver.instance(
				extensions, loadConfig, storageConfig, dataInput, dataConfig.boolVal("verify"),
				loadConfig.stringVal("step-id")
			);
			final Config namingConfig = itemConfig.configVal("naming");
			final String namingPrefix = namingConfig.stringVal("prefix");
			final int namingRadix = namingConfig.intVal("radix");
			itemInput = new StorageItemInput<>(
				storageDriver, batchSize, itemFactory, itemInputPath, namingPrefix, namingRadix
			);
		} catch(final IOException | IllegalStateException | IllegalArgumentException e) {
			LogUtil.exception(Level.WARN, e, "Failed to initialize the data input");
		} catch(final OmgShootMyFootException e) {
			LogUtil.exception(Level.ERROR, e, "Failed to initialize the storage driver");
		} catch(final InterruptedException e) {
			throw new CancellationException();
		}

		return itemInput;
	}

	static List<String> sliceItemInput(
		final Input<Item> itemInput, final List<FileManager> fileMgrs, final List<Config> configSlices,
		final int batchSize
	) throws IOException {

		final int sliceCount = configSlices.size();

		final List<String> itemInputFileNames = new ArrayList<>(sliceCount);
		for(int i = 0; i < sliceCount; i ++) {
			try {
				final String itemInputFileName = fileMgrs.get(i).newTmpFileName();
				itemInputFileNames.add(itemInputFileName);
			} catch(final Exception e) {
				LogUtil.exception(Level.ERROR, e, "Failed to get the item input file name for the step slice #" + i);
				itemInputFileNames.add(null);
			}
		}

		final List<ByteArrayOutputStream> itemsOutByteBuffs = fileMgrs
			.stream()
			.map(fileMgr -> new ByteArrayOutputStream(batchSize * 0x40))
			.collect(Collectors.toList());

		final List<ObjectOutputStream> itemsOutputs = itemsOutByteBuffs
			.stream()
			.map(
				itemsOutByteBuff -> {
					try {
						return new ObjectOutputStream(itemsOutByteBuff);
					} catch(final IOException ignored) {
					}
					return null;
				}
			)
			.collect(Collectors.toList());

		transferItemsInputData(itemInput, batchSize, fileMgrs, itemInputFileNames, itemsOutByteBuffs, itemsOutputs);
		Loggers.MSG.info("{}: items input data is distributed to the {} step slices", configSlices.size());

		itemsOutputs
			.parallelStream()
			.filter(Objects::nonNull)
			.forEach(
				outStream -> {
					try {
						outStream.close();
					} catch(final IOException ignored) {
					}
				}
			);

		for(int i = 0; i < sliceCount; i ++) {
			final Config configSlice = configSlices.get(i);
			final String itemInputFileName = itemInputFileNames.get(i);
			configSlice.val("item-input-file", itemInputFileName);
		}

		return itemInputFileNames;
	}

	static void transferItemsInputData(
		final Input<? extends Item> itemInput, final int batchSize, final List<FileManager> fileMgrs,
		final List<String> itemInputFileNames, final List<ByteArrayOutputStream> itemsOutByteBuffs,
		final List<ObjectOutputStream> itemsOutputs
	) throws IOException {

		final int sliceCount = itemsOutByteBuffs.size();
		final List<? extends Item> itemsBuff = new ArrayList<>(batchSize);

		int n;
		final ObjectOutputStream out = itemsOutputs.get(0);

		while(true) {

			// get the next batch of items
			try {
				n = itemInput.get((List) itemsBuff, batchSize);
			} catch(final EOFException e) {
				break;
			}

			if(n > 0) {

				// convert the items to the text representation
				if(sliceCount > 1) {
					// distribute the items using round robin
					for(int i = 0; i < n; i ++) {
						itemsOutputs
							.get(i % sliceCount)
							.writeUnshared(itemsBuff.get(i));
					}
				} else {
					for(int i = 0; i < n; i ++) {
						out.writeUnshared(itemsBuff.get(i));
					}
				}

				itemsBuff.clear();

				// write the text items data to the remote input files
				for(int i = 0; i < sliceCount; i ++ ) {
					final FileManager fileMgr = fileMgrs.get(i);
					final ByteArrayOutputStream buff = itemsOutByteBuffs.get(i);
					final String itemInputFileName = itemInputFileNames.get(i);
					try {
						final byte[] data = buff.toByteArray();
						fileMgr.writeToFile(itemInputFileName, data);
						buff.reset();
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "Failed to write the items input data to the file manager #{}", i
						);
					}
				}
			} else {
				break;
			}
		}
	}

	static List<String> sliceItemOutputFileConfig(
		final List<FileManager> fileMgrs, final List<Config> configSlices, final String itemOutputFile
	) {
		final int sliceCount = fileMgrs.size();
		final List<String> itemOutputFileNames = new ArrayList<>(sliceCount);
		for(int i = 0; i < sliceCount; i ++) {
			final FileManager fileMgr = fileMgrs.get(i);
			if(i == 0) {
				if(fileMgr instanceof FileManagerService) {
					throw new AssertionError("File manager @ index #" + i + " shouldn't be a service");
				}
				itemOutputFileNames.add(itemOutputFile);
			} else {
				if(fileMgr instanceof FileManagerService) {
					try {
						final String remoteItemOutputFileName = fileMgr.newTmpFileName();
						configSlices.get(i).val("item-output-file", remoteItemOutputFileName);
					} catch(final Exception e) {
						LogUtil.exception(
							Level.ERROR, e,
							"Failed to get the new temporary file name for the file manager service \"{}\"", fileMgr
						);
					}
				} else {
					throw new AssertionError("File manager @ index #" + i + " should be a service");
				}
			}
		}
		return itemOutputFileNames;
	}

	static LoadStepService resolveRemoteLoadStepSlice(
		final Config configSlice, final List<Map<String, Object>> stepConfigs, final String stepTypeName,
		final String nodeAddrWithPort
	) {

		final LoadStepManagerService stepMgrSvc;
		try {
			stepMgrSvc = ServiceUtil.resolve(nodeAddrWithPort, LoadStepManagerService.SVC_NAME);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.ERROR, e, "Failed to resolve the service \"{}\" @ {}", LoadStepManagerService.SVC_NAME,
				nodeAddrWithPort
			);
			return null;
		}

		final String stepSvcName;
		try {
			stepSvcName = stepMgrSvc.getStepService(stepTypeName, configSlice, stepConfigs);
		} catch(final Exception e) {
			LogUtil.exception(Level.ERROR, e, "Failed to start the new scenario step service @ {}", nodeAddrWithPort);
			return null;
		}

		final LoadStepService stepSvc;
		try {
			stepSvc = ServiceUtil.resolve(nodeAddrWithPort, stepSvcName);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.ERROR, e, "Failed to resolve the service \"{}\" @ {}", LoadStepManagerService.SVC_NAME,
				nodeAddrWithPort
			);
			return null;
		}

		try {
			Loggers.MSG.info("{}: load step service is resolved @ {}", stepSvc.name(), nodeAddrWithPort);
		} catch(final RemoteException ignored) {
		}

		return stepSvc;
	}
}
