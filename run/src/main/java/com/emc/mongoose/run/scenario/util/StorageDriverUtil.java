package com.emc.mongoose.run.scenario.util;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.storage.StorageDriverSvc;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.DriverConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig.MetricsConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.util.List;

/**
 Created by kurila on 09.03.17.
 */
public interface StorageDriverUtil {
	
	static void init(
		final List<StorageDriver> drivers, final ItemConfig itemConfig, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final StepConfig stepConfig,
		final ContentSource contentSrc
	) {
		
		final Logger log = LogManager.getLogger();
		final DriverConfig driverConfig = storageConfig.getDriverConfig();
		final MetricsConfig metricsConfig = stepConfig.getMetricsConfig();
		final String testStepName = stepConfig.getName();
		final int driverPort = driverConfig.getPort();
		final boolean remoteDriversFlag = driverConfig.getRemote();
		
		if(remoteDriversFlag) {
			final List<String> driverSvcAddrs = driverConfig.getAddrs();
			for(final String driverSvcAddr : driverSvcAddrs) {
				final StorageDriverBuilderSvc driverBuilderSvc;
				if(driverSvcAddr.contains(":")) {
					try {
						driverBuilderSvc = ServiceUtil.resolve(
							driverSvcAddr, StorageDriverBuilderSvc.SVC_NAME
						);
					} catch(final NotBoundException | IOException | URISyntaxException e) {
						LogUtil.exception(
							log, Level.FATAL, e,
							"Failed to resolve the storage driver builder service @{}",
							driverSvcAddr
						);
						return;
					}
				} else {
					try {
						driverBuilderSvc = ServiceUtil.resolve(
							driverSvcAddr, driverPort, StorageDriverBuilderSvc.SVC_NAME
						);
					} catch(final NotBoundException | IOException | URISyntaxException e) {
						LogUtil.exception(
							log, Level.FATAL, e,
							"Failed to resolve the storage driver builder service @{}:{}",
							driverSvcAddr, driverPort
						);
						return;
					}
				}
				log.info(
					Markers.MSG, "Connected the service \"{}\" @ {}",
					StorageDriverBuilderSvc.SVC_NAME, driverSvcAddr
				);
				if(driverBuilderSvc == null) {
					log.warn(
						Markers.ERR,
						"Failed to resolve the storage driver builder service @ {}",
						driverSvcAddr
					);
					continue;
				}
				final String driverSvcName;
				try {
					driverSvcName = driverBuilderSvc
						.setTestStepName(testStepName)
						.setContentSource(contentSrc)
						.setItemConfig(itemConfig)
						.setLoadConfig(loadConfig)
						.setMetricsConfig(metricsConfig)
						.setStorageConfig(storageConfig)
						.buildRemotely();
				} catch(final IOException | UserShootHisFootException e) {
					throw new RuntimeException(e);
				}
				
				final StorageDriverSvc driverSvc;
				if(driverSvcAddr.contains(":")) {
					try {
						driverSvc = ServiceUtil.resolve(driverSvcAddr, driverSvcName);
					} catch(final NotBoundException | IOException | URISyntaxException e) {
						LogUtil.exception(
							log, Level.FATAL, e, "Failed to resolve the storage driver service @{}",
							driverSvcAddr
						);
						return;
					}
				} else {
					try {
						driverSvc = ServiceUtil.resolve(driverSvcAddr, driverPort, driverSvcName);
					} catch(final NotBoundException | IOException | URISyntaxException e) {
						LogUtil.exception(
							log, Level.FATAL, e,
							"Failed to resolve the storage driver service @{}:{}", driverSvcAddr,
							driverPort
						);
						return;
					}
				}
				log.info(
					Markers.MSG, "Connected the service \"{}\" @ {}", driverSvcName,
					driverSvcAddr
				);
				if(driverSvc != null) {
					drivers.add(driverSvc);
				} else {
					log.warn(
						Markers.ERR, "Failed to resolve the storage driver service @ {}",
						driverSvcAddr
					);
				}
			}
		} else {
			try {
				drivers.add(
					new BasicStorageDriverBuilder<>()
						.setTestStepName(testStepName)
						.setItemConfig(itemConfig)
						.setLoadConfig(loadConfig)
						.setMetricsConfig(metricsConfig)
						.setStorageConfig(storageConfig)
						.build()
				);
			} catch(final UserShootHisFootException e) {
				throw new RuntimeException(e);
			}
		}
		log.info(Markers.MSG, "Storage drivers initialized");
	}
}