package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.concurrent.AsyncRunnableBase;
import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.item.CsvFileItemInput;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.api.model.svc.Service;
import com.emc.mongoose.api.model.svc.ServiceUtil;
import com.emc.mongoose.load.generator.StorageItemInput;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.data.DataConfig;
import com.emc.mongoose.ui.config.item.data.input.layer.LayerConfig;
import com.emc.mongoose.ui.config.item.input.InputConfig;
import com.emc.mongoose.ui.config.item.naming.NamingConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.node.NodeConfig;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import com.emc.mongoose.ui.log.Loggers;

import com.github.akurilov.commons.func.Function2;
import com.github.akurilov.commons.func.Function3;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.file.BinFileInput;
import com.github.akurilov.commons.net.NetUtil;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
	private Map<String, Optional<FileManagerService>> fileMgrSvcs = null;
	private Map<String, Optional<FileService>> itemInputFileSvcs = null;
	private Map<String, Optional<FileService>> itemOutputFileSvcs = null;
	private Map<String, Optional<FileService>> ioTraceLogFileSvcs = null;

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

		initFileManagerServices(nodeAddrs);
		if(actualConfig.getOutputConfig().getMetricsConfig().getTraceConfig().getPersist()) {
			initIoTraceLogFileServices(nodeAddrs);
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

	private void initFileManagerServices(final List<String> nodeAddrs) {
		fileMgrSvcs = nodeAddrs
			.parallelStream()
			.collect(
				Collectors.toMap(
					Function.identity(),
					nodeAddrWithPort -> {
						try {
							return Optional.of(
								ServiceUtil.resolve(nodeAddrWithPort, FileManagerService.SVC_NAME)
							);
						} catch(final Exception e) {
							LogUtil.exception(
								Level.ERROR, e,
								"Failed to resolve the remote file manager service @ {}",
								nodeAddrWithPort
							);
						}
						return Optional.empty();
					}
				)
			);
	}

	private void initIoTraceLogFileServices(final List<String> nodeAddrs) {
		ioTraceLogFileSvcs = nodeAddrs
			.stream()
			.collect(
				Collectors.toMap(
					Function.identity(),
					nodeAddrWithPort -> fileMgrSvcs
						.get(nodeAddrWithPort)
						.map(
							fileMgrSvc -> {
								try {
									return fileMgrSvc.createLogFileService(
										Loggers.IO_TRACE.getName(), id
									);
								} catch(final RemoteException e) {
									LogUtil.exception(
										Level.WARN, e, "Failed to create the log file service @ {}",
										nodeAddrWithPort
									);
								}
								return null;
							}
						)
						.map(
							ioTraceLogFileSvcName -> {
								try {
									return ServiceUtil.resolve(
										nodeAddrWithPort, ioTraceLogFileSvcName
									);
								} catch(final Exception e) {
									LogUtil.exception(
										Level.WARN, e,
										"Failed to resolve the log file service \"{}\" @ {}",
										ioTraceLogFileSvcName, nodeAddrWithPort
									);
								}
								return null;
							}
						)
				)
			);
	}

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
		try(final Input<? extends Item> itemInput = createItemInput(config, batchSize)) {
			if(itemInput != null) {
				sliceItemInput(itemInput, nodeAddrs, configSlices, batchSize);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to use the item input");
		} catch(final Throwable cause) {
			cause.printStackTrace(System.err);
		}

		// item output file (if any)
		final String itemOutputFile = config.getItemConfig().getOutputConfig().getFile();
		if(itemOutputFile != null && !itemOutputFile.isEmpty()) {
			itemOutputFileSvcs = nodeAddrs
				.parallelStream()
				.collect(
					Collectors.toMap(
						Function.identity(),
						nodeAddrWithPort -> fileMgrSvcs
							.get(nodeAddrWithPort)
							.map(
								Function3.partial13(
									StepBase::createFileService, nodeAddrWithPort, null
								)
							)
							.map(
								Function2
									.partial1(StepBase::resolveService, nodeAddrWithPort)
									.andThen(svc -> (FileService) svc)
							)
							.map(Function2.partial1(StepBase::createRemoteFile, nodeAddrWithPort))
					)
				);
		}

		return configSlices;
	}

	@SuppressWarnings("unchecked")
	private static Input<? extends Item> createItemInput(final Config config, final int batchSize) {

		final ItemConfig itemConfig = config.getItemConfig();
		final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		final ItemFactory<? extends Item> itemFactory = ItemType.getItemFactory(itemType);
		final InputConfig itemInputConfig = itemConfig.getInputConfig();
		final String itemInputFile = itemInputConfig.getFile();

		if(itemInputFile != null && !itemInputFile.isEmpty()) {

			final Path itemInputFilePath = Paths.get(itemInputFile);
			try {
				final String mimeType = Files.probeContentType(itemInputFilePath);
				if(mimeType.startsWith("text")) {
					try {
						return new CsvFileItemInput<>(itemInputFilePath, itemFactory);
					} catch(final NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				} else {
					return new BinFileInput<>(itemInputFilePath);
				}
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "Failed to open the item input file \"{}\"", itemInputFile
				);
			}
		} else {

			final String itemInputPath = itemInputConfig.getPath();
			if(itemInputPath != null && !itemInputPath.isEmpty()) {
				final DataConfig dataConfig = itemConfig.getDataConfig();
				final com.emc.mongoose.ui.config.item.data.input.InputConfig
					dataInputConfig = dataConfig.getInputConfig();
				final LayerConfig dataLayerConfig = dataInputConfig.getLayerConfig();
				try {
					final DataInput dataInput = DataInput.getInstance(
						dataInputConfig.getFile(), dataInputConfig.getSeed(),
						dataLayerConfig.getSize(), dataLayerConfig.getCache()
					);
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
				} catch(final IOException | IllegalStateException | IllegalArgumentException e) {
					LogUtil.exception(Level.WARN, e, "Failed to initialize the data input");
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

		itemInputFileSvcs = nodeAddrs
			.parallelStream()
			.collect(
				Collectors.toMap(
					Function.identity(),
					nodeAddrWithPort -> fileMgrSvcs
						.get(nodeAddrWithPort)
						.map(
							fileMgrSvc -> {
								try {
									return fileMgrSvc.createFileService(null);
								} catch(final RemoteException e) {
									LogUtil.exception(
										Level.WARN, e,
										"Failed to create the remote file service @ {}",
										nodeAddrWithPort
									);
								}
								return null;
							}
						)
						.map(
							Function2
								.partial1(StepBase::resolveService, nodeAddrWithPort)
								.andThen(svc -> (FileService) svc)
						)
						.map(
							fileSvc -> {
								try {
									fileSvc.open(FileService.WRITE_OPEN_OPTIONS);
								} catch(final IOException e) {
									LogUtil.exception(
										Level.WARN, e,
										"Failed to open the remote file for writing @ {}",
										nodeAddrWithPort
									);
								}
								return fileSvc;
							}
						)
				)
			);

		final int nodeCount = nodeAddrs.size();
		final Map<String, ByteArrayOutputStream> itemsDataByNode = nodeAddrs
			.stream()
			.collect(
				Collectors.toMap(
					Function.identity(),
					n -> new ByteArrayOutputStream(batchSize * 0x40)
				)
			);
		final Map<String, ObjectOutputStream> itemsOutByNode = itemsDataByNode
			.keySet()
			.stream()
			.collect(
				Collectors.toMap(
					Function.identity(),
					n -> {
						try {
							return new ObjectOutputStream(itemsDataByNode.get(n));
						} catch(final IOException ignored) {
						}
						return null;
					}
				)
			);
		final List<Item> itemsBuff = new ArrayList<>(batchSize);

		int n;
		ObjectOutputStream out = itemsOutByNode.get(nodeAddrs.get(0));

		while(true) {

			// get the next batch of items
			try {
				n = itemInput.get((List) itemsBuff, batchSize);
			} catch(final EOFException e) {
				break;
			}

			if(n > 0) {

				// convert the items to the text representation
				if(nodeCount > 1) {
					// distribute the items using round robin
					for(int i = 0; i < n; i ++) {
						itemsOutByNode
							.get(nodeAddrs.get(i % nodeCount))
							.writeUnshared(itemsBuff.get(i));
					}
				} else {
					for(int i = 0; i < n; i ++) {
						out.writeUnshared(itemsBuff.get(i));
					}
				}

				itemsBuff.clear();

				// write the text items data to the remote input files
				nodeAddrs
					.parallelStream()
					.forEach(
						nodeAddrWithPort -> {
							final ByteArrayOutputStream buff = itemsDataByNode.get(
								nodeAddrWithPort
							);
							itemInputFileSvcs
								.get(nodeAddrWithPort)
								.ifPresent(
									itemInputFileSvc -> {
										try {
											final byte[] data = buff.toByteArray();
											itemInputFileSvc.write(data);
											buff.reset();
										} catch(final IOException e) {
											LogUtil.exception(
												Level.WARN, e,
												"Failed to write the items input data to the " +
													"remote file @ {}",
												nodeAddrWithPort
											);
										}
									}
								);
						}
					);
			} else {
				break;
			}
		}

		nodeAddrs
			.parallelStream()
			.map(itemsOutByNode::get)
			.filter(Objects::nonNull)
			.forEach(
				o -> {
					try {
						o.close();
					} catch(final IOException ignored) {
					}
				}
			);

		itemInputFileSvcs
			.values()
			.parallelStream()
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(
				fileSvc -> {
					try{
						fileSvc.closeFile();
					} catch(final IOException e) {
						LogUtil.exception(Level.DEBUG, e, "Failed to close the remote file");
					}
				}
			);

		nodeAddrs
			.parallelStream()
			.forEach(
				nodeAddrWithPort -> itemInputFileSvcs
					.get(nodeAddrWithPort)
					.ifPresent(
						fileSvc -> {
							try {
								configSlices
									.get(nodeAddrWithPort)
									.getItemConfig()
									.getInputConfig()
									.setFile(fileSvc.filePath());
							} catch(final RemoteException e) {
								LogUtil.exception(
									Level.WARN, e,
									"Failed to invoke the file service \"{}\" call @ {}",
									fileSvc, nodeAddrWithPort
								);
							}
						}
					)
			);
	}

	private static String createFileService(
		final String nodeAddrWithPort, final FileManagerService fileMgrSvc, final String fileSvcName
	) {
		try {
			return fileMgrSvc.createFileService(fileSvcName);
		} catch(final RemoteException e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to create the file service @{}", nodeAddrWithPort
			);
		}
		return null;
	}

	private static Service resolveService(final String nodeAddrWithPort, final String svcName) {
		try {
			return ServiceUtil.resolve(nodeAddrWithPort, svcName);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to resolve the service @ {}", nodeAddrWithPort
			);
		}
		return null;
	}

	private static FileService createRemoteFile(
		final String nodeAddrWithPort, final FileService fileSvc
	) {
		try {
			fileSvc.open(FileService.WRITE_OPEN_OPTIONS);
			fileSvc.closeFile();
			final String filePath = fileSvc.filePath();
			Loggers.MSG.info(
				"Use temporary remote item output file \"{}\" @ {}",
				filePath, Service.address(fileSvc)
			);
		} catch(final IOException e) {
			LogUtil.exception(
				Level.WARN, e,
				"Failed to create the remote file @ {}",
				nodeAddrWithPort
			);
		}
		return fileSvc;
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
					stepSvc.name()
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
			doCloseRemote();
		} else {
			doCloseLocal();
		}
	}

	private void doCloseRemote() {

		stepSvcs
			.parallelStream()
			.forEach(StepBase::closeStepSvc);
		stepSvcs.clear();
		stepSvcs = null;

		if(itemInputFileSvcs != null) {
			itemInputFileSvcs
				.entrySet()
				.parallelStream()
				.filter(entry -> entry.getValue().isPresent())
				.forEach(entry -> closeFileSvc(entry.getValue().get(), entry.getKey()));
			itemInputFileSvcs.clear();
		}

		if(itemOutputFileSvcs != null) {
			final String itemOutputFile = actualConfig
				.getItemConfig().getOutputConfig().getFile();
			transferItemOutputData(itemOutputFileSvcs, itemOutputFile);
			itemOutputFileSvcs
				.entrySet()
				.parallelStream()
				.filter(entry -> entry.getValue().isPresent())
				.forEach(entry -> closeFileSvc(entry.getValue().get(), entry.getKey()));
			itemOutputFileSvcs.clear();
		}

		if(ioTraceLogFileSvcs != null) {
			ioTraceLogFileSvcs
				.values()
				.parallelStream()
				.filter(Optional::isPresent)
				.map(Optional::get)
				.forEach(StepBase::transferIoTraceData);
			ioTraceLogFileSvcs.clear();
		}
	}

	private static void transferItemOutputData(
		final Map<String, Optional<FileService>> itemOutputFileSvcs, final String itemOutputFile
	) {
		Loggers.MSG.info(
			"Transfer the items output data from the remote nodes to the local file \"{}\"...",
			itemOutputFile
		);
		try(
			final OutputStream out = Files.newOutputStream(
				Paths.get(itemOutputFile), FileService.WRITE_OPEN_OPTIONS
			)
		) {
			itemOutputFileSvcs
				.values()
				.parallelStream()
				.filter(Optional::isPresent)
				.map(Optional::get)
				.forEach(
					fileSvc -> {
						try {
							fileSvc.open(FileService.READ_OPTIONS);
							byte buff[];
							while(true) {
								buff = fileSvc.read();
								synchronized(out) {
									out.write(buff);
								}
							}
						} catch(final EOFException e) {
						} catch(final IOException e) {
							LogUtil.exception(
								Level.WARN, e, "Remote items output file transfer failure"
							);
						}
					}
				);
		} catch(final IOException e) {
			LogUtil.exception(
				Level.ERROR, e, "Failed to open the local file \"{}\" for the items output",
				itemOutputFile
			);
		}
	}

	private static StepService closeStepSvc(final StepService stepSvc) {
		if(stepSvc != null) {
			try {
				stepSvc.close();
			} catch(final Exception e) {
				try {
					LogUtil.exception(
						Level.WARN, e, "Failed to close the step service \"{}\"",
						stepSvc.name()
					);
				} catch(final Exception ignored) {
				}
			}
		}
		return stepSvc;
	}

	private static FileService closeFileSvc(
		final FileService fileSvc, final String nodeAddrWithPort
	) {
		if(fileSvc != null) {
			try {
				fileSvc.close();
			} catch(final IOException e) {
				try {
					LogUtil.exception(
						Level.WARN, e, "Failed to close the file service \"{}\" @ {}",
						fileSvc.name(), nodeAddrWithPort
					);
				} catch(final RemoteException ignored) {
				}
			}
		}
		return fileSvc;
	}

	private static void transferIoTraceData(final FileService ioTraceLogFileSvc) {
		try {
			ioTraceLogFileSvc.open(FileService.READ_OPTIONS);
			byte[] data;
			while(true) {
				data = ioTraceLogFileSvc.read();
				if(data.length == 0) {
					break; // EOF
				}
				Loggers.IO_TRACE.info(new String(data));
			}
		} catch(final RemoteException e) {
			final Throwable cause = e.getCause();
			if(!(cause instanceof EOFException)) {
				LogUtil.exception(
					Level.WARN, e, "Failed to read the data from the remote file"
				);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.ERROR, e, "Unexpected I/O exception");
		} finally {
			try {
				ioTraceLogFileSvc.close();
			} catch(final IOException e) {
				try {
					LogUtil.exception(
						Level.DEBUG, e, "Failed to close the remote file {}",
						ioTraceLogFileSvc.filePath()
					);
				} catch(final RemoteException ignored) {
				}
			}
		}
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
}
