package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.concurrent.AsyncRunnableBase;
import com.emc.mongoose.api.model.item.CsvFileItemInput;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.svc.ServiceUtil;
import com.emc.mongoose.scenario.ScenarioParseException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.input.InputConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.node.NodeConfig;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import com.github.akurilov.commons.io.Input;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

public abstract class StepBase
extends AsyncRunnableBase
implements Step, Runnable {

	protected final Config baseConfig;
	protected final List<Map<String, Object>> stepConfigs;
	protected final Map<String, String> env;
	protected final List<StepService> stepSvcs = new ArrayList<>();
	protected boolean distributedFlag = false;
	protected String id;

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
			LogUtil.exception(Level.WARN, cause, "{} step failed", id);
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

		final List<String> nodeAddrs = nodeConfig
			.getAddrs()
			.stream()
			.map(
				nodeAddr -> nodeAddr.contains(":") ?
					nodeAddr : nodeAddr + ':' + Integer.toString(nodePort)
			)
			.collect(Collectors.toList());

		if(nodeAddrs.size() < 1) {
			throw new IllegalArgumentException(
				"There should be at least 1 node address to be configured if the distributed " +
					"mode is enabled"
			);
		}

		final Map<String, Config> configSlices = new HashMap<>(nodeAddrs.size());
		sliceConfig(actualConfig, nodeAddrs, configSlices);

		nodeAddrs.forEach(
			nodeAddrWithPort -> {

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
					return;
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
					return;
				}

				StepService stepSvc;
				try {
					stepSvc = ServiceUtil.resolve(nodeAddrWithPort, stepSvcName);
				} catch(final Exception e) {
					LogUtil.exception(
						Level.WARN, e, "Failed to resolve the service \"{}\" @ {}",
						StepManagerService.SVC_NAME, nodeAddrWithPort
					);
					return;
				}

				stepSvcs.add(stepSvc);
			}
		);
	}

	protected abstract void doStartLocal(final Config actualConfig);

	@Override
	protected void doStop() {
		if(distributedFlag) {
			stepSvcs
				.parallelStream()
				.forEach(
					stepSvc -> {
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
					}
				);
		}
	}

	@Override
	protected void doClose() {
		if(distributedFlag) {
			stepSvcs
				.parallelStream()
				.forEach(
					stepSvc -> {
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
					}
				);
		}
		stepSvcs.clear();
	}

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

	protected void sliceConfig(
		final Config config, final List<String> nodeAddrs, final Map<String, Config> configSlices
	) {

		nodeAddrs.forEach(
			nodeAddrWithPort -> {
				// disable the distributed mode flag
				final Config configSlice = new Config(config);
				configSlice.getTestConfig().getStepConfig().setDistributed(false);
				configSlices.put(nodeAddrWithPort, configSlice);
			}
		);

		final ItemConfig itemConfig = config.getItemConfig();

		// slice the item input file (if any)
		final InputConfig itemInputConfig = itemConfig.getInputConfig();
		final int batchSize = config.getLoadConfig().getBatchConfig().getSize();
		final String itemInputFile = itemInputConfig.getFile();

		if(itemInputFile != null && !itemInputFile.isEmpty()) {

			final int nodeCount = nodeAddrs.size();
			final Map<String, FileService> fileSvcs = new HashMap<>(nodeCount);
			nodeAddrs.forEach(
				nodeAddrWithPort -> {
					try {
						final FileManagerService fileMgrSvc = ServiceUtil.resolve(
							nodeAddrWithPort, FileManagerService.SVC_NAME
						);
						try {
							final String fileSvcName = fileMgrSvc.getFileService(null);
							try {
								final FileService fileSvc = ServiceUtil.resolve(
									nodeAddrWithPort, fileSvcName
								);
								fileSvcs.put(nodeAddrWithPort, fileSvc);
							} catch(final NotBoundException | RemoteException e) {
								LogUtil.exception(
									Level.ERROR, e,
									"Failed to communicate the file service \"{}\" @ {}",
									fileSvcName, nodeAddrWithPort
								);
							} catch(final MalformedURLException | URISyntaxException e) {
								e.printStackTrace();
							}
						} catch(final IOException e) {

						}
					} catch(final NotBoundException | RemoteException e) {
						LogUtil.exception(
							Level.ERROR, e, "Failed to communicate the file manage service @ {}",
							nodeAddrWithPort
						);
					} catch(final MalformedURLException | URISyntaxException e) {
						e.printStackTrace();
					}
				}
			);

			final List<? extends Item> itemsBuff = new ArrayList<>(batchSize);
			final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
			final ItemFactory<? extends Item> itemFactory = ItemType.getItemFactory(itemType);
			int n;

			try(
				final Input<? extends Item> itemInput = new CsvFileItemInput<>(
					Paths.get(itemInputFile), itemFactory
				)
			) {
				n = itemInput.get((List) itemsBuff, batchSize);
				if(n > 0) {

				}
			} catch(final NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "Failed to use the item input file \"{}\"", itemInputFile
				);
			}
		}
	}
}
