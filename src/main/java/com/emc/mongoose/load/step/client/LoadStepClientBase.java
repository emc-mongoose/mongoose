package com.emc.mongoose.load.step.client;

import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.load.step.FileManager;
import com.emc.mongoose.load.step.FileManagerImpl;
import com.emc.mongoose.load.step.LoadStepBase;
import com.emc.mongoose.metrics.AggregatingMetricsContext;
import com.emc.mongoose.metrics.MetricsSnapshot;
import com.emc.mongoose.logging.LogContextThreadFactory;
import com.emc.mongoose.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.svc.Service;
import com.emc.mongoose.svc.ServiceUtil;
import com.emc.mongoose.load.step.service.FileManagerService;
import com.emc.mongoose.load.step.LoadStep;
import com.emc.mongoose.load.step.LoadStepManagerService;
import com.emc.mongoose.load.step.service.LoadStepService;
import com.emc.mongoose.load.step.metrics.MetricsSnapshotsSupplierTask;
import com.emc.mongoose.load.step.metrics.MetricsSnapshotsSupplierTaskImpl;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;

import com.github.akurilov.commons.func.Function3;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.net.NetUtil;
import com.github.akurilov.commons.system.SizeInBytes;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;

import static com.emc.mongoose.load.step.LoadStepFactory.createLocalLoadStep;
import static com.emc.mongoose.load.step.client.LoadStepClient.createItemInput;
import static com.emc.mongoose.load.step.client.LoadStepClient.initIoTraceLogFileServices;
import static com.emc.mongoose.load.step.client.LoadStepClient.resolveRemoteLoadStepSlice;
import static com.emc.mongoose.load.step.client.LoadStepClient.resolveFileManagers;
import static com.emc.mongoose.load.step.client.LoadStepClient.sliceCountLimit;
import static com.emc.mongoose.load.step.client.LoadStepClient.sliceItemInput;
import static com.emc.mongoose.load.step.client.LoadStepClient.sliceItemOutputFileConfig;

import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import static org.apache.logging.log4j.CloseableThreadContext.put;
import org.apache.logging.log4j.Level;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class LoadStepClientBase
extends LoadStepBase
implements LoadStepClient {

	private final List<LoadStep> stepSlices = new ArrayList<>();
	private final Map<LoadStep, MetricsSnapshotsSupplierTask> metricsSnapshotsSuppliers = new HashMap<>();
	private final List<FileManager> fileMgrs = new ArrayList<>();

	public LoadStepClientBase(
		final Config baseConfig, final List<Extension> extensions, final List<Map<String, Object>> stepConfigs
	) {
		super(baseConfig, extensions, stepConfigs);
	}

	private List<String> itemInputFileNames = null;
	private List<String> itemOutputFileNames = null;
	private List<String> ioTraceLogFileNames = null;

	@Override
	protected final void doStartWrapped()
	throws IllegalArgumentException {

		try(final Instance logCtx = put(KEY_STEP_ID, id()).put(KEY_CLASS_NAME, getClass().getSimpleName())) {

			// need to set the once generated step id
			final Config config = new BasicConfig(baseConfig);
			config.val("load-step-id", id());

			// determine the additional/remote full node addresses
			final Config nodeConfig = baseConfig.configVal("load-step-node");
			final int nodePort = nodeConfig.intVal("port");
			final List<String> nodeAddrs = nodeConfig
				.<String>listVal("addrs")
				.stream()
				.map(addr -> NetUtil.addPortIfMissing(addr, nodePort))
				.collect(Collectors.toList());

			resolveFileManagers(nodeAddrs, fileMgrs);

			if(baseConfig.boolVal("output-metrics-trace-persist")) {
				ioTraceLogFileNames = initIoTraceLogFileServices(fileMgrs, id());
			}

			final int sliceCount = 1 + nodeAddrs.size();
			final List<Config> configSlices = sliceConfigs(baseConfig, sliceCount);

			final String stepTypeName;
			try {
				stepTypeName = getTypeName();
			} catch(final RemoteException e) {
				throw new AssertionError(e);
			}

			for(int i = 0; i < sliceCount; i ++) {

				final Config configSlice = configSlices.get(i);
				final LoadStep stepSlice;
				if(i == 0) {
					stepSlice = createLocalLoadStep(configSlice, extensions, stepConfigs, stepTypeName);
				} else {
					final String nodeAddrWithPort = nodeAddrs.get(i);
					stepSlice = resolveRemoteLoadStepSlice(configSlice, stepConfigs, stepTypeName, nodeAddrWithPort);
				}
				stepSlices.add(stepSlice);

				if(stepSlice != null) {
					try {
						stepSlice.start();
						final MetricsSnapshotsSupplierTask snapshotsSupplier = new MetricsSnapshotsSupplierTaskImpl(
							ServiceTaskExecutor.INSTANCE, stepSlice
						);
						snapshotsSupplier.start();
						metricsSnapshotsSuppliers.put(stepSlice, snapshotsSupplier);
					} catch(final Exception e) {
						LogUtil.exception(Level.ERROR, e, "Failed to start the step slice \"{}\"", stepSlice);
					}
				}
			}

			Loggers.MSG.info(
				"Load step client \"{}\" started @ {}", id(), Arrays.toString(nodeAddrs.toArray())
			);
		}
	}

	private List<Config> sliceConfigs(final Config config, final int sliceCount) {

		final List<Config> configSlices = new ArrayList<>(sliceCount);
		for(int i = 0; i < sliceCount; i ++) {
			configSlices.add(LoadStep.initConfigSlice(config));
		}

		final long countLimit = config.longVal("load-step-limit-count");
		if(sliceCount > 1 && countLimit > 0) {
			sliceCountLimit(countLimit, sliceCount, configSlices);
		}

		final int batchSize = config.intVal("load-batch-size");
		try(final Input<Item> itemInput = createItemInput(config, extensions, batchSize)) {
			if(itemInput != null) {
				Loggers.MSG.info("{}: slice the item input \"{}\"...", id(), itemInput);
				itemInputFileNames = sliceItemInput(itemInput, fileMgrs, configSlices, batchSize);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to use the item input");
		} catch(final Throwable cause) {
			LogUtil.exception(Level.ERROR, cause, "Unexpected failure");
		}

		final String itemOutputFile = config.stringVal("item-output-file");
		if(itemOutputFile != null && !itemOutputFile.isEmpty()) {
			itemOutputFileNames = sliceItemOutputFileConfig(fileMgrs, configSlices, itemOutputFile);
		}

		return configSlices;
	}


	protected final void initMetrics(
		final int originIndex, final IoType ioType, final int concurrency, final Config metricsConfig,
		final SizeInBytes itemDataSize, final boolean outputColorFlag
	) {
		final int nodeCount = metricsSnapshotsSuppliers.size();
		final int sumConcurrency = concurrency * nodeCount;
		final int sumConcurrencyThreshold = (int) (concurrency * nodeCount * metricsConfig.doubleVal("threshold"));
		final int metricsAvgPeriod = (int) TimeUtil.getTimeInSeconds(metricsConfig.stringVal("average-period"));
		final boolean metricsAvgPersistFlag = metricsConfig.boolVal("average-persist");
		final boolean metricsSumPersistFlag = metricsConfig.boolVal("summary-persist");
		final boolean metricsSumPerfDbOutputFlag = metricsConfig.boolVal("summary-perfDbResultsFile");

		metricsContexts.add(
			new AggregatingMetricsContext(
				id(), ioType, nodeCount, sumConcurrency, sumConcurrencyThreshold, itemDataSize, metricsAvgPeriod,
				outputColorFlag, metricsAvgPersistFlag, metricsSumPersistFlag, metricsSumPerfDbOutputFlag,
				() -> metricsSnapshotsSuppliers
					.values()
					.stream()
					.map(Supplier::get)
					.filter(Objects::nonNull)
					.map(metricsSnapshots -> metricsSnapshots.get(originIndex))
					.filter(Objects::nonNull)
					.collect(Collectors.toList())
			)
		);
	}

	@Override
	protected final void doShutdown() {
		stepSlices
			.parallelStream()
			.forEach(
				stepSlice -> {
					try(
						final Instance logCtx =put(KEY_STEP_ID, id())
							.put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
					) {
						stepSlice.shutdown();
					} catch(final RemoteException e) {
						LogUtil.exception(Level.WARN, e, "Failed to shutdown the step service {}", stepSlice);
					}
				}
			);
	}

	/*private static StepFileService createRemoteFile(
		final String nodeAddrWithPort, final StepFileService fileSvc
	) {
		try {
			fileSvc.open(StepFileService.WRITE_OPEN_OPTIONS);
			fileSvc.closeFile();
			final String filePath = fileSvc.filePath();
			Loggers.MSG.info("Use temporary remote item output file \"{}\"", filePath);
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to create the remote file @ {}", nodeAddrWithPort);
		}
		return fileSvc;
	}*/

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		if(stepSlices == null || stepSlices.size() == 0) {
			throw new IllegalStateException("No step slices are available");
		}
		final ExecutorService awaitExecutor = Executors.newFixedThreadPool(
			stepSlices.size(), new LogContextThreadFactory("stepSliceAwaitWorker", true)
		);
		stepSlices
			.stream()
			.map(
				stepSlice ->
					(Runnable) () ->
						Function3
							.partial1(LoadStepClientBase::awaitStepSlice, stepSlice)
							.apply(timeout, timeUnit)
			)
			.forEach(awaitExecutor::submit);
		awaitExecutor.shutdown();
		return awaitExecutor.awaitTermination(timeout, TimeUnit.SECONDS);
	}

	private static boolean awaitStepSlice(final LoadStep stepSlice, final long timeout, final TimeUnit timeUnit) {
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

	@Override
	protected final void doStopWrapped() {
		stepSlices
			.parallelStream()
			.forEach(LoadStepClientBase::stopStepSlice);
	}

	private static LoadStep stopStepSlice(final LoadStep stepSlice) {
		try(
			final Instance logCtx = put(KEY_STEP_ID, stepSlice.id())
				.put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
		) {
			stepSlice.stop();
		} catch(final Exception e) {
			try {
				LogUtil.exception(Level.WARN, e, "Failed to stop the step slice \"{}\"", stepSlice);
			} catch(final Exception ignored) {
			}
		}
		return stepSlice;
	}

	@Override
	protected final void doCloseWrapped() {

		metricsSnapshotsSuppliers
			.values()
			.parallelStream()
			.forEach(
				snapshotsFetcher -> {
					try(
						final Instance logCtx =put(KEY_STEP_ID, id())
							.put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
					) {
						snapshotsFetcher.stop();
					} catch(final IOException e) {
						LogUtil.exception(Level.WARN, e, "Failed to stop the remote metrics snapshot fetcher");
					}
				}
			);

		stepSlices.parallelStream().forEach(LoadStepClientBase::closeStepSlice);
		stepSlices.clear();

		metricsSnapshotsSuppliers
			.values()
			.parallelStream()
			.forEach(
				snapshotsFetcher -> {
					try(
						final Instance logCtx =put(KEY_STEP_ID, id())
							.put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
					) {
						snapshotsFetcher.close();
					} catch(final IOException e) {
						LogUtil.exception(Level.WARN, e, "Failed to close the remote metrics snapshot fetcher");
					}
				}
			);
		metricsSnapshotsSuppliers.clear();

		if(null != itemInputFileSvcs) {
			itemInputFileSvcs
				.entrySet()
				.parallelStream()
				.filter(entry -> entry.getValue().isPresent())
				.forEach(entry -> closeFileSvc(entry.getValue().get(), entry.getKey()));
			itemInputFileSvcs.clear();
			itemInputFileSvcs = null;
		}

		if(null != itemOutputFileNames) {
			final String itemOutputFile = baseConfig.stringVal("item-output-file");
			transferItemOutputData(itemOutputFileNames, itemOutputFile);
			itemOutputFileNames
				.entrySet()
				.parallelStream()
				.filter(entry -> entry.getValue().isPresent())
				.forEach(entry -> closeFileSvc(entry.getValue().get(), entry.getKey()));
			itemOutputFileNames.clear();
			itemOutputFileNames = null;
		}

		if(null != ioTraceLogFileNames) {
			Loggers.MSG.info("{}: transfer the I/O traces data from the nodes", id());
			ioTraceLogFileNames
				.values()
				.parallelStream()
				.filter(Optional::isPresent)
				.map(Optional::get)
				.forEach(LoadStepClientBase::transferIoTraceData);
			ioTraceLogFileNames.clear();
			ioTraceLogFileNames = null;
		}
	}

	private static void transferItemOutputData(
		final Map<String, Optional<StepFileService>> itemOutputFileSvcs, final String itemOutputFile
	) {
		final Path itemOutputFilePath = Paths.get(itemOutputFile);
		if(Files.exists(itemOutputFilePath)) {
			Loggers.MSG.info("Item output file \"{}\" already exists - will be appended", itemOutputFile);
		} else {
			Loggers.MSG.info(
				"Transfer the items output data from the remote nodes to the local file \"{}\"...",  itemOutputFile
			);
		}
		try(
			final OutputStream out = Files.newOutputStream(
				Paths.get(itemOutputFile), StepFileService.APPEND_OPEN_OPTIONS
			)
		) {
			itemOutputFileSvcs
				.values()
				.parallelStream()
				.filter(Optional::isPresent)
				.map(Optional::get)
				.forEach(
					fileSvc -> {
						long transferredByteCount = 0;
						try(
							final Instance logCtx =put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
						) {
							fileSvc.open(StepFileService.READ_OPTIONS);
							byte buff[];
							while(true) {
								buff = fileSvc.read();
								synchronized(out) {
									out.write(buff);
								}
								transferredByteCount += buff.length;
							}
						} catch(final EOFException ok) {
						} catch(final IOException e) {
							LogUtil.exception(Level.WARN, e, "Remote items output file transfer failure");
						} catch(final Throwable cause) {
							LogUtil.exception(Level.ERROR, cause, "Unexpected failure");
						} finally {
							try {
								Loggers.MSG.info(
									"{} of items output data transferred from \"{}\" to \"{}\"",
									SizeInBytes.formatFixedSize(transferredByteCount),
									fileSvc.name(), itemOutputFile
								);
							} catch(final RemoteException ignored) {
							}
						}
					}
				);
		} catch(final IOException e) {
			LogUtil.exception(
				Level.ERROR, e, "Failed to open the local file \"{}\" for the items output", itemOutputFile
			);
		}
	}

	private static LoadStep closeStepSlice(final LoadStep stepSlice) {
		if(null != stepSlice) {
			try(
				final Instance logCtx =put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
			) {
				stepSlice.close();
			} catch(final Exception e) {
				try {
					LogUtil.exception(Level.WARN, e, "Failed to close the step service \"{}\"",  stepSlice);
				} catch(final Exception ignored) {
				}
			}
		}
		return stepSlice;
	}

	private static StepFileService closeFileSvc(
		final StepFileService fileSvc, final String nodeAddrWithPort
	) {
		if(null != fileSvc) {
			try(
				final Instance logCtx =put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
			) {
				fileSvc.close();
			} catch(final IOException e) {
				try {
					LogUtil.exception(
						Level.WARN, e, "Failed to close the file service \"{}\" @ {}",  fileSvc.name(), nodeAddrWithPort
					);
				} catch(final RemoteException ignored) {
				}
			}
		}
		return fileSvc;
	}

	private static void transferIoTraceData(final StepFileService ioTraceLogFileSvc) {
		long transferredByteCount = 0;
		try(
			final Instance logCtx =put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
		) {
			ioTraceLogFileSvc.open(StepFileService.READ_OPTIONS);
			Loggers.MSG.debug("Opened the remote I/O traces file \"{}\"", ioTraceLogFileSvc.name());
			byte[] data;
			while(true) {
				data = ioTraceLogFileSvc.read();
				Loggers.IO_TRACE.info(new String(data));
				transferredByteCount += data.length;
			}
		} catch(final EOFException ok) {
		} catch(final RemoteException e) {
			LogUtil.exception(Level.WARN, e, "Failed to read the data from the remote file");
		} catch(final IOException e) {
			LogUtil.exception(Level.ERROR, e, "Unexpected I/O exception");
		} finally {
			try {
				Loggers.MSG.info(
					"Transferred {} of the remote I/O traces data from the remote file \"{}\"",
					SizeInBytes.formatFixedSize(transferredByteCount), ioTraceLogFileSvc.name()
				);
				ioTraceLogFileSvc.close();
			} catch(final IOException e) {
				try {
					LogUtil.exception(
						Level.DEBUG, e, "Failed to close the remote file {}",  ioTraceLogFileSvc.filePath()
					);
				} catch(final Exception ignored) {
				}
			}
		}
	}

	@Override
	public final LoadStepClientBase config(final Map<String, Object> config) {
		return this;
	}
}
