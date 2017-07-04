package com.emc.mongoose.ui.config.item.data.ranges;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.ui.config.SizeInBytesDeserializer;
import com.emc.mongoose.ui.config.SizeInBytesSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 05.07.17.
 */
public final class RangesConfig
implements Serializable {

	public static final String KEY_FIXED = "fixed";
	public static final String KEY_RANDOM = "random";
	public static final String KEY_THRESHOLD = "threshold";

	@JsonProperty(KEY_FIXED) private List<String> fixed;

	@JsonProperty(KEY_RANDOM) private int random;

	@JsonProperty(KEY_THRESHOLD)
	@JsonDeserialize(using = SizeInBytesDeserializer.class)
	@JsonSerialize(using = SizeInBytesSerializer.class)
	private SizeInBytes threshold;

	public RangesConfig() {
	}

	public RangesConfig(final RangesConfig other) {
		final List<String> otherRanges = other.getFixed();
		this.fixed = otherRanges == null ? null : new ArrayList<>(otherRanges);
		this.random = other.getRandom();
		this.threshold = new SizeInBytes(other.getThreshold());
	}

	public final List<String> getFixed() {
		return fixed;
	}

	public final void setFixed(final List<String> fixed) {
		this.fixed = fixed;
	}

	public final int getRandom() {
		return random;
	}

	public final void setRandom(final int random) {
		this.random = random;
	}

	public final SizeInBytes getThreshold() {
		return threshold;
	}

	public final void setThreshold(final SizeInBytes threshold) {
		this.threshold = threshold;
	}
}