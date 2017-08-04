package com.emc.mongoose.ui.config.test.step.limit;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.ui.config.SizeInBytesDeserializer;
import com.emc.mongoose.ui.config.SizeInBytesSerializer;
import com.emc.mongoose.ui.config.TimeStrToLongDeserializer;
import com.emc.mongoose.ui.config.test.step.limit.fail.FailConfig;

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
	public static final String KEY_FAIL = "fail";
	public static final String KEY_SIZE = "size";
	public static final String KEY_TIME = "time";

	public final void setCount(final long count) {
		this.count = count;
	}

	public final void setFailConfig(final FailConfig failConfig) {
		this.failConfig = failConfig;
	}

	public final void setSize(final SizeInBytes size) {
		this.size = size;
	}

	public final void setTime(final long time) {
		this.time = time;
	}

	@JsonProperty(KEY_COUNT) private long count;

	@JsonProperty(KEY_FAIL) private FailConfig failConfig;

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
		this.failConfig = other.getFailConfig();
		this.size = new SizeInBytes(other.getSize());
	}

	public final long getCount() {
		return count;
	}

	public final FailConfig getFailConfig() {
		return failConfig;
	}

	public final SizeInBytes getSize() {
		return size;
	}

	public final long getTime() {
		return time;
	}
}