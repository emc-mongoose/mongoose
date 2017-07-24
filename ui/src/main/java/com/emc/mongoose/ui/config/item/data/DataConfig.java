package com.emc.mongoose.ui.config.item.data;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.ui.config.SizeInBytesDeserializer;
import com.emc.mongoose.ui.config.SizeInBytesSerializer;
import com.emc.mongoose.ui.config.item.data.input.InputConfig;
import com.emc.mongoose.ui.config.item.data.ranges.RangesConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class DataConfig
implements Serializable {

	public static final String KEY_INPUT = "input";
	public static final String KEY_RANGES = "ranges";
	public static final String KEY_SIZE = "size";
	public static final String KEY_VERIFY = "verify";

	public final void setInputConfig(final InputConfig inputConfig) {
		this.inputConfig = inputConfig;
	}

	public final void setRangesConfig(final RangesConfig rangesConfig) {
		this.rangesConfig = rangesConfig;
	}

	public final void setSize(final SizeInBytes size) {
		this.size = size;
	}

	public final void setVerify(final boolean verify) {
		this.verify = verify;
	}

	@JsonProperty(KEY_INPUT) private InputConfig inputConfig;

	@JsonProperty(KEY_RANGES) private RangesConfig rangesConfig;

	@JsonProperty(KEY_SIZE)
	@JsonDeserialize(using = SizeInBytesDeserializer.class)
	@JsonSerialize(using = SizeInBytesSerializer.class)
	private SizeInBytes size;

	@JsonProperty(KEY_VERIFY) private boolean verify;

	public DataConfig() {
	}

	public DataConfig(final DataConfig other) {
		this.inputConfig = new InputConfig(other.getInputConfig());
		this.rangesConfig = new RangesConfig(other.getRangesConfig());
		this.size = new SizeInBytes(other.getSize());
		this.verify = other.getVerify();
	}

	public InputConfig getInputConfig() {
		return inputConfig;
	}

	public final RangesConfig getRangesConfig() {
		return rangesConfig;
	}

	public final SizeInBytes getSize() {
		return size;
	}

	public final boolean getVerify() {
		return verify;
	}
}