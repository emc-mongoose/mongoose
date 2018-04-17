package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.metrics.average.AverageConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;

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
		final var driverConfig = storageConfig.getDriverConfig();
		final var testStepName = stepConfig.getId();
		try {
			drivers.add(
				new BasicStorageDriverBuilder<>()
					.testStepId(testStepName)
					.itemConfig(itemConfig)
					.dataInput(contentSrc)
					.loadConfig(loadConfig)
					.storageConfig(storageConfig)
					.build()
			);
		} catch(final OmgShootMyFootException e) {
			throw new RuntimeException(e);
		}
	}
}
