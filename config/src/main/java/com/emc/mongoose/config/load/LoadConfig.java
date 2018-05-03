package com.emc.mongoose.config.load;

import com.emc.mongoose.config.load.batch.BatchConfig;
import com.emc.mongoose.config.load.generator.GeneratorConfig;
import com.emc.mongoose.config.load.rate.LimitConfig;
import com.emc.mongoose.config.load.service.ServiceConfig;
import com.emc.mongoose.config.load.batch.BatchConfig;
import com.emc.mongoose.config.load.generator.GeneratorConfig;
import com.emc.mongoose.config.load.rate.LimitConfig;
import com.emc.mongoose.config.load.service.ServiceConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class LoadConfig
implements Serializable {

	public static final String KEY_BATCH = "batch";
	public static final String KEY_GENERATOR = "generator";
	public static final String KEY_LIMIT = "limit";
	public static final String KEY_SERVICE = "service";
	public static final String KEY_TYPE = "type";

	public final void setBatchConfig(final BatchConfig batchConfig) {
		this.batchConfig = batchConfig;
	}

	public final void setGeneratorConfig(final GeneratorConfig generatorConfig) {
		this.generatorConfig = generatorConfig;
	}

	public final void setLimitConfig(final LimitConfig limitConfig) {
		this.limitConfig = limitConfig;
	}

	public final void setServiceConfig(final ServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public final void setType(final String type) {
		this.type = type;
	}

	@JsonProperty(KEY_BATCH) private BatchConfig batchConfig;
	@JsonProperty(KEY_GENERATOR) private GeneratorConfig generatorConfig;
	@JsonProperty(KEY_LIMIT) private LimitConfig limitConfig;
	@JsonProperty(KEY_SERVICE) private ServiceConfig serviceConfig;
	@JsonProperty(KEY_TYPE) private String type;

	public LoadConfig() {
	}

	public LoadConfig(final LoadConfig other) {
		this.batchConfig = new BatchConfig(other.getBatchConfig());
		this.generatorConfig = new GeneratorConfig(other.getGeneratorConfig());
		this.limitConfig = new LimitConfig(other.getLimitConfig());
		this.serviceConfig = new ServiceConfig(other.getServiceConfig());
		this.type = other.getType();
	}

	public final BatchConfig getBatchConfig() {
		return batchConfig;
	}

	public final String getType() {
		return type;
	}

	public final GeneratorConfig getGeneratorConfig() {
		return generatorConfig;
	}

	public final LimitConfig getLimitConfig() {
		return limitConfig;
	}

	public final ServiceConfig getServiceConfig() {
		return serviceConfig;
	}
}
