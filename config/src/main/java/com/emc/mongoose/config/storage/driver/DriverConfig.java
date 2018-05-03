package com.emc.mongoose.config.storage.driver;

import com.emc.mongoose.config.storage.driver.queue.QueueConfig;
import com.emc.mongoose.config.storage.driver.queue.QueueConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 05.07.17.
 */
public final class DriverConfig
implements Serializable {

	public static final String KEY_QUEUE = "queue";
	public static final String KEY_THREADS = "threads";
	public static final String KEY_TYPE = "type";

	public final void setQueueConfig(final QueueConfig queueConfig) {
		this.queueConfig = queueConfig;
	}

	public final void setThreads(final int count) {
		this.threads = count;
	}

	public final void setType(final String type) {
		this.type = type;
	}

	@JsonProperty(KEY_QUEUE) private QueueConfig queueConfig;
	@JsonProperty(KEY_THREADS) private int threads;
	@JsonProperty(KEY_TYPE) private String type;

	public DriverConfig() {
	}

	public DriverConfig(final DriverConfig other) {
		this.queueConfig = new QueueConfig(other.getQueueConfig());
		this.threads = other.getThreads();
		this.type = other.getType();
	}

	public final QueueConfig getQueueConfig() {
		return queueConfig;
	}

	public final int getThreads() {
		return threads;
	}

	public final String getType() {
		return type;
	}
}
