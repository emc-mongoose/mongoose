package com.emc.mongoose.config.item.data.ranges;

import com.emc.mongoose.config.RangeDeserializer;
import com.emc.mongoose.config.RangeSerializer;
import com.emc.mongoose.config.SizeInBytesDeserializer;
import com.emc.mongoose.config.SizeInBytesSerializer;
import com.emc.mongoose.config.RangeDeserializer;
import com.emc.mongoose.config.RangeSerializer;
import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.system.SizeInBytes;
import com.emc.mongoose.config.SizeInBytesDeserializer;
import com.emc.mongoose.config.SizeInBytesSerializer;
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

	public static final String KEY_CONCAT = "concat";
	public static final String KEY_FIXED = "fixed";
	public static final String KEY_RANDOM = "random";
	public static final String KEY_THRESHOLD = "threshold";

	@JsonDeserialize(using = RangeDeserializer.class)
	@JsonSerialize(using = RangeSerializer.class)
	@JsonProperty(KEY_CONCAT) private Range concat;

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
		this.concat = other.getConcat();
		if(this.concat != null) {
			this.concat = new Range(this.concat);
		}
		this.fixed = otherRanges == null ? null : new ArrayList<>(otherRanges);
		this.random = other.getRandom();
		this.threshold = new SizeInBytes(other.getThreshold());
	}

	public final Range getConcat() {
		return concat;
	}

	public final void setConcat(final Range concat) {
		this.concat = concat;
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
