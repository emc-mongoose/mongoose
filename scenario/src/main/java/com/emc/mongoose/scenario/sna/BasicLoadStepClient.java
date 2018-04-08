package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.metrics.MetricsSnapshot;
import com.emc.mongoose.api.model.concurrent.DaemonBase;
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
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import com.github.akurilov.commons.func.Function2;
import com.github.akurilov.commons.func.Function3;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.file.BinFileInput;
import com.github.akurilov.commons.net.NetUtil;

import org.apache.logging.log4j.Level;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class BasicLoadStepClient
extends DaemonBase
implements LoadStepClient {

	private final LoadStep loadStep;
	private final Config actualConfig;

	public BasicLoadStepClient(final LoadStep loadStep, final Config actualConfig) {
		this.loadStep = loadStep;
		this.actualConfig = actualConfig;
	}

	private List<LoadStepService> stepSvcs = null;
	private Map<String, Optional<FileManagerService>> fileMgrSvcs = null;
	private Map<String, Optional<FileService>> itemInputFileSvcs = null;
	private Map<String, Optional<FileService>> itemOutputFileSvcs = null;
	private Map<String, Optional<FileService>> ioTraceLogFileSvcs = null;

	@Override
	protected final void doStart()
	throws IllegalArgumentException {

		final var nodeConfig = actualConfig.getTestConfig().getStepConfig().getNodeConfig();
		final var nodePort = nodeConfig.getPort();
		final var addPortIfMissingPartialFunc = Function2
			.partial2(NetUtil::addPortIfMissing, nodePort);
		final var nodeAddrs = nodeConfig
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
		final var configSlices = sliceConfigs(actualConfig, nodeAddrs);
		final var resolveStepSvcPartialFunc = Function2.partial1(
			this::resolveStepSvc, configSlices
		);

		stepSvcs = nodeAddrs
			.parallelStream()
			.map(resolveStepSvcPartialFunc)
			.filter(Objects::nonNull)
			.peek(
				stepSvc -> {
					try {
						stepSvc.start();
					} catch(final RemoteException | IllegalStateException e) {
						LogUtil.exception(
							Level.WARN, e, "Failed to start the step service {}",  stepSvc
						);
					}
				}
			)
			.collect(Collectors.toList());
	}

	@Override
	protected final void doShutdown() {
	}

	private Map<String, Config> sliceConfigs(final Config config, final List<String> nodeAddrs) {

		final Map<String, Config> configSlices = nodeAddrs
			.stream()
			.collect(
				Collectors.toMap(
					Function.identity(),
					Function2.partial1(LoadStep::initConfigSlice, config)
				)
			);

		// slice the count limit (if any)
		final var nodeCount = nodeAddrs.size();
		final var countLimit = config.getTestConfig().getStepConfig().getLimitConfig().getCount();
		if(nodeCount > 1 && countLimit > 0) {
			final var countLimitPerNode = (long) Math.ceil(((double) countLimit) / nodeCount);
			var remainingCountLimit = countLimit;
			for(final var nodeAddr : configSlices.keySet()) {
				final var limitConfigSlice = configSlices
					.get(nodeAddr)
					.getTestConfig()
					.getStepConfig()
					.getLimitConfig();
				if(remainingCountLimit > countLimitPerNode) {
					Loggers.MSG.info("Node \"{}\": count limit = {}", nodeAddr, countLimitPerNode);
					limitConfigSlice.setCount(countLimitPerNode);
					remainingCountLimit -= countLimitPerNode;
				} else {
					Loggers.MSG.info("Node \"{}\": count limit = {}", nodeAddr, remainingCountLimit);
					limitConfigSlice.setCount(remainingCountLimit);
					remainingCountLimit = 0;
				}
			}
		}

		// slice an item input (if any)
		final var batchSize = config.getLoadConfig().getBatchConfig().getSize();
		try(final var itemInput = createItemInput(config, batchSize)) {
			if(itemInput != null) {
				sliceItemInput(itemInput, nodeAddrs, configSlices, batchSize);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to use the item input");
		} catch(final Throwable cause) {
			cause.printStackTrace(System.err);
		}

		// item output file (if any)
		final var itemOutputFile = config.getItemConfig().getOutputConfig().getFile();
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
									BasicLoadStepClient::createFileService, nodeAddrWithPort, null
								)
							)
							.map(
								Function2
									.partial1(BasicLoadStepClient::resolveService, nodeAddrWithPort)
									.andThen(svc -> (FileService) svc)
							)
							.map(Function2.partial1(BasicLoadStepClient::createRemoteFile, nodeAddrWithPort))
					)
				);
		}

		return configSlices;
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
			final var filePath = fileSvc.filePath();
			Loggers.MSG.info("Use temporary remote item output file \"{}\"", filePath);
		} catch(final IOException e) {
			LogUtil.exception(
				Level.WARN, e,
				"Failed to create the remote file @ {}",
				nodeAddrWithPort
			);
		}
		return fileSvc;
	}

	private static Input<? extends Item> createItemInput(final Config config, final int batchSize) {

		final var itemConfig = config.getItemConfig();
		final var itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		final ItemFactory<? extends Item> itemFactory = ItemType.getItemFactory(itemType);
		final var itemInputConfig = itemConfig.getInputConfig();
		final var itemInputFile = itemInputConfig.getFile();

		if(itemInputFile != null && !itemInputFile.isEmpty()) {

			final var itemInputFilePath = Paths.get(itemInputFile);
			try {
				final var mimeType = Files.probeContentType(itemInputFilePath);
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

			final var itemInputPath = itemInputConfig.getPath();
			if(itemInputPath != null && !itemInputPath.isEmpty()) {
				final var dataConfig = itemConfig.getDataConfig();
				final var dataInputConfig = dataConfig.getInputConfig();
				final var dataLayerConfig = dataInputConfig.getLayerConfig();
				try {
					final var dataInput = DataInput.getInstance(
						dataInputConfig.getFile(), dataInputConfig.getSeed(),
						dataLayerConfig.getSize(), dataLayerConfig.getCache()
					);
					final var storageDriver = new BasicStorageDriverBuilder<>()
						.setTestStepName(config.getTestConfig().getStepConfig().getId())
						.setItemConfig(itemConfig)
						.setContentSource(dataInput)
						.setLoadConfig(config.getLoadConfig())
						.setStorageConfig(config.getStorageConfig())
						.build();
					final var namingConfig = itemConfig.getNamingConfig();
					final var namingPrefix = namingConfig.getPrefix();
					final var namingRadix = namingConfig.getRadix();
					return new StorageItemInput<>(
						(StorageDriver) storageDriver, batchSize, itemFactory, itemInputPath,
						namingPrefix, namingRadix
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

		itemInputFileSvcs = createOpenItemInputFileServices(nodeAddrs);

		final var itemsDataByNode = nodeAddrs
			.stream()
			.collect(
				Collectors.toMap(
					Function.identity(), n -> new ByteArrayOutputStream(batchSize * 0x40)
				)
			);
		final var itemsOutByNode = itemsDataByNode
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

		transferItemsInputData(nodeAddrs, itemInput, batchSize, itemsDataByNode, itemsOutByNode);

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

	private Map<String, Optional<FileService>> createOpenItemInputFileServices(
		final List<String> nodeAddrs
	) {
		return nodeAddrs
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
								.partial1(BasicLoadStepClient::resolveService, nodeAddrWithPort)
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
	}

	private void transferItemsInputData(
		final List<String> nodeAddrs, final Input<? extends Item> itemInput, final int batchSize,
		final Map<String, ByteArrayOutputStream> itemsDataByNode,
		final Map<String, ObjectOutputStream> itemsOutByNode
	) throws IOException {

		final var nodeCount = nodeAddrs.size();
		final var itemsBuff = new ArrayList<>(batchSize);

		int n;
		var out = itemsOutByNode.get(nodeAddrs.get(0));

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
					for(var i = 0; i < n; i ++) {
						itemsOutByNode
							.get(nodeAddrs.get(i % nodeCount))
							.writeUnshared(itemsBuff.get(i));
					}
				} else {
					for(var i = 0; i < n; i ++) {
						out.writeUnshared(itemsBuff.get(i));
					}
				}

				itemsBuff.clear();

				// write the text items data to the remote input files
				nodeAddrs
					.parallelStream()
					.forEach(
						nodeAddrWithPort -> {
							final var buff = itemsDataByNode.get(nodeAddrWithPort);
							itemInputFileSvcs
								.get(nodeAddrWithPort)
								.ifPresent(
									itemInputFileSvc -> {
										try {
											final var data = buff.toByteArray();
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
	}

	private LoadStepService resolveStepSvc(
		final Map<String, Config> configSlices, final String nodeAddrWithPort
	) {

		LoadStepManagerService stepMgrSvc;
		try {
			stepMgrSvc = ServiceUtil.resolve(
				nodeAddrWithPort, LoadStepManagerService.SVC_NAME
			);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to resolve the service \"{}\" @ {}",
				LoadStepManagerService.SVC_NAME, nodeAddrWithPort
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

		LoadStepService stepSvc;
		try {
			stepSvc = ServiceUtil.resolve(nodeAddrWithPort, stepSvcName);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to resolve the service \"{}\" @ {}",
				LoadStepManagerService.SVC_NAME, nodeAddrWithPort
			);
			return null;
		}

		return stepSvc;
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		if(stepSvcs == null || stepSvcs.size() == 0) {
			throw new IllegalStateException("No step services available");
		}
		final var awaitExecutor = Executors.newFixedThreadPool(
			stepSvcs.size(), new LogContextThreadFactory("remoteStepSvcAwaitWorker", true)
		);
		stepSvcs
			.stream()
			.map(
				stepSvc ->
					(Runnable) () ->
						Function3
							.partial1(BasicLoadStepClient::awaitStepService, stepSvc)
							.apply(timeout, timeUnit)
			)
			.forEach(awaitExecutor::submit);
		awaitExecutor.shutdown();
		return awaitExecutor.awaitTermination(timeout, TimeUnit.SECONDS);
	}

	private static boolean awaitStepService(
		final LoadStepService stepSvc, final long timeout, final TimeUnit timeUnit
	) {
		try {
			long commFailCount = 0;
			while(true) {
				try {
					if(stepSvc.await(timeout, timeUnit)) {
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
						.filter(fileMgrSvc -> ! ServiceUtil.isLocalAddress(nodeAddrWithPort))
						.map(
							fileMgrSvc -> {
								try {
									return fileMgrSvc.createLogFileService(
										Loggers.IO_TRACE.getName(), id()
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

	@Override
	protected final void doStop() {
		stepSvcs
			.parallelStream()
			.forEach(BasicLoadStepClient::stopStepSvc);
	}

	private static LoadStepService stopStepSvc(final LoadStepService stepSvc) {
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

	@Override
	public final void doClose()
	throws RemoteException {

		stepSvcs
			.parallelStream()
			.forEach(BasicLoadStepClient::closeStepSvc);
		stepSvcs.clear();
		stepSvcs = null;

		if(itemInputFileSvcs != null) {
			itemInputFileSvcs
				.entrySet()
				.parallelStream()
				.filter(entry -> entry.getValue().isPresent())
				.forEach(entry -> closeFileSvc(entry.getValue().get(), entry.getKey()));
			itemInputFileSvcs.clear();
			itemInputFileSvcs = null;
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
			itemOutputFileSvcs = null;
		}

		if(ioTraceLogFileSvcs != null) {
			ioTraceLogFileSvcs
				.values()
				.parallelStream()
				.filter(Optional::isPresent)
				.map(Optional::get)
				.forEach(BasicLoadStepClient::transferIoTraceData);
			ioTraceLogFileSvcs.clear();
			ioTraceLogFileSvcs = null;
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
			final var out = Files.newOutputStream(
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

	private static LoadStepService closeStepSvc(final LoadStepService stepSvc) {
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

	@Override
	public final BasicLoadStepClient config(final Map<String, Object> config) {
		return this;
	}

	@Override
	public final String id()
	throws RemoteException {
		return loadStep.id();
	}

	@Override
	public String getTypeName()
	throws RemoteException {
		return loadStep.getTypeName();
	}

	@Override
	public final MetricsSnapshot metricsSnapshot(final int ioTypeCode) {
		return null;
	}

	@Override
	public final List<MetricsSnapshot> remoteMetricsSnapshots(final int ioTypeCode) {
		return stepSvcs
			.stream()
			.map(
				stepSvc -> {
					try {
						return stepSvc.metricsSnapshot(ioTypeCode);
					} catch(final RemoteException e) {
						LogUtil.exception(Level.WARN, e, "Failed to fetch the metrics snapshot");
						return null;
					}
				}
			)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}
}
