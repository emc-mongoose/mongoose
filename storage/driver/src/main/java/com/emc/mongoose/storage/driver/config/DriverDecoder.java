package com.emc.mongoose.storage.driver.config;

import com.emc.mongoose.common.config.decoder.DecodeException;
import com.emc.mongoose.common.config.decoder.Decoder;

import javax.json.JsonObject;

/**
 Created on 11.07.16.
 */
public class DriverDecoder implements Decoder<DriverConfig> {

	@Override
	public DriverConfig decode(final JsonObject driverJson)
	throws DecodeException {
		final DriverConfig.LoadConfig loadConfig = new DriverConfig.LoadConfig(
			driverJson.getJsonObject(DriverConfig.KEY_LOAD).getInt(
				DriverConfig.LoadConfig.KEY_CONCURRENCY));
		return new DriverConfig(loadConfig);
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}

}
