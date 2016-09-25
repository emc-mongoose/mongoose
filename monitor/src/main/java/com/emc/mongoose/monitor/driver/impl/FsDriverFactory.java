package com.emc.mongoose.monitor.driver.impl;

import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.util.SizeInBytes;
import com.emc.mongoose.common.pattern.Factory;
import com.emc.mongoose.storage.driver.fs.BasicFileDriver;
import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;

/**
 Created by on 9/21/2016.
 */
public class FsDriverFactory<I extends MutableDataItem, O extends DataIoTask<I>>
implements Factory<Driver<I, O>> {

	private final String runId;
	private final LoadConfig loadConfig;
	private final String srcContainer;
	private final AuthConfig authConfig;
	private final SizeInBytes ioBuffSize;

	public FsDriverFactory(
		final String runId, final AuthConfig authConfig, final LoadConfig loadConfig,
		final String srcContainer, final SizeInBytes ioBuffSize
	) {
		this.runId = runId;
		this.authConfig = authConfig;
		this.loadConfig = loadConfig;
		this.srcContainer = srcContainer;
		this.ioBuffSize = ioBuffSize;
	}

	@Override
	public Driver<I, O> create() {
		return new BasicFileDriver<>(runId, authConfig, loadConfig, srcContainer, ioBuffSize);
	}
}
