package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.concurrent.AsyncRunnableBase;
import com.emc.mongoose.api.model.item.CsvFileItemInput;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.svc.ServiceUtil;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.input.InputConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.node.NodeConfig;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

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
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class StepBase
extends AsyncRunnableBase
implements Step, Runnable {

	protected final Config baseConfig;
	protected final List<Map<String, Object>> stepConfigs;
	protected final Map<String, String> env;
	protected String id;

	protected boolean distributedFlag = false;
	protected List<StepService> stepSvcs = null;
	protected Map<String, FileService> itemInputFileSvcs = null;

	protected StepBase(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs,
		final Map<String, String> env
	) {
		this.baseConfig = baseConfig;
		this.stepConfigs = stepConfigs;
		this.env = env;
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
	protected void doStart()
	throws IllegalStateException {
		final Config actualConfig = initConfig();
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
		} catch(final Throwable cause) {
			LogUtil.exception(Level.WARN, cause, "{} step failed to start", id);
		}
	}

	protected Config initConfig() {
		final String autoStepId = getTypeName() + "_" + LogUtil.getDateTimeStamp() + "_"
			+ hashCode();
		final Config config = new Config(baseConfig);
		if(stepConfigs != null && stepConfigs.size() > 0) {
			for(final Map<String, Object> nextStepConfig: stepConfigs) {
				config.apply(nextStepConfig, autoStepId);
			}
		}
		id = config.getTestConfig().getStepConfig().getId();
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
					.keySet()
					.parallelStream()
					.forEach(
						nodeAddrWithPort -> {
							final FileService fileSvc = itemInputFileSvcs.get(nodeAddrWithPort);
							try {
								fileSvc.close();
							} catch(final IOException e) {
								try {
									LogUtil.exception(
										Level.WARN, e,
										"Failed to close the file service \"{}\" @ {}",
										fileSvc.getName(), nodeAddrWithPort
									);
								} catch(final RemoteException ignored) {
								}
							}
						}
					);
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

	protected Map<String, Config> sliceConfigs(final Config config, final List<String> nodeAddrs) {

		final Map<String, Config> configSlices = nodeAddrs
			.stream()
			.collect(
				Collectors.toMap(
					Function.identity(),
					Function2.partial1(Step::initConfigSlice, config)
				)
			);

		final ItemConfig itemConfig = config.getItemConfig();

		// slice the item input file (if any)
		final InputConfig itemInputConfig = itemConfig.getInputConfig();
		final int batchSize = config.getLoadConfig().getBatchConfig().getSize();
		final String itemInputFile = itemInputConfig.getFile();

		if(itemInputFile != null && !itemInputFile.isEmpty()) {

			final int nodeCount = nodeAddrs.size();
			final List<Item> itemsBuff = new ArrayList<>(batchSize);
			final Map<String, StringBuilder> nodeItemsData = new HashMap<>(nodeCount);

			itemInputFileSvcs = new HashMap<>(nodeCount);
			final Function<String, Map<String, FileService>> resolveFileSvcsPartialFunc = Function3
				.partial12(FileService::resolveFileSvcs, nodeItemsData, itemInputFileSvcs);
			nodeAddrs
				.parallelStream()
				.map(resolveFileSvcsPartialFunc);

			final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
			final ItemFactory<? extends Item> itemFactory = ItemType.getItemFactory(itemType);
			int n;

			try(
				final Input<? extends Item> itemInput = new CsvFileItemInput<>(
					Paths.get(itemInputFile), itemFactory
				)
			) {
				final Function<String, String> writeItemDataPartialFunc = Function3
					.partial12(FileService::writeItemData, nodeItemsData, itemInputFileSvcs);
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
			} catch(final NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "Failed to use the item input file \"{}\"", itemInputFile
				);
			} finally {
				final Function<String, String> setConfigSlicesItemInputFilePartialFunc = Function3
					.partial12(Step::setConfigSlicesItemInputFile, configSlices, itemInputFileSvcs);
				nodeAddrs
					.parallelStream()
					.map(setConfigSlicesItemInputFilePartialFunc);
			}
		}

		return configSlices;
	}
}
