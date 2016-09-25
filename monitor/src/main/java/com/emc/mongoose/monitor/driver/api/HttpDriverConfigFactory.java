package com.emc.mongoose.monitor.driver.api;

import com.emc.mongoose.ui.config.Config.SocketConfig;

/**
 Created on 25.09.16.
 */
public interface HttpDriverConfigFactory
extends DriverConfigFactory {

	SocketConfig getSocketConfig();

}
