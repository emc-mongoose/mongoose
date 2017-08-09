package com.emc.mongoose.ui.config.load;

import com.emc.mongoose.ui.config.load.batch.BatchConfig;
import com.emc.mongoose.ui.config.load.generator.GeneratorConfig;
import com.emc.mongoose.ui.config.load.rate.RateConfig;
import com.emc.mongoose.ui.config.load.service.ServiceConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class LoadConfig
implements Serializable {

	public static final String KEY_BATCH = "batch";
	public static final String KEY_GENERATOR = "generator";
	public static final String KEY_RATE = "rate";
	public static final String KEY_SERVICE = "service";
	public static final String KEY_TYPE = "type";

	public final void setBatchConfig(final BatchConfig batchConfig) {
		this.batchConfig = batchConfig;
	}

	public final void setGeneratorConfig(final GeneratorConfig generatorConfig) {
		this.generatorConfig = generatorConfig;
	}

	public final void setRateConfig(final RateConfig rateConfig) {
		this.rateConfig = rateConfig;
	}

	public final void setServiceConfig(final ServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public final void setType(final String type) {
		this.type = type;
	}

	@JsonProperty(KEY_BATCH) private BatchConfig batchConfig;
	@JsonProperty(KEY_GENERATOR) private GeneratorConfig generatorConfig;
	@JsonProperty(KEY_RATE) private RateConfig rateConfig;
	@JsonProperty(KEY_SERVICE) private ServiceConfig serviceConfig;
	@JsonProperty(KEY_TYPE) private String type;

	public LoadConfig() {
	}

	public LoadConfig(final LoadConfig other) {
		this.batchConfig = new BatchConfig(other.getBatchConfig());
		this.generatorConfig = new GeneratorConfig(other.getGeneratorConfig());
		this.rateConfig = new RateConfig(other.getRateConfig());
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

	public final RateConfig getRateConfig() {
		return rateConfig;
	}

	public final ServiceConfig getServiceConfig() {
		return serviceConfig;
	}
}