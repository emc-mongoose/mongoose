package com.emc.mongoose.monitor.driver.impl;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.common.pattern.EnumFactory;
import com.emc.mongoose.monitor.driver.api.HttpDriverConfigFactory;
import com.emc.mongoose.storage.driver.http.s3.HttpS3Driver;
import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.SocketConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 Created by on 9/21/2016.
 */
public class HttpDriverFactory<I extends MutableDataItem, O extends DataIoTask<I>>
implements EnumFactory<Driver<I, O>, HttpDriverFactory.Api> {

	private final HttpDriverConfigFactory configFactory;

	public HttpDriverFactory(final HttpDriverConfigFactory configFactory) {
		this.configFactory = configFactory;
	}

	@Override
	public Driver<I, O> create(final Api type) {
		final Logger log = LogManager.getLogger();
		switch(type) {
			case ATMOS:
				break;
			case SWIFT:
				break;
			case S3:
				try {
					return new HttpS3Driver<>(
						configFactory.getRunId(),
						configFactory.getLoadConfig(),
						configFactory.getStorageConfig(),
						configFactory.getSourceContainer(),
						configFactory.getSocketConfig()
					);
				} catch(final UserShootHisFootException e) {
					log.error(e);
				}
		}
		return null;
	}

	public enum Api {
		S3, SWIFT, ATMOS
	}
}
