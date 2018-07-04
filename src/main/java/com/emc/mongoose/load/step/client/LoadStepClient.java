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
import com.emc.mongoose.load.step.metrics.MetricsSnapshotsSupplierTask;
import com.emc.mongoose.load.step.service.FileManagerService;
import com.emc.mongoose.load.step.service.LoadStepService;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.load.step.LoadStep;
import com.emc.mongoose.storage.driver.StorageDriver;
import com.emc.mongoose.svc.ServiceUtil;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;

import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.file.BinFileInput;
import com.github.akurilov.commons.math.MathUtil;
import com.github.akurilov.commons.system.SizeInBytes;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;

import org.apache.logging.log4j.CloseableThreadContext.Instance;

import static com.emc.mongoose.load.step.FileManager.APPEND_OPEN_OPTIONS;
import static org.apache.logging.log4j.CloseableThreadContext.put;
import org.apache.logging.log4j.Level;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface LoadStepClient
extends LoadStep {

	int OUTPUT_PROGRESS_PERIOD_MILLIS = 10_000;

	/**
	 Configure the step
	 @param config a dictionary of the configuration values to override the inherited config
	 @return <b>new/copied</b> step instance with the applied config values
	 */
	<T extends LoadStepClient> T config(final Map<String, Object> config);

	/**
	 Append the context. The actual behavior depends on the particular step type
	 @param context a dictionary of the additional parameters handled by the particular load step implementation
	 @return <b>new/copied</b> step instance with the appended context
	 */
	<T extends LoadStepClient> T append(final Map<String, Object> context);


	static void resolveFileManagers(final List<String> nodeAddrs, final List<FileManager> fileMgrsDst) {
		// local
		fileMgrsDst.add(new FileManagerImpl());
		// remote
		nodeAddrs
			.stream()
			.map(LoadStepClient::resolveFileManager)
			.forEachOrdered(fileMgrsDst::add);
	}

	static FileManagerService resolveFileManager(final String nodeAddrWithPort) {
		try {
			return (FileManagerService) ServiceUtil.resolve(nodeAddrWithPort, FileManagerService.SVC_NAME);
		} catch(final Exception e) {
			LogUtil.exception(Level.ERROR, e, "Failed to resolve the file manager service @ {}", nodeAddrWithPort);
		}
		return null;
	}

	static Config initConfigSlice(final Config config) {
		final Config configSlice = new BasicConfig(config);
		// disable the distributed mode on the slave nodes
		configSlice.val("load-step-node-addrs", Collections.EMPTY_LIST);
		return configSlice;
	}

	static Map<FileManager, String> initIoTraceLogFileSlices(final List<FileManager> fileMgrs, final String id) {
		return fileMgrs
			.stream()
			// exclude local I/O trace log file
			.filter(fileMgr -> fileMgr instanceof FileManagerService)
			.collect(
				Collectors.toMap(
					Function.identity(),
					fileMgr -> {
						String ioTraceLogFileSliceName = null;
						try {
							ioTraceLogFileSliceName = fileMgr.logFileName(Loggers.IO_TRACE.getName(), id);
							Loggers.MSG.debug(
								"{}: the remote file manager \"{}\" returned the file name \"{}\" for the I/O traces",
								id, fileMgr, ioTraceLogFileSliceName
							);
						} catch(final IOException e) {
							LogUtil.exception(Level.WARN, e, "{}: failed to get the remote log file name", id);
						}
						return ioTraceLogFileSliceName;
					}
				)
			);
	}

	static void sliceCountLimit(final long countLimit, final int sliceCount, final List<Config> configSlices) {
		final long countLimitPerSlice = (long) Math.ceil(((double) countLimit) / sliceCount);
		long remainingCountLimit = countLimit;
		for(int i = 0; i < sliceCount; i ++) {
			final Config limitConfigSlice = configSlices.get(i).configVal("load-step-limit");
			if(remainingCountLimit > countLimitPerSlice) {
				Loggers.MSG.debug("Config slice #{}: count limit = {}", i, countLimitPerSlice);
				limitConfigSlice.val("count", countLimitPerSlice);
				remainingCountLimit -= countLimitPerSlice;
			} else {
				Loggers.MSG.debug("Config slice #{}: count limit = {}", i, remainingCountLimit);
				limitConfigSlice.val("count", remainingCountLimit);
				remainingCountLimit = 0;
			}
		}
	}

	static void sliceStorageNodeAddrs(final List<Config> configSlices, final List<String> storageNodeAddrs) {
		final int sliceCount = configSlices.size();
		final int storageNodeCount = storageNodeAddrs.size();
		if(storageNodeCount > 1) {
			final int gcd = MathUtil.gcd(sliceCount, storageNodeCount);
			final int sliceCountPerGcd = sliceCount / gcd;
			final int storgeNodeCountPerGcd = storageNodeCount / gcd;
			for(int i = 0; i < gcd; i ++) {
				final List<String> storageNodeAddrsSlice = storageNodeAddrs.subList(
					storgeNodeCountPerGcd * i, storgeNodeCountPerGcd * (i + 1)
				);
				for(int j = i * sliceCountPerGcd; j < (i + 1) * sliceCountPerGcd; j ++) {
					Loggers.MSG.info(
						"Load step slice #{} ({}) storage nodes: {}", j, (j == 0 ? "local" : "remote"),
						Arrays.toString(storageNodeAddrsSlice.toArray())
					);
					configSlices.get(i).val("storage-net-node-addrs", storageNodeAddrsSlice);
				}
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
			Loggers.MSG.debug("Using the file \"{}\" as items input", itemInputFile);
		} else {
			final String itemInputPath = itemInputConfig.stringVal("path");
			if(itemInputPath != null && !itemInputPath.isEmpty()) {
				itemInput = createPathItemInput(
					itemConfig, config.configVal("load"), config.configVal("storage"), extensions, batchSize,
					itemFactory, itemInputPath
				);
				Loggers.MSG.debug("Using the storage path \"{}\" as items input", itemInputPath);
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
				fileItemInput = new BinFileInput<>(itemInputFilePath);
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

	static Map<FileManager, String> sliceItemInput(
		final Input<Item> itemInput, final List<FileManager> fileMgrs, final List<Config> configSlices,
		final int batchSize
	) throws IOException {

		final int sliceCount = configSlices.size();

		final Map<FileManager, String> itemInputFileSlices = new HashMap<>(sliceCount);
		for(int i = 0; i < sliceCount; i ++) {
			try {
				final FileManager fileMgr = fileMgrs.get(i);
				final String itemInputFileName = fileMgr.newTmpFileName();
				itemInputFileSlices.put(fileMgr, itemInputFileName);
			} catch(final Exception e) {
				LogUtil.exception(Level.ERROR, e, "Failed to get the item input file name for the step slice #" + i);
			}
		}

		final Map<FileManager, ByteArrayOutputStream> itemsOutByteBuffs = fileMgrs
			.stream()
			.collect(Collectors.toMap(Function.identity(), fileMgr -> new ByteArrayOutputStream(batchSize * 0x40)));

		final Map<FileManager, ObjectOutputStream> itemsOutputs = itemsOutByteBuffs
			.entrySet()
			.stream()
			.collect(
				Collectors.toMap(
					Map.Entry::getKey,
					entry -> {
						try {
							return new ObjectOutputStream(entry.getValue());
						} catch(final IOException ignored) {
						}
						return null;
					}
				)
			);

		transferItemsInputData(itemInput, batchSize, fileMgrs, itemInputFileSlices, itemsOutByteBuffs, itemsOutputs);

		itemsOutputs
			.values()
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
			final String itemInputFileName = itemInputFileSlices.get(fileMgrs.get(i));
			configSlice.val("item-input-file", itemInputFileName);
		}

		return itemInputFileSlices;
	}

	static void transferItemsInputData(
		final Input<Item> itemInput, final int batchSize, final List<FileManager> fileMgrs,
		final Map<FileManager, String> itemInputFileSlices,
		final Map<FileManager, ByteArrayOutputStream> itemsOutByteBuffs,
		final Map<FileManager, ObjectOutputStream> itemsOutputs
	) throws IOException {

		final int sliceCount = itemsOutByteBuffs.size();
		final List<Item> itemsBuff = new ArrayList<>(batchSize);

		int n;
		long count = 0;
		long lastProgressOutputTimeMillis = System.currentTimeMillis();
		final ObjectOutputStream out = itemsOutputs
			.values()
			.stream()
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("No outputs is available for items input data transfer"));

		Loggers.MSG.info(
			"Items input \"{}\": starting to distribute the items among the {} load step slices", itemInput, sliceCount
		);

		while(true) {

			// get the next batch of items
			try {
				n = itemInput.get(itemsBuff, batchSize);
			} catch(final EOFException e) {
				break;
			}

			if(n > 0) {

				// distribute the items using the round robin
				for(int i = 0; i < n; i ++) {
					itemsOutputs
						.get(fileMgrs.get(i % sliceCount))
						.writeUnshared(itemsBuff.get(i));
				}

				itemsBuff.clear();

				// write the text items data to the remote input files
				fileMgrs
					.parallelStream()
					.forEach(
						fileMgr -> {
							final ByteArrayOutputStream buff = itemsOutByteBuffs.get(fileMgr);
							final String itemInputFileName = itemInputFileSlices.get(fileMgr);
							try {
								final byte[] data = buff.toByteArray();
								fileMgr.writeToFile(itemInputFileName, data);
								buff.reset();
							} catch(final IOException e) {
								LogUtil.exception(
									Level.WARN, e,
									"Failed to write the items input data to the {} file \"{}\"",
									itemInputFileName, (fileMgr instanceof FileManagerService ? "remote" : "local")
								);
							}
						}
					);

				count += n;

				if(System.currentTimeMillis() - lastProgressOutputTimeMillis > OUTPUT_PROGRESS_PERIOD_MILLIS) {
					Loggers.MSG.info("Transferred {} items from the input \"{}\"...", count, itemInput);
					lastProgressOutputTimeMillis = System.currentTimeMillis();
				}

			} else {
				break;
			}
		}

		Loggers.MSG.info(
			"Items input \"{}\": {} items was distributed among the {} load step slices", itemInput, count, sliceCount
		);
	}

	static Map<FileManager, String> sliceItemOutputFileConfig(
		final List<FileManager> fileMgrs, final List<Config> configSlices, final String itemOutputFile
	) {
		final int sliceCount = fileMgrs.size();
		final Map<FileManager, String> itemOutputFileSlices = new HashMap<>(sliceCount);
		for(int i = 0; i < sliceCount; i ++) {
			final FileManager fileMgr = fileMgrs.get(i);
			if(i == 0) {
				if(fileMgr instanceof FileManagerService) {
					throw new AssertionError("File manager @ index #" + i + " shouldn't be a service");
				}
				itemOutputFileSlices.put(fileMgr, itemOutputFile);
			} else {
				if(fileMgr instanceof FileManagerService) {
					try {
						final String remoteItemOutputFileName = fileMgr.newTmpFileName();
						configSlices.get(i).val("item-output-file", remoteItemOutputFileName);
						itemOutputFileSlices.put(fileMgr, remoteItemOutputFileName);
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
		return itemOutputFileSlices;
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

	static boolean awaitStepSlice(final LoadStep stepSlice, final long timeout, final TimeUnit timeUnit) {
		try(
			final Instance logCtx = put(KEY_STEP_ID, stepSlice.id())
				.put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
		) {
			long commFailCount = 0;
			while(true) {
				try {
					if(stepSlice.await(timeout, timeUnit)) {
						return true;
					}
				} catch(final RemoteException e) {
					LogUtil.exception(
						Level.DEBUG, e, "Failed to invoke the step slice \"{}\" await method {} times",
						stepSlice, commFailCount
					);
					commFailCount ++;
					Thread.sleep(commFailCount);
				}
			}
		} catch(final InterruptedException e) {
			throw new CancellationException();
		} catch(final RemoteException ignored) {
			return false;
		}
	}

	static void stopMetricsSnapshotsSuppliers(
		final String id, final Map<LoadStep, MetricsSnapshotsSupplierTask> metricsSnapshotsSuppliers
	) {
		metricsSnapshotsSuppliers
			.values()
			.parallelStream()
			.forEach(
				snapshotsSupplier -> {
					try(
						final Instance logCtx = put(KEY_STEP_ID, id)
							.put(KEY_CLASS_NAME, LoadStepClient.class.getSimpleName())
					) {
						snapshotsSupplier.stop();
					} catch(final IOException e) {
						LogUtil.exception(Level.WARN, e, "{}: failed to stop the metrics snapshot supplier", id);
					}
				}
			);
	}

	static void releaseStepSlices(final String id, final List<LoadStep> stepSlices) {
		stepSlices
			.parallelStream()
			.forEach(
				stepSlice -> {
					try {
						stepSlice.close();
						Loggers.MSG.debug("{}: step slice \"{}\" closed", id, stepSlice);
					} catch(final Exception e) {
						LogUtil.exception(Level.WARN, e, "{}: failed to close the step service \"{}\"", id, stepSlice);
					}
				}
			);
		Loggers.MSG.debug("{}: closed all {} step slices", id, stepSlices.size());
		stepSlices.clear();
	}

	static void releaseMetricsSnapshotsSuppliers(
		final String id, final Map<LoadStep, MetricsSnapshotsSupplierTask> metricsSnapshotsSuppliers
	) {
		metricsSnapshotsSuppliers
			.values()
			.parallelStream()
			.forEach(
				snapshotsSupplier -> {
					try(
						final Instance logCtx = put(KEY_STEP_ID, id)
							.put(KEY_CLASS_NAME, LoadStepClient.class.getSimpleName())
					) {
						snapshotsSupplier.close();
					} catch(final IOException e) {
						LogUtil.exception(Level.WARN, e, "{}: failed to close the metrics snapshot supplier", id);
					}
				}
			);
		metricsSnapshotsSuppliers.clear();
	}

	static void releaseItemInputFileSlices(final String id, final Map<FileManager, String> itemInputFileSlices) {
		itemInputFileSlices
			.entrySet()
			.parallelStream()
			.forEach(
				entry -> {
					final FileManager fileMgr = entry.getKey();
					final String itemInputFileName = entry.getValue();
					try {
						fileMgr.deleteFile(itemInputFileName);
					} catch(final Exception e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to delete the file \"{}\" @ file manager \"{}\"", id,
							itemInputFileName, fileMgr
						);
					}
				}
			);
		itemInputFileSlices.clear();
	}

	static void collectItemOutputFileAndRelease(
		final String id, final Map<FileManager, String> itemOutputFileSlices, final String localItemOutputFileName
	) {
		final LongAdder byteCounter = new LongAdder();
		final Thread progressOutputThread = new Thread(
			() -> {
				Loggers.MSG.info(
					"\"{}\" <- start to transfer the output items data to the local file", localItemOutputFileName
				);
				try {
					while(true) {
						TimeUnit.MILLISECONDS.sleep(OUTPUT_PROGRESS_PERIOD_MILLIS);
						Loggers.MSG.info(
							"\"{}\" <- transferred {} of the output items data...", localItemOutputFileName,
							SizeInBytes.formatFixedSize(byteCounter.longValue())
						);
					}
				} catch(final InterruptedException ok) {
				}
			}
		);
		progressOutputThread.setDaemon(true);
		progressOutputThread.start();

		try(
			final OutputStream localItemOutput = Files.newOutputStream(
				Paths.get(localItemOutputFileName), APPEND_OPEN_OPTIONS
			)
		) {
			final Lock localItemOutputLock = new ReentrantLock();
			itemOutputFileSlices
				.entrySet()
				.parallelStream()
				// don't transfer & delete local item output file
				.filter(entry -> entry.getKey() instanceof FileManagerService)
				.forEach(
					entry -> {
						final FileManager fileMgr = entry.getKey();
						final String remoteItemOutputFileName = entry.getValue();
						transferItemOutputData(
							fileMgr, remoteItemOutputFileName, localItemOutput, localItemOutputLock, byteCounter
						);
						try {
							fileMgr.deleteFile(remoteItemOutputFileName);
						} catch(final Exception e) {
							LogUtil.exception(
								Level.WARN, e, "{}: failed to delete the file \"{}\" @ file manager \"{}\"", id,
								remoteItemOutputFileName, fileMgr
							);
						}
					}
				);
		} catch(final IOException e) {
			LogUtil.exception(
				Level.WARN, e, "{}: failed to open the local item output file \"{}\" for appending", id,
				localItemOutputFileName
			);
		} finally {
			progressOutputThread.interrupt();
			Loggers.MSG.info(
				"\"{}\" <- transferred {} of the output items data", localItemOutputFileName,
				SizeInBytes.formatFixedSize(byteCounter.longValue())
			);
			itemOutputFileSlices.clear();
		}
	}

	static void transferItemOutputData(
		final FileManager fileMgr, final String remoteItemOutputFileName, final OutputStream localItemOutput,
		final Lock localItemOutputLock, final LongAdder byteCounter
	) {
		long transferredByteCount = 0;
		try(final Instance logCtx = put(KEY_CLASS_NAME, LoadStepClient.class.getSimpleName())) {
			byte buff[];
			while(true) {
				buff = fileMgr.readFromFile(remoteItemOutputFileName, transferredByteCount);
				localItemOutputLock.lock();
				try {
					localItemOutput.write(buff);
				} finally {
					localItemOutputLock.unlock();
				}
				transferredByteCount += buff.length;
				byteCounter.add(buff.length);
			}
		} catch(final EOFException ok) {
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Remote items output file transfer failure");
		} finally {
			Loggers.MSG.debug(
				"{} of items output data transferred from \"{}\" @ \"{}\" to \"{}\"",
				SizeInBytes.formatFixedSize(transferredByteCount), remoteItemOutputFileName, fileMgr,
				localItemOutput
			);
		}
	}

	static void collectIoTraceLogFileAndRelease(final String id, final Map<FileManager, String> ioTraceLogFileSlices) {

		final LongAdder byteCounter = new LongAdder();
		final Thread progressOutputThread = new Thread(
			() -> {
				Loggers.MSG.info("\"{}\": start to transfer the I/O trace data to the local log file", id);
				try {
					while(true) {
						TimeUnit.MILLISECONDS.sleep(OUTPUT_PROGRESS_PERIOD_MILLIS);
						Loggers.MSG.info(
							"\"{}\": transferred {} I/O trace data...", id,
							SizeInBytes.formatFixedSize(byteCounter.longValue())
						);
					}
				} catch(final InterruptedException ok) {
				}
			}
		);
		progressOutputThread.setDaemon(true);
		progressOutputThread.start();

		try {
			ioTraceLogFileSlices
				.entrySet()
				.parallelStream()
				// don't transfer the local file data
				.filter(entry -> entry.getKey() instanceof FileManagerService)
				.forEach(
					entry -> {
						final FileManager fileMgr = entry.getKey();
						final String remoteIoTraceLogFileName = entry.getValue();
						transferIoTraceData(fileMgr, remoteIoTraceLogFileName, byteCounter);
						try {
							fileMgr.deleteFile(remoteIoTraceLogFileName);
						} catch(final Exception e) {
							LogUtil.exception(
								Level.WARN, e, "{}: failed to delete the file \"{}\" @ file manager \"{}\"", id,
								remoteIoTraceLogFileName, fileMgr
							);
						}
					}
				);
		} finally {
			progressOutputThread.interrupt();
			Loggers.MSG.info(
				"\"{}\": transferred {} of the I/O trace data", id,
				SizeInBytes.formatFixedSize(byteCounter.longValue())
			);
			ioTraceLogFileSlices.clear();
		}
	}

	static void transferIoTraceData(
		final FileManager fileMgr, final String remoteIoTraceLogFileName, final LongAdder byteCounter
	) {
		long transferredByteCount = 0;
		try(final Instance logCtx = put(KEY_CLASS_NAME, LoadStepClient.class.getSimpleName())) {
			byte[] data;
			while(true) {
				data = fileMgr.readFromFile(remoteIoTraceLogFileName, transferredByteCount);
				Loggers.IO_TRACE.info(new String(data));
				transferredByteCount += data.length;
				byteCounter.add(data.length);
			}
		} catch(final EOFException ok) {
		} catch(final RemoteException e) {
			LogUtil.exception(Level.WARN, e, "Failed to read the data from the remote file");
		} catch(final IOException e) {
			LogUtil.exception(Level.ERROR, e, "Unexpected I/O exception");
		} finally {
			Loggers.MSG.debug(
				"Transferred {} of the remote I/O traces data from the remote file \"{}\" @ \"{}\"",
				SizeInBytes.formatFixedSize(transferredByteCount), remoteIoTraceLogFileName, fileMgr
			);
		}
	}
}
