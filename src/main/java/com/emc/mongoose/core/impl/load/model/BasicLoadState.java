package com.emc.mongoose.core.impl.load.model;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.load.model.LoadState;

import java.util.concurrent.TimeUnit;

/**
 * Created by gusakk on 19.06.15.
 */
public class BasicLoadState implements LoadState {
	//
	private final int loadNumber;
	private final RunTimeConfig runTimeConfig;
	private final long countSucc;
	private final long countFail;
	private final long countBytes;
	private final long countSubm;
	private final long timeValue;
	private final TimeUnit timeUnit;
	private long[] latencyValues;
	//
	@Override
	public int getLoadNumber() {
		return loadNumber;
	}
	//
	@Override
	public RunTimeConfig getRunTimeConfig() {
		return runTimeConfig;
	}
	//
	@Override
	public long getCountSucc() {
		return countSucc;
	}
	//
	@Override
	public long getCountFail() {
		return countFail;
	}
	//
	@Override
	public long getCountBytes() {
		return countBytes;
	}
	//
	@Override
	public long getCountSubm() {
		return countSubm;
	}
	//
	@Override
	public long[] getLatencyValues() {
		return latencyValues;
	}
	//
	@Override
	public TimeUnit getLoadElapsedTimeUnit() {
		return timeUnit;
	}
	//
	@Override
	public long getLoadElapsedTimeValue() {
		return timeValue;
	}
	//
	public static class Builder implements LoadState.Builder<BasicLoadState> {
		//
		private int loadNumber;
		private RunTimeConfig runTimeConfig;
		private long countSucc;
		private long countFail;
		private long countBytes;
		private long countSubm;
		private long timeValue;
		private TimeUnit timeUnit;
		private long[] latencyValues;
		//
		@Override
		public Builder setLoadNumber(final int loadNumber) {
			this.loadNumber = loadNumber;
			return this;
		}
		//
		@Override
		public Builder setRunTimeConfig(final RunTimeConfig runTimeConfig) {
			this.runTimeConfig = runTimeConfig;
			return this;
		}
		//
		@Override
		public Builder setCountSucc(final long countSucc) {
			this.countSucc = countSucc;
			return this;
		}
		//
		@Override
		public Builder setCountFail(final long countFail) {
			this.countFail = countFail;
			return this;
		}
		//
		@Override
		public Builder setCountBytes(final long countBytes) {
			this.countBytes = countBytes;
			return this;
		}
		//
		@Override
		public Builder setCountSubm(final long countSubm) {
			this.countSubm = countSubm;
			return this;
		}
		//
		@Override
		public Builder setLoadElapsedTimeValue(final long timeValue) {
			this.timeValue = timeValue;
			return this;
		}
		//
		@Override
		public Builder setLoadElapsedTimeUnit(final TimeUnit timeUnit) {
			this.timeUnit = timeUnit;
			return this;
		}
		//
		@Override
		public Builder setLatencyValues(final long[] latencyValues) {
			this.latencyValues = latencyValues;
			return this;
		}
		//
		@Override
		public BasicLoadState build() {
			return new BasicLoadState(this);
		}
		//
	}
	//
	private BasicLoadState(final Builder builder) {
		this.loadNumber = builder.loadNumber;
		this.runTimeConfig = builder.runTimeConfig;
		this.countSucc = builder.countSucc;
		this.countFail = builder.countFail;
		this.countBytes = builder.countBytes;
		this.countSubm = builder.countSubm;
		this.timeValue = builder.timeValue;
		this.timeUnit = builder.timeUnit;
		this.latencyValues = builder.latencyValues;
	}
	//
}
