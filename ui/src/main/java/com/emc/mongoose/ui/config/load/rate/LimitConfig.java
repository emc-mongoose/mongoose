package com.emc.mongoose.ui.config.load.rate;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 08.07.17.
 */
public final class LimitConfig
implements Serializable {

	public static final String KEY_CONCURRENCY = "concurrency";
	public static final String KEY_RATE = "rate";

	public final void setConcurrency(final int concurrency) {
		this.concurrency = concurrency;
	}

	public final void setRate(final double rate) {
		this.rate = rate;
	}

	@JsonProperty(KEY_CONCURRENCY) private int concurrency;
	@JsonProperty(KEY_RATE) private double rate;

	public LimitConfig() {
	}

	public LimitConfig(final LimitConfig other) {
		this.concurrency = other.getConcurrency();
		this.rate = other.getRate();
	}

	public final int getConcurrency() {
		return concurrency;
	}

	public final double getRate() {
		return rate;
	}
}
