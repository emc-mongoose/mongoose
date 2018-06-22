package com.emc.mongoose.load.step.client;

import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.load.step.FileManager;
import com.emc.mongoose.load.step.LoadStepBase;
import com.emc.mongoose.load.step.service.FileManagerService;
import com.emc.mongoose.metrics.AggregatingMetricsContext;
import com.emc.mongoose.logging.LogContextThreadFactory;
import com.emc.mongoose.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.load.step.LoadStep;
import com.emc.mongoose.load.step.metrics.MetricsSnapshotsSupplierTask;
import com.emc.mongoose.load.step.metrics.MetricsSnapshotsSupplierTaskImpl;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;

import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.net.NetUtil;
import com.github.akurilov.commons.system.SizeInBytes;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;

import static com.emc.mongoose.load.step.FileManager.APPEND_OPEN_OPTIONS;
import static com.emc.mongoose.load.step.LoadStepFactory.createLocalLoadStep;
import static com.emc.mongoose.load.step.client.LoadStepClient.awaitStepSlice;
import static com.emc.mongoose.load.step.client.LoadStepClient.createItemInput;
import static com.emc.mongoose.load.step.client.LoadStepClient.initIoTraceLogFileServices;
import static com.emc.mongoose.load.step.client.LoadStepClient.resolveRemoteLoadStepSlice;
import static com.emc.mongoose.load.step.client.LoadStepClient.resolveFileManagers;
import static com.emc.mongoose.load.step.client.LoadStepClient.sliceCountLimit;
import static com.emc.mongoose.load.step.client.LoadStepClient.sliceItemInput;
import static com.emc.mongoose.load.step.client.LoadStepClient.sliceItemOutputFileConfig;
import static com.emc.mongoose.load.step.client.LoadStepClient.transferIoTraceData;
import static com.emc.mongoose.load.step.client.LoadStepClient.transferItemOutputData;

import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import static org.apache.logging.log4j.CloseableThreadContext.put;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

	private Map<FileManager, String> itemInputFileSlices = null;
	private Map<FileManager, String> itemOutputFileSlices = null;
	private Map<FileManager, String> ioTraceLogFileSlices = null;

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
				ioTraceLogFileSlices = initIoTraceLogFileServices(fileMgrs, id());
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
						LogUtil.exception(Level.ERROR, e, "{}: failed to start the step slice \"{}\"", id(), stepSlice);
					}
				}
			}

			Loggers.MSG.info("{}: load step client started @ {}", id(), Arrays.toString(nodeAddrs.toArray()));
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
				itemInputFileSlices = sliceItemInput(itemInput, fileMgrs, configSlices, batchSize);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "{}: failed to use the item input", id());
		} catch(final Throwable cause) {
			LogUtil.exception(Level.ERROR, cause, "Unexpected failure");
		}

		final String itemOutputFile = config.stringVal("item-output-file");
		if(itemOutputFile != null && !itemOutputFile.isEmpty()) {
			itemOutputFileSlices = sliceItemOutputFileConfig(fileMgrs, configSlices, itemOutputFile);
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
						final Instance logCtx = put(KEY_STEP_ID, id())
							.put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
					) {
						stepSlice.shutdown();
					} catch(final RemoteException e) {
						LogUtil.exception(Level.WARN, e, "{}: failed to shutdown the step service {}", id(), stepSlice);
					}
				}
			);
	}

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
			.map(stepSlice -> (Runnable) (() -> awaitStepSlice(stepSlice, timeout, timeUnit)))
			.forEach(awaitExecutor::submit);
		awaitExecutor.shutdown();
		return awaitExecutor.awaitTermination(timeout, TimeUnit.SECONDS);
	}

	@Override
	protected final void doStopWrapped() {
		stepSlices
			.parallelStream()
			.forEach(
				stepSlice -> {
					try(
						final Instance logCtx = put(KEY_STEP_ID, stepSlice.id())
							.put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
					) {
						stepSlice.stop();
					} catch(final Exception e) {
						LogUtil.exception(Level.WARN, e, "{}: failed to stop the step slice \"{}\"", id(), stepSlice);
					}
				}
			);
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
						LogUtil.exception(
							Level.WARN, e, "{}: failed to stop the remote metrics snapshot fetcher", id()
						);
					}
				}
			);

		stepSlices
			.parallelStream()
			.forEach(
				stepSlice -> {
					try {
						stepSlice.close();
					} catch(final Exception e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to close the step service \"{}\"", id(), stepSlice
						);
					}
				}
			);
		stepSlices.clear();

		metricsSnapshotsSuppliers
			.values()
			.parallelStream()
			.forEach(
				snapshotsFetcher -> {
					try(
						final Instance logCtx = put(KEY_STEP_ID, id())
							.put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
					) {
						snapshotsFetcher.close();
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to close the remote metrics snapshot fetcher", id()
						);
					}
				}
			);
		metricsSnapshotsSuppliers.clear();

		if(null != itemInputFileSlices) {
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
								Level.WARN, e, "{}: failed to delete the file \"{}\" @ file manager \"{}\"", id(),
								itemInputFileName, fileMgr
							);
						}
					}
				);
			itemInputFileSlices.clear();
			itemInputFileSlices = null;
		}

		if(null != itemOutputFileSlices) {
			final String localItemOutputFileName = baseConfig.stringVal("item-output-file");
			try(
				final OutputStream localItemOutput = Files.newOutputStream(
					Paths.get(localItemOutputFileName), APPEND_OPEN_OPTIONS
				)
			) {
				itemOutputFileSlices
					.entrySet()
					.parallelStream()
					// don't transfer & delete local item output file
					.filter(entry -> !(entry.getKey() instanceof FileManagerService))
					.forEach(
						entry -> {
							final FileManager fileMgr = entry.getKey();
							final String remoteItemOutputFileName = entry.getValue();
							transferItemOutputData(fileMgr, remoteItemOutputFileName, localItemOutput);
							try {
								fileMgr.deleteFile(remoteItemOutputFileName);
							} catch(final Exception e) {
								LogUtil.exception(
									Level.WARN, e, "{}: failed to delete the file \"{}\" @ file manager \"{}\"", id(),
									remoteItemOutputFileName, fileMgr
								);
							}
						}
					);
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "{}: failed to open the local item output file \"{}\" for appending", id(),
					localItemOutputFileName
				);
			}
			itemOutputFileSlices.clear();
			itemOutputFileSlices = null;
		}

		if(null != ioTraceLogFileSlices) {
			Loggers.MSG.info("{}: transfer the I/O traces data from the nodes", id());
			ioTraceLogFileSlices
				.entrySet()
				.parallelStream()
				// don't transfer the local file data
				.filter(entry -> !(entry.getKey() instanceof FileManagerService))
				.forEach(
					entry -> {
						final FileManager fileMgr = entry.getKey();
						final String remoteIoTraceLogFileName = entry.getValue();
						transferIoTraceData(fileMgr, remoteIoTraceLogFileName);
						try {
							fileMgr.deleteFile(remoteIoTraceLogFileName);
						} catch(final Exception e) {
							LogUtil.exception(
								Level.WARN, e, "{}: failed to delete the file \"{}\" @ file manager \"{}\"", id(),
								remoteIoTraceLogFileName, fileMgr
							);
						}
					}
				);
			ioTraceLogFileSlices.clear();
			ioTraceLogFileSlices = null;
		}
	}

	@Override
	public final LoadStepClientBase config(final Map<String, Object> config) {
		return this;
	}
}
