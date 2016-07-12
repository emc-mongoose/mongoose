package com.emc.mongoose.storage.driver.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 Created on 11.07.16.
 */
public class DriverConfig {

	public static final String KEY_LOAD = "load";

	private final LoadConfig loadConfig;

	public DriverConfig(final LoadConfig loadConfig) {
		this.loadConfig = loadConfig;
	}

	public LoadConfig getLoadConfig() {
		return loadConfig;
	}

	public static class LoadConfig {
		
		public static final String KEY_CONCURRENCY = "concurrency";

		private final int concurrency;

		public LoadConfig(final int concurrency) {
			this.concurrency = concurrency;
		}

		public int getConcurrency() {
			return concurrency;
		}
	}
}
