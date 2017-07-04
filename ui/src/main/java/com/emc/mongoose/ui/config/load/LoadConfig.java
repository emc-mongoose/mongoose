package com.emc.mongoose.ui.config.load;

import com.emc.mongoose.ui.config.load.batch.BatchConfig;
import com.emc.mongoose.ui.config.load.generator.GeneratorConfig;
import com.emc.mongoose.ui.config.load.queue.QueueConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class LoadConfig
implements Serializable {

	public static final String KEY_BATCH = "batch";
	public static final String KEY_CIRCULAR = "circular";
	public static final String KEY_GENERATOR = "generator";
	public static final String KEY_QUEUE = "queue";
	public static final String KEY_TYPE = "type";

	public final void setBatchConfig(final BatchConfig batchConfig) {
		this.batchConfig = batchConfig;
	}

	public final void setCircular(final boolean circular) {
		this.circular = circular;
	}

	public final void setGeneratorConfig(final GeneratorConfig generatorConfig) {
		this.generatorConfig = generatorConfig;
	}

	public final void setQueueConfig(final QueueConfig queueConfig) {
		this.queueConfig = queueConfig;
	}

	public final void setType(final String type) {
		this.type = type;
	}

	@JsonProperty(KEY_BATCH) private BatchConfig batchConfig;
	@JsonProperty(KEY_CIRCULAR) private boolean circular;
	@JsonProperty(KEY_GENERATOR) private GeneratorConfig generatorConfig;
	@JsonProperty(KEY_QUEUE) private QueueConfig queueConfig;
	@JsonProperty(KEY_TYPE) private String type;

	public LoadConfig() {
	}

	public LoadConfig(final LoadConfig other) {
		this.batchConfig = new BatchConfig(other.batchConfig);
		this.circular = other.circular;
		this.generatorConfig = new GeneratorConfig(other.generatorConfig);
		this.queueConfig = new QueueConfig(other.queueConfig);
		this.type = other.type;
	}

	public final BatchConfig getBatchConfig() {
		return batchConfig;
	}

	public final String getType() {
		return type;
	}

	public final boolean getCircular() {
		return circular;
	}

	public final GeneratorConfig getGeneratorConfig() {
		return generatorConfig;
	}

	public final QueueConfig getQueueConfig() {
		return queueConfig;
	}
}