package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.metrics.average.AverageConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.driver.DriverConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;

import java.util.List;

/**
 Created by kurila on 09.03.17.
 */
public interface StorageDriverUtil {
	
	static void init(
		final List<StorageDriver> drivers, final ItemConfig itemConfig, final LoadConfig loadConfig,
		final AverageConfig avgMetricsConfig, final StorageConfig storageConfig,
		final StepConfig stepConfig, final DataInput contentSrc
	) throws InterruptedException {
		final DriverConfig driverConfig = storageConfig.getDriverConfig();
		final String testStepName = stepConfig.getId();
		try {
			drivers.add(
				new BasicStorageDriverBuilder<>()
					.setTestStepName(testStepName)
					.setItemConfig(itemConfig)
					.setContentSource(contentSrc)
					.setLoadConfig(loadConfig)
					.setAverageConfig(avgMetricsConfig)
					.setStorageConfig(storageConfig)
					.build()
			);
		} catch(final OmgShootMyFootException e) {
			throw new RuntimeException(e);
		}
	}
}
