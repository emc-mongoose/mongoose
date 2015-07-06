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
	private final TimeUnit timeUnit;
	private final long timeValue;
	private long[] latencyValues;
	//
	public BasicLoadState(final int loadNumber, final RunTimeConfig runTimeConfig,
    final long countSucc, final long countFail, final long countBytes, final long countSubm,
    final long timeValue, final TimeUnit timeUnit, final long[] latencyValues) {
		this.loadNumber = loadNumber;
		this.runTimeConfig = runTimeConfig;
		this.countSucc = countSucc;
		this.countFail = countFail;
		this.countBytes = countBytes;
		this.countSubm = countSubm;
		this.timeUnit = timeUnit;
		this.timeValue = timeValue;
		this.latencyValues = latencyValues;
	}
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
}
