package com.emc.mongoose.config.storage;

import com.emc.mongoose.config.storage.auth.AuthConfig;
import com.emc.mongoose.config.storage.driver.DriverConfig;
import com.emc.mongoose.config.storage.net.NetConfig;
import com.emc.mongoose.config.storage.auth.AuthConfig;
import com.emc.mongoose.config.storage.driver.DriverConfig;
import com.emc.mongoose.config.storage.net.NetConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class StorageConfig
implements Serializable {

	public static final String KEY_AUTH = "auth";
	public static final String KEY_NET = "net";
	public static final String KEY_DRIVER = "driver";

	public final void setAuthConfig(final AuthConfig authConfig) {
		this.authConfig = authConfig;
	}

	public final void setNetConfig(final NetConfig netConfig) {
		this.netConfig = netConfig;
	}

	public final void setDriverConfig(final DriverConfig driverConfig) {
		this.driverConfig = driverConfig;
	}

	@JsonProperty(KEY_AUTH) private AuthConfig authConfig;
	@JsonProperty(KEY_NET) private NetConfig netConfig;
	@JsonProperty(KEY_DRIVER) private DriverConfig driverConfig;

	public StorageConfig() {
	}

	public StorageConfig(final StorageConfig other) {
		this.authConfig = new AuthConfig(other.getAuthConfig());
		this.netConfig = new NetConfig(other.getNetConfig());
		this.driverConfig = new DriverConfig(other.getDriverConfig());
	}

	public AuthConfig getAuthConfig() {
		return authConfig;
	}

	public NetConfig getNetConfig() {
		return netConfig;
	}

	public DriverConfig getDriverConfig() {
		return driverConfig;
	}
}
