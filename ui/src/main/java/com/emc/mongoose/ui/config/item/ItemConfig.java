package com.emc.mongoose.ui.config.item;

import com.emc.mongoose.ui.config.item.data.DataConfig;
import com.emc.mongoose.ui.config.item.input.InputConfig;
import com.emc.mongoose.ui.config.item.naming.NamingConfig;
import com.emc.mongoose.ui.config.item.output.OutputConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class ItemConfig
implements Serializable {

	public static final String KEY_TYPE = "type";
	public static final String KEY_DATA = "data";
	public static final String KEY_INPUT = "input";
	public static final String KEY_OUTPUT = "output";
	public static final String KEY_NAMING = "naming";

	public final void setType(final String type) {
		this.type = type;
	}

	public final void setDataConfig(final DataConfig dataConfig) {
		this.dataConfig = dataConfig;
	}

	public final void setInputConfig(final InputConfig inputConfig) {
		this.inputConfig = inputConfig;
	}

	public final void setOutputConfig(final OutputConfig outputConfig) {
		this.outputConfig = outputConfig;
	}

	public final void setNamingConfig(final NamingConfig namingConfig) {
		this.namingConfig = namingConfig;
	}

	@JsonProperty(KEY_TYPE) private String type;
	@JsonProperty(KEY_DATA) private DataConfig dataConfig;
	@JsonProperty(KEY_INPUT) private InputConfig inputConfig;
	@JsonProperty(KEY_OUTPUT) private OutputConfig outputConfig;
	@JsonProperty(KEY_NAMING) private NamingConfig namingConfig;

	public ItemConfig() {
	}

	public ItemConfig(final ItemConfig other) {
		this.type = other.getType();
		this.dataConfig = new DataConfig(other.getDataConfig());
		this.inputConfig = new InputConfig(other.getInputConfig());
		this.outputConfig = new OutputConfig(other.getOutputConfig());
		this.namingConfig = new NamingConfig(other.getNamingConfig());
	}

	public final String getType() {
		return type;
	}

	public final DataConfig getDataConfig() {
		return dataConfig;
	}

	public final InputConfig getInputConfig() {
		return inputConfig;
	}

	public final OutputConfig getOutputConfig() {
		return outputConfig;
	}

	public final NamingConfig getNamingConfig() {
		return namingConfig;
	}
}