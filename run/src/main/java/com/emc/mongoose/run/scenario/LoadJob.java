package com.emc.mongoose.run.scenario;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.load.generator.BasicLoadGenerator;
import com.emc.mongoose.load.monitor.BasicLoadMonitor;
import com.emc.mongoose.load.monitor.BasicLoadMonitorSvc;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.ContentSourceUtil;
import com.emc.mongoose.model.io.Output;
import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import com.emc.mongoose.model.io.task.BasicMutableDataIoTaskBuilder;
import com.emc.mongoose.model.io.task.IoTaskBuilder;
import com.emc.mongoose.model.item.BasicMutableDataItemFactory;
import com.emc.mongoose.model.item.CsvFileItemOutput;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.model.load.LoadType;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.storage.StorageDriverSvc;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.DriverConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 02.02.16.
 */
public final class LoadJob
extends JobBase {
	
	private final static Logger LOG = LogManager.getLogger();

	private final ContentSource contentSrc;
	private final LoadMonitor monitor;
	private final long timeLimitSec;

	public LoadJob(final Config config) {
		this(config, Collections.EMPTY_MAP, false);
	}
	
	public LoadJob(
		final Config appConfig, final Map<String, Object> subTree, final boolean preconditionFlag
	) {
		super(appConfig);

		final Map<String, Object> nodeConfig = (Map<String, Object>) subTree.get(KEY_NODE_CONFIG);
		if(nodeConfig != null) {
			localConfig.apply(nodeConfig);
		}
		final String jobName = localConfig.getName();
		final LoadConfig loadConfig = localConfig.getLoadConfig();
		loadConfig.getMetricsConfig().setPrecondition(preconditionFlag);

		final ItemConfig itemConfig = localConfig.getItemConfig();
		final DataConfig dataConfig = itemConfig.getDataConfig();
		final ContentConfig contentConfig = dataConfig.getContentConfig();

		try {
			contentSrc = ContentSourceUtil.getInstance(
				contentConfig.getFile(), contentConfig.getSeed(), contentConfig.getRingSize()
			);
		} catch(final IOException e) {
			throw new RuntimeException("Failed to initialize the content source", e);
		}

		final List<StorageDriver> drivers = new ArrayList<>();
		final StorageConfig storageConfig = localConfig.getStorageConfig();
		final DriverConfig driverConfig = storageConfig.getDriverConfig();
		final boolean remoteDriversFlag = driverConfig.getRemote();

		if(remoteDriversFlag) {
			final List<String> driverSvcAddrs = driverConfig.getAddrs();
			for(final String driverSvcAddr : driverSvcAddrs) {
				final StorageDriverBuilderSvc driverBuilderSvc = ServiceUtil.resolve(
					driverSvcAddr, StorageDriverBuilderSvc.SVC_NAME
				);
				LOG.info(
					Markers.MSG, "Connected the service \"{}\" @ {}",
					StorageDriverBuilderSvc.SVC_NAME, driverSvcAddr
				);
				if(driverBuilderSvc == null) {
					LOG.warn(
						Markers.ERR,
						"Failed to resolve the storage driver builder service @ {}",
						driverSvcAddr
					);
					continue;
				}
				try {
					final String driverSvcName = driverBuilderSvc
						.setJobName(jobName)
						.setContentSource(contentSrc)
						.setItemConfig(itemConfig)
						.setLoadConfig(loadConfig)
						.setSocketConfig(localConfig.getSocketConfig())
						.setStorageConfig(storageConfig)
						.buildRemotely();
					final StorageDriverSvc driverSvc = ServiceUtil.resolve(
						driverSvcAddr, driverSvcName
					);
					LOG.info(
						Markers.MSG, "Connected the service \"{}\" @ {}", driverSvcName,
						driverSvcAddr
					);
					if(driverSvc != null) {
						drivers.add(driverSvc);
					} else {
						LOG.warn(
							Markers.ERR, "Failed to resolve the storage driver service @ {}",
							driverSvcAddr
						);
					}
				} catch(final RemoteException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Looks like network failure");
				} catch(final UserShootHisFootException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Looks like configuration failure");
				}
			}
		} else {
			try {
				drivers.add(
					new BasicStorageDriverBuilder<>()
						.setJobName(jobName)
						.setContentSource(contentSrc)
						.setItemConfig(itemConfig)
						.setLoadConfig(loadConfig)
						.setSocketConfig(localConfig.getSocketConfig())
						.setStorageConfig(storageConfig)
						.build()
				);
			} catch(final RemoteException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Looks like network failure");
			} catch(final UserShootHisFootException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Looks like configuration failure");
			}
		}
		LOG.info(Markers.MSG, "Load drivers initialized");

		final IoTaskBuilder ioTaskBuilder;
		final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		if(ItemType.PATH.equals(itemType)) {
			// TODO path I/O tasks factory
			ioTaskBuilder = new BasicIoTaskBuilder();
		} else {
			ioTaskBuilder = new BasicMutableDataIoTaskBuilder<>()
				.setRangesConfig(dataConfig.getRangesConfig());
		}
		ioTaskBuilder.setSrcPath(itemConfig.getInputConfig().getPath());
		ioTaskBuilder.setIoType(LoadType.valueOf(loadConfig.getType().toUpperCase()));

		final LoadConfig.LimitConfig limitConfig = loadConfig.getLimitConfig();
		final long t = limitConfig.getTime();
		timeLimitSec = t > 0 ? t : Long.MAX_VALUE;

		final ItemFactory itemFactory;
		if(ItemType.DATA.equals(itemType)) {
			itemFactory = new BasicMutableDataItemFactory(contentSrc);
			LOG.info(Markers.MSG, "Work on the mutable data items");
		} else {
			// TODO path item factory
			itemFactory = null;
			LOG.info(Markers.MSG, "Work on the path items");
		}

		final List<LoadGenerator> generators = new ArrayList<>();

		try {
			generators.add(
				new BasicLoadGenerator(
					jobName, drivers, itemFactory, ioTaskBuilder, itemConfig, loadConfig
				)
			);
		} catch(final UserShootHisFootException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Looks like configuration failure");
		}
		LOG.info(Markers.MSG, "Load generators initialized");

		monitor = remoteDriversFlag ?
			new BasicLoadMonitorSvc(jobName, generators, drivers, loadConfig) :
			new BasicLoadMonitor(jobName, generators, drivers, loadConfig);

		final String itemOutputFile = itemConfig.getOutputConfig().getFile();
		if(itemOutputFile != null && itemOutputFile.length() > 0) {
			final Path itemOutputPath = Paths.get(itemOutputFile);
			try {
				final Output itemOutput = new CsvFileItemOutput(itemOutputPath, itemFactory);
				monitor.setItemOutput(itemOutput);
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failure");
			}
		}
	}
	
	@Override
	public final void run() {
		try {
			monitor.start();
			if(monitor.await(timeLimitSec, TimeUnit.SECONDS)) {
				LOG.info(Markers.MSG, "Load monitor done");
			} else {
				LOG.info(Markers.MSG, "Load monitor timeout");
			}
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to invoke the remote call");
		} catch(final InterruptedException ignored) {
		}
	}
	
	@Override
	public final String toString() {
		return "singleLoadJobContainer#" + hashCode();
	}
	
	@Override
	public final void close()
	throws IOException {
		monitor.close();
		contentSrc.close();
	}
}
