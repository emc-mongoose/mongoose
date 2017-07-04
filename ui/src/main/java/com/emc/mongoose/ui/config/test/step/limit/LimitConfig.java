package com.emc.mongoose.ui.config.test.step.limit;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.ui.config.SizeInBytesDeserializer;
import com.emc.mongoose.ui.config.SizeInBytesSerializer;
import com.emc.mongoose.ui.config.TimeStrToLongDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class LimitConfig
implements Serializable {

	public static final String KEY_COUNT = "count";
	public static final String KEY_RATE = "rate";
	public static final String KEY_SIZE = "size";
	public static final String KEY_TIME = "time";

	public final void setCount(final long count) {
		this.count = count;
	}

	public final void setRate(final double rate) {
		this.rate = rate;
	}

	public final void setSize(final SizeInBytes size) {
		this.size = size;
	}

	public final void setTime(final long time) {
		this.time = time;
	}

	@JsonProperty(KEY_COUNT) private long count;

	@JsonProperty(KEY_RATE) private double rate;

	@JsonDeserialize(using = SizeInBytesDeserializer.class)
	@JsonSerialize(using = SizeInBytesSerializer.class)
	@JsonProperty(KEY_SIZE)
	private SizeInBytes size;

	@JsonDeserialize(using = TimeStrToLongDeserializer.class) @JsonProperty(KEY_TIME)
	private long time;

	public LimitConfig() {
	}

	public LimitConfig(final LimitConfig other) {
		this.count = other.getCount();
		this.time = other.getTime();
		this.rate = other.getRate();
		this.size = new SizeInBytes(other.getSize());
	}

	public final long getCount() {
		return count;
	}

	public final double getRate() {
		return rate;
	}

	public final SizeInBytes getSize() {
		return size;
	}

	public final long getTime() {
		return time;
	}
}