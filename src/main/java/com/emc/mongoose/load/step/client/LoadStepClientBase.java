package com.emc.mongoose.load.step.client;

import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.load.step.FileManager;
import com.emc.mongoose.load.step.LoadStepBase;
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
import static com.emc.mongoose.load.step.LoadStepFactory.createLocalLoadStep;
import static com.emc.mongoose.load.step.client.LoadStepClient.collectIoTraceLogFileAndRelease;
import static com.emc.mongoose.load.step.client.LoadStepClient.initConfigSlice;
import static com.emc.mongoose.load.step.client.LoadStepClient.awaitStepSlice;
import static com.emc.mongoose.load.step.client.LoadStepClient.createItemInput;
import static com.emc.mongoose.load.step.client.LoadStepClient.initIoTraceLogFileSlices;
import static com.emc.mongoose.load.step.client.LoadStepClient.releaseItemInputFileSlices;
import static com.emc.mongoose.load.step.client.LoadStepClient.collectItemOutputFileAndRelease;
import static com.emc.mongoose.load.step.client.LoadStepClient.releaseMetricsSnapshotsSuppliers;
import static com.emc.mongoose.load.step.client.LoadStepClient.releaseStepSlices;
import static com.emc.mongoose.load.step.client.LoadStepClient.resolveRemoteLoadStepSlice;
import static com.emc.mongoose.load.step.client.LoadStepClient.resolveFileManagers;
import static com.emc.mongoose.load.step.client.LoadStepClient.sliceCountLimit;
import static com.emc.mongoose.load.step.client.LoadStepClient.sliceItemInput;
import static com.emc.mongoose.load.step.client.LoadStepClient.sliceItemOutputFileConfig;
import static com.emc.mongoose.load.step.client.LoadStepClient.stopMetricsSnapshotsSuppliers;
import com.emc.mongoose.metrics.MetricsContext;
import com.emc.mongoose.metrics.MetricsSnapshot;

import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.net.NetUtil;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import static com.github.akurilov.commons.collection.TreeUtil.reduceForest;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.exceptions.InvalidValuePathException;
import com.github.akurilov.confuse.exceptions.InvalidValueTypeException;
import com.github.akurilov.confuse.impl.BasicConfig;
import static com.github.akurilov.confuse.Config.deepToMap;

import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import static org.apache.logging.log4j.CloseableThreadContext.put;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
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
		final Config baseConfig, final List<Extension> extensions, final List<Map<String, Object>> contexts
	) {
		super(baseConfig, extensions, contexts);
	}

	private Map<FileManager, String> itemInputFileSlices = null;
	private Map<FileManager, String> itemOutputFileSlices = null;
	private Map<FileManager, String> ioTraceLogFileSlices = null;

	@Override
	protected final void doStartWrapped()
	throws IllegalArgumentException {

		try(
			final Instance logCtx = put(KEY_STEP_ID, id())
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {

			// need to set the once generated step id
			config.val("load-step-id", id());
			config.val("load-step-idAutoGenerated", false);

			// determine the additional/remote full node addresses
			final Config nodeConfig = config.configVal("load-step-node");
			final int nodePort = nodeConfig.intVal("port");
			final List<String> nodeAddrs = nodeConfig
				.<String>listVal("addrs")
				.stream()
				.map(addr -> NetUtil.addPortIfMissing(addr, nodePort))
				.collect(Collectors.toList());

			resolveFileManagers(nodeAddrs, fileMgrs);

			if(config.boolVal("output-metrics-trace-persist")) {
				ioTraceLogFileSlices = initIoTraceLogFileSlices(fileMgrs, id());
				Loggers.MSG.debug("{}: I/O trace log file slices initialized", id());
			}

			final int sliceCount = 1 + nodeAddrs.size();
			final List<Config> configSlices = sliceConfigs(config, sliceCount);

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
					stepSlice = createLocalLoadStep(configSlice, extensions, contexts, stepTypeName);
				} else {
					final String nodeAddrWithPort = nodeAddrs.get(i - 1);
					stepSlice = resolveRemoteLoadStepSlice(configSlice, contexts, stepTypeName, nodeAddrWithPort);
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

			Loggers.MSG.info(
				"{}: load step client started, additional nodes: {}", id(), Arrays.toString(nodeAddrs.toArray())
			);
		}
	}

	private List<Config> sliceConfigs(final Config config, final int sliceCount) {

		final List<Config> configSlices = new ArrayList<>(sliceCount);
		for(int i = 0; i < sliceCount; i ++) {
			final Config configSlice = initConfigSlice(config);
			if(i == 0) {
				// local step slice: disable the average metrics output
				configSlice.val("output-metrics-average-period", "0s");
			}
			configSlices.add(configSlice);
		}

		if(sliceCount > 1) {

			final long countLimit = config.longVal("load-step-limit-count");
			if(countLimit > 0) {
				sliceCountLimit(countLimit, sliceCount, configSlices);
			}

			final int batchSize = config.intVal("load-batch-size");
			try(final Input<Item> itemInput = createItemInput(config, extensions, batchSize)) {
				if(itemInput != null) {
					Loggers.MSG.info("{}: slice the item input \"{}\"...", id(), itemInput);
					itemInputFileSlices = sliceItemInput(itemInput, fileMgrs, configSlices, batchSize);
					Loggers.MSG.info("{}: slice the item input \"{}\" done", id(), itemInput);
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
		}

		return configSlices;
	}

	private int sliceCount() {
		return stepSlices.size();
	}

	private List<MetricsSnapshot> metricsSnapshotsByIndex(final int originIndex) {
		return metricsSnapshotsSuppliers
			.values()
			.stream()
			.map(Supplier::get)
			.filter(Objects::nonNull)
			.map(metricsSnapshots -> metricsSnapshots.get(originIndex))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	protected final void initMetrics(
		final int originIndex, final IoType ioType, final int concurrencyLimit, final Config metricsConfig,
		final SizeInBytes itemDataSize, final boolean outputColorFlag
	) {
		final double concurrencyThreshold = concurrencyLimit * metricsConfig.doubleVal("threshold");
		final int metricsAvgPeriod;
		final Object metricsAvgPeriodRaw = metricsConfig.val("average-period");
		if(metricsAvgPeriodRaw instanceof String) {
			metricsAvgPeriod = (int) TimeUtil.getTimeInSeconds((String) metricsAvgPeriodRaw);
		} else {
			metricsAvgPeriod = TypeUtil.typeConvert(metricsAvgPeriodRaw, int.class);
		}
		final boolean metricsAvgPersistFlag = metricsConfig.boolVal("average-persist");
		final boolean metricsSumPersistFlag = metricsConfig.boolVal("summary-persist");
		final boolean metricsSumPerfDbOutputFlag = metricsConfig.boolVal("summary-perfDbResultsFile");

		// it's not known yet how many nodes are involved, so passing the function "this::sliceCount" reference for
		// further usage
		final MetricsContext metricsCtx = new AggregatingMetricsContext(
			id(), ioType, this::sliceCount, concurrencyLimit, concurrencyThreshold, itemDataSize, metricsAvgPeriod,
			outputColorFlag, metricsAvgPersistFlag, metricsSumPersistFlag, metricsSumPerfDbOutputFlag,
			() -> metricsSnapshotsByIndex(originIndex)
		);
		metricsContexts.add(metricsCtx);
	}

	@Override
	protected final void doShutdown() {
		stepSlices
			.parallelStream()
			.forEach(
				stepSlice -> {
					try(
						final Instance logCtx = put(KEY_STEP_ID, id())
							.put(KEY_CLASS_NAME, getClass().getSimpleName())
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
	protected final void doStop() {
		stepSlices
			.parallelStream()
			.forEach(
				stepSlice -> {
					try(
						final Instance logCtx = put(KEY_STEP_ID, stepSlice.id())
							.put(KEY_CLASS_NAME, getClass().getSimpleName())
					) {
						stepSlice.stop();
					} catch(final Exception e) {
						LogUtil.exception(Level.WARN, e, "{}: failed to stop the step slice \"{}\"", id(), stepSlice);
					}
				}
			);
		super.doStop();
	}

	@Override
	protected final void doClose()
	throws IOException {
		super.doClose();
		stopMetricsSnapshotsSuppliers(id(), metricsSnapshotsSuppliers);
		releaseStepSlices(id(), stepSlices);
		releaseMetricsSnapshotsSuppliers(id(), metricsSnapshotsSuppliers);
		if(null != itemInputFileSlices) {
			releaseItemInputFileSlices(id(), itemInputFileSlices);
			itemInputFileSlices = null;
		}
		if(null != itemOutputFileSlices) {
			final String localItemOutputFileName = config.stringVal("item-output-file");
			collectItemOutputFileAndRelease(id(), itemOutputFileSlices, localItemOutputFileName);
			itemOutputFileSlices = null;
		}
		if(null != ioTraceLogFileSlices) {
			collectIoTraceLogFileAndRelease(id(), ioTraceLogFileSlices);
			ioTraceLogFileSlices = null;
		}
	}

	@Override
	public final <T extends LoadStepClient> T config(final Map<String, Object> configMap) {
		final Map<String, Object> baseConfigMap = deepToMap(config);
		final Map<String, Object> mergedConfigMap = reduceForest(Arrays.asList(baseConfigMap, configMap));
		final Config config;
		try {
			config = new BasicConfig(this.config.pathSep(), this.config.schema(), mergedConfigMap);
		} catch(final InvalidValueTypeException | InvalidValuePathException e) {
			LogUtil.exception(Level.FATAL, e, "Scenario syntax error");
			throw new CancellationException();
		}
		return copyInstance(config, contexts);
	}

	@Override
	public final <T extends LoadStepClient> T append(final Map<String, Object> context) {
		final List<Map<String, Object>> contextsCopy = new ArrayList<>();
		if(contexts != null) {
			contextsCopy.addAll(contexts);
		}
		final Map<String, Object> stepConfig = deepCopyTree(context);
		contextsCopy.add(stepConfig);
		return copyInstance(config, contextsCopy);
	}

	private static Map<String, Object> deepCopyTree(final Map<String, Object> srcTree) {
		return srcTree
			.entrySet()
			.stream()
			.collect(
				Collectors.toMap(
					Map.Entry::getKey,
					entry -> {
						final Object value = entry.getValue();
						return value instanceof Map ? deepCopyTree((Map<String, Object>) value) : value;
					}
				)
			);
	}

	protected abstract <T extends LoadStepClient> T copyInstance(
		final Config config, final List<Map<String, Object>> contexts
	);
}
