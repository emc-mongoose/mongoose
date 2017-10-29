package com.emc.mongoose.ui.config.test.step.limit.fail;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 08.07.17.
 */
public final class FailConfig
implements Serializable {

	public static final String KEY_COUNT = "count";
	public static final String KEY_RATE = "rate";

	public final void setCount(final long count) {
		this.count = count;
	}

	public final void setRate(final boolean rateFlag) {
		this.rateFlag = rateFlag;
	}

	@JsonProperty(KEY_COUNT) private long count;
	@JsonProperty(KEY_RATE) private boolean rateFlag;

	public FailConfig() {
	}

	public FailConfig(final FailConfig other) {
		this.count = other.getCount();
		this.rateFlag = other.getRate();
	}

	public final long getCount() {
		return count;
	}

	public final boolean getRate() {
		return rateFlag;
	}
}
