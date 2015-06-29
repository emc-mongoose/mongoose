package com.emc.mongoose.core.impl.models;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.models.LoadState;

import java.util.concurrent.TimeUnit;

/**
 * Created by gusakk on 19.06.15.
 */
public class BasicLoadState implements LoadState {
	//
	private int loadNumber;
	private RunTimeConfig runTimeConfig;
	private long countSucc;
	private long countFail;
	private long countBytes;
	private TimeUnit timeUnit;
	private long timeValue;
	//
	public BasicLoadState(int loadNumber, RunTimeConfig runTimeConfig,
    long countSucc, long countFail, long countBytes, TimeUnit timeUnit, long timeValue) {
		this.loadNumber = loadNumber;
		this.runTimeConfig = runTimeConfig;
		this.countSucc = countSucc;
		this.countFail = countFail;
		this.countBytes = countBytes;
		this.timeUnit = timeUnit;
		this.timeValue = timeValue;
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
	public TimeUnit getLoadElapsedTimeUnit() {
		return timeUnit;
	}
	//
	@Override
	public long getLoadElapsedTimeValue() {
		return timeValue;
	}
}
