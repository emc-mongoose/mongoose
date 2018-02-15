package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.concurrent.AsyncRunnableBase;
import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.CsvFileItemInput;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.api.model.svc.ServiceUtil;
import com.emc.mongoose.load.generator.StorageItemInput;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.storage.driver.builder.StorageDriverUtil;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.data.DataConfig;
import com.emc.mongoose.ui.config.item.data.input.layer.LayerConfig;
import com.emc.mongoose.ui.config.item.input.InputConfig;
import com.emc.mongoose.ui.config.item.naming.NamingConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.config.output.metrics.MetricsConfig;
import com.emc.mongoose.ui.config.output.metrics.average.AverageConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.node.NodeConfig;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import com.emc.mongoose.ui.log.Loggers;

import com.github.akurilov.commons.func.Function2;
import com.github.akurilov.commons.func.Function3;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.net.NetUtil;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class StepBase
extends AsyncRunnableBase
implements Step, Runnable {

	protected final Config baseConfig;
	protected final List<Map<String, Object>> stepConfigs;

	private volatile Config actualConfig = null;
	private volatile long timeLimitSec = Long.MAX_VALUE;
	private volatile long startTimeSec = -1;
	private String id = null;
	private boolean distributedFlag = false;
	private List<StepService> stepSvcs = null;
	private Map<String, FileService> itemInputFileSvcs = null;
	private Map<String, FileService> itemOutputFileSvcs = null;

	protected StepBase(final Config baseConfig, final List<Map<String, Object>> stepConfigs) {
		this.baseConfig = baseConfig;
		this.stepConfigs = stepConfigs;
	}

	@Override
	public final void run() {
		try {
			start();
			try {
				await();
			} catch(final IllegalStateException e) {
				LogUtil.exception(Level.WARN, e, "Failed to await \"{}\"", toString());
			} catch(final InterruptedException e) {
				throw new CancellationException();
			}
		} catch(final IllegalStateException e) {
			LogUtil.exception(Level.WARN, e, "Failed to start \"{}\"", toString());
		} finally {
			try {
				close();
			} catch(final Exception e) {
				LogUtil.exception(Level.WARN, e, "Failed to close \"{}\"", toString());
			}
		}
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		final long minTimeoutSec = Math.min(timeLimitSec, timeUnit.toSeconds(timeout));
		if(distributedFlag) {
			if(stepSvcs == null || stepSvcs.size() == 0) {
				throw new IllegalStateException("No step services available");
			}
			final ExecutorService awaitExecutor = Executors.newFixedThreadPool(
				stepSvcs.size(), new LogContextThreadFactory("remoteStepSvcAwaitWorker", true)
			);
			stepSvcs
				.stream()
				.map(
					stepSvc ->
						(Runnable) () ->
							Function2
								.partial1(StepBase::awaitRemoteStepService, stepSvc)
								.apply(minTimeoutSec)
				)
				.forEach(awaitExecutor::submit);
			awaitExecutor.shutdown();
			return awaitExecutor.awaitTermination(minTimeoutSec, TimeUnit.SECONDS);
		} else {
			return awaitLocal(minTimeoutSec, TimeUnit.SECONDS);
		}
	}

	private static boolean awaitRemoteStepService(
		final StepService stepSvc, final long minTimeoutSec
	) {
		try {
			long commFailCount = 0;
			while(true) {
				try {
					if(stepSvc.await(minTimeoutSec, TimeUnit.SECONDS)) {
						return true;
					}
				} catch(final RemoteException e) {
					LogUtil.exception(
						Level.DEBUG, e,
						"Failed to invoke the step service \"{}\" await method {} times",
						stepSvc, commFailCount
					);
					commFailCount ++;
					Thread.sleep(commFailCount);
				}
			}
		} catch(final InterruptedException e) {
			throw new CancellationException();
		}
	}

	protected abstract boolean awaitLocal(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException;

	@Override
	protected void doStart()
	throws IllegalStateException {

		actualConfig = initConfig();
		final StepConfig stepConfig = actualConfig.getTestConfig().getStepConfig();
		final String stepId = stepConfig.getId();
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {

			distributedFlag = stepConfig.getDistributed();
			if(distributedFlag) {
				doStartRemote(actualConfig, stepConfig.getNodeConfig());
			} else {
				doStartLocal(actualConfig);
			}

			long t = stepConfig.getLimitConfig().getTime();
			if(t > 0) {
				timeLimitSec = t;
			}
			startTimeSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		} catch(final Throwable cause) {
			LogUtil.exception(Level.WARN, cause, "{} step failed to start", id);
		}
	}

	protected Config initConfig() {
		final String autoStepId = getTypeName().toLowerCase() + "_" + LogUtil.getDateTimeStamp()
			+ "_" + hashCode();
		final Config config = new Config(baseConfig);
		final StepConfig stepConfig = config.getTestConfig().getStepConfig();
		if(stepConfigs == null || stepConfigs.size() == 0) {
			if(stepConfig.getIdTmp()) {
				stepConfig.setId(autoStepId);
			}
		} else {
			for(final Map<String, Object> nextStepConfig: stepConfigs) {
				config.apply(nextStepConfig, autoStepId);
			}
		}
		id = stepConfig.getId();
		return config;
	}

	protected void doStartRemote(final Config actualConfig, final NodeConfig nodeConfig)
	throws IllegalArgumentException {

		final int nodePort = nodeConfig.getPort();

		final Function<String, String> addPortIfMissingPartialFunc = Function2
			.partial2(NetUtil::addPortIfMissing, nodePort);
		final List<String> nodeAddrs = nodeConfig
			.getAddrs()
			.stream()
			.map(addPortIfMissingPartialFunc)
			.collect(Collectors.toList());

		if(nodeAddrs.size() < 1) {
			throw new IllegalArgumentException(
				"There should be at least 1 node address to be configured if the distributed " +
					"mode is enabled"
			);
		}

		final Map<String, Config> configSlices = sliceConfigs(actualConfig, nodeAddrs);
		final Function<String, StepService> resolveStepSvcPartialFunc = Function2
			.partial1(this::resolveStepSvc, configSlices);
		stepSvcs = nodeAddrs
			.parallelStream()
			.map(resolveStepSvcPartialFunc)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private StepService resolveStepSvc(
		final Map<String, Config> configSlices, final String nodeAddrWithPort
	) {

		StepManagerService stepMgrSvc;
		try {
			stepMgrSvc = ServiceUtil.resolve(
				nodeAddrWithPort, StepManagerService.SVC_NAME
			);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to resolve the service \"{}\" @ {}",
				StepManagerService.SVC_NAME, nodeAddrWithPort
			);
			return null;
		}

		String stepSvcName;
		try {
			stepSvcName = stepMgrSvc.getStepService(
				getTypeName(), configSlices.get(nodeAddrWithPort)
			);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to start the new scenario step service @ {}",
				nodeAddrWithPort
			);
			return null;
		}

		StepService stepSvc;
		try {
			stepSvc = ServiceUtil.resolve(nodeAddrWithPort, stepSvcName);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to resolve the service \"{}\" @ {}",
				StepManagerService.SVC_NAME, nodeAddrWithPort
			);
			return null;
		}

		return stepSvc;
	}

	protected abstract void doStartLocal(final Config actualConfig);

	@Override
	protected void doStop() {

		if(distributedFlag) {
			stepSvcs
				.parallelStream()
				.forEach(StepBase::stopStepSvc);
		} else {
			doStopLocal();
		}

		final long t = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - startTimeSec;
		if(t < 0) {
			Loggers.ERR.warn(
				"Stopped earlier than started, won't account the elapsed time"
			);
		} else if(t > timeLimitSec) {
			Loggers.MSG.warn(
				"The elapsed time ({}[s]) is more than the limit ({}[s]), won't resume",
				t, timeLimitSec
			);
			timeLimitSec = 0;
		} else {
			timeLimitSec -= t;
		}
	}

	private static StepService stopStepSvc(final StepService stepSvc) {
		try {
			stepSvc.stop();
		} catch(final Exception e) {
			try {
				LogUtil.exception(
					Level.WARN, e, "Failed to stop the step service \"{}\"",
					stepSvc.getName()
				);
			} catch(final Exception ignored) {
			}
		}
		return stepSvc;
	}

	protected abstract void doStopLocal();

	@Override
	protected void doClose() {

		if(distributedFlag) {

			stepSvcs
				.parallelStream()
				.forEach(StepBase::closeStepSvc);
			stepSvcs.clear();
			stepSvcs = null;

			if(itemInputFileSvcs != null) {
				itemInputFileSvcs
					.entrySet()
					.parallelStream()
					.forEach(entry -> closeFileSvc(entry.getValue(), entry.getKey()));
			}
		} else {
			doCloseLocal();
		}
	}

	private static StepService closeStepSvc(final StepService stepSvc) {
		try {
			stepSvc.close();
		} catch(final Exception e) {
			try {
				LogUtil.exception(
					Level.WARN, e, "Failed to close the step service \"{}\"",
					stepSvc.getName()
				);
			} catch(final Exception ignored) {
			}
		}
		return stepSvc;
	}

	private static FileService closeFileSvc(
		final FileService fileSvc, final String nodeAddrWithPort
	) {
		try {
			fileSvc.close();
		} catch(final IOException e) {
			try {
				LogUtil.exception(
					Level.WARN, e, "Failed to close the file service \"{}\" @ {}",
					fileSvc.getName(), nodeAddrWithPort
				);
			} catch(final RemoteException ignored) {
			}
		}
		return fileSvc;
	}

	protected abstract void doCloseLocal();

	protected abstract String getTypeName();

	@Override
	public StepBase config(final Map<String, Object> config) {
		final List<Map<String, Object>> stepConfigsCopy = new ArrayList<>();
		if(stepConfigs != null) {
			stepConfigsCopy.addAll(stepConfigs);
		}
		stepConfigsCopy.add(config);
		return copyInstance(stepConfigsCopy);
	}

	@Override
	public final String id() {
		return id;
	}

	protected abstract StepBase copyInstance(final List<Map<String, Object>> stepConfigs);

	private Map<String, Config> sliceConfigs(final Config config, final List<String> nodeAddrs) {

		final Map<String, Config> configSlices = nodeAddrs
			.stream()
			.collect(
				Collectors.toMap(
					Function.identity(),
					Function2.partial1(Step::initConfigSlice, config)
				)
			);

		// slice an item input (if any)
		final int batchSize = config.getLoadConfig().getBatchConfig().getSize();
		try(final Input<? extends Item> itemInput = getItemInput(config, batchSize)) {
			if(itemInput != null) {
				sliceItemInput(itemInput, nodeAddrs, configSlices, batchSize);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to use the item input");
		}

		// slice an item output (if any configured)
		final String itemOutputFile = config.getItemConfig().getOutputConfig().getFile();
		if(itemOutputFile != null && !itemOutputFile.isEmpty()) {
			itemOutputFileSvcs = new HashMap<>(nodeAddrs.size());

		}

		return configSlices;
	}

	@SuppressWarnings("unchecked")
	private static Input<? extends Item> getItemInput(final Config config, final int batchSize)
	throws IOException {

		final ItemConfig itemConfig = config.getItemConfig();
		final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		final ItemFactory<? extends Item> itemFactory = ItemType.getItemFactory(itemType);
		final InputConfig itemInputConfig = itemConfig.getInputConfig();
		final String itemInputFile = itemInputConfig.getFile();

		if(itemInputFile != null && !itemInputFile.isEmpty()) {

			try {
				return new CsvFileItemInput<>(Paths.get(itemInputFile), itemFactory);
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "Failed to open the item input file \"{}\"", itemInputFile
				);
			} catch(final NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		} else {

			final String itemInputPath = itemInputConfig.getPath();
			if(itemInputPath != null && !itemInputPath.isEmpty()) {
				final DataConfig dataConfig = itemConfig.getDataConfig();
				final com.emc.mongoose.ui.config.item.data.input.InputConfig
					dataInputConfig = dataConfig.getInputConfig();
				final LayerConfig dataLayerConfig = dataInputConfig.getLayerConfig();
				final DataInput dataInput = DataInput.getInstance(
					dataInputConfig.getFile(), dataInputConfig.getSeed(),
					dataLayerConfig.getSize(), dataLayerConfig.getCache()
				);
				try {
					final StorageDriver storageDriver = new BasicStorageDriverBuilder<>()
						.setTestStepName(config.getTestConfig().getStepConfig().getId())
						.setItemConfig(itemConfig)
						.setContentSource(dataInput)
						.setLoadConfig(config.getLoadConfig())
						.setStorageConfig(config.getStorageConfig())
						.build();
					final NamingConfig namingConfig = itemConfig.getNamingConfig();
					final String namingPrefix = namingConfig.getPrefix();
					final int namingRadix = namingConfig.getRadix();
					return new StorageItemInput<>(
						storageDriver, batchSize, itemFactory, itemInputPath, namingPrefix,
						namingRadix
					);
				} catch(final OmgShootMyFootException e) {
					LogUtil.exception(Level.ERROR, e, "Failed to initialize the storage driver");
				} catch(final InterruptedException e) {
					throw new CancellationException();
				}
			}
		}

		return null;
	}

	private void sliceItemInput(
		final Input<? extends Item> itemInput, final List<String> nodeAddrs,
		final Map<String, Config> configSlices, final int batchSize
	) throws IOException {

		final int nodeCount = nodeAddrs.size();
		final List<Item> itemsBuff = new ArrayList<>(batchSize);
		final Map<String, StringBuilder> nodeItemsData = new HashMap<>(nodeCount);

		itemInputFileSvcs = new HashMap<>(nodeCount);
		final Function<String, Map<String, FileService>> resolveFileSvcsPartialFunc = Function3
			.partial12(FileService::resolveFileSvcs, nodeItemsData, itemInputFileSvcs);
		nodeAddrs
			.parallelStream()
			.map(resolveFileSvcsPartialFunc);

		final Function<String, String> writeItemDataPartialFunc = Function3
			.partial12(FileService::writeItemData, nodeItemsData, itemInputFileSvcs);
		int n;
		while(true) {
			try {
				n = itemInput.get((List) itemsBuff, batchSize);
			} catch(final EOFException e) {
				break;
			}
			if(n > 0) {
				for(int i = 0; i < n; i ++) {
					nodeItemsData
						.get(nodeAddrs.get(i % nodeCount))
						.append(itemsBuff.get(i).toString())
						.append(System.lineSeparator());
				}
				nodeAddrs
					.parallelStream()
					.map(writeItemDataPartialFunc);
			} else {
				break;
			}
		}

		final Function<String, String> setConfigSlicesItemInputFilePartialFunc = Function3
			.partial12(Step::setConfigSlicesItemInputFile, configSlices, itemInputFileSvcs);
		nodeAddrs
			.parallelStream()
			.map(setConfigSlicesItemInputFilePartialFunc);
	}
}
