package com.emc.mongoose.core.impl.models;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.models.LoadState;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.TimeUnit;

/**
 * Created by gusakk on 19.06.15.
 */
public class BasicLoadState
implements LoadState {
	//
	private int loadNumber;
	private RunTimeConfig rtConfig;
	private long countSucc;
	private long countFail;
	private TimeUnit timeUnit;
	private long timeValue;
	//
	public BasicLoadState(
		final int loadNumber, final RunTimeConfig runTimeConfig, final long countSucc,
		final long countFail, final TimeUnit timeUnit, final long timeValue
	) {
		this.loadNumber = loadNumber;
		this.rtConfig = runTimeConfig;
		this.countSucc = countSucc;
		this.countFail = countFail;
		this.timeUnit = timeUnit;
		this.timeValue = timeValue;
	}
	//
	@Override
	public void setLoadNumber(int loadNumber) {
		this.loadNumber = loadNumber;
	}
	//
	@Override
	public int getLoadNumber() {
		return loadNumber;
	}
	//
	@Override
	public void setRtConfig(RunTimeConfig rtConfig) {
		this.rtConfig = rtConfig;
	}
	//
	@Override
	public RunTimeConfig getRtConfig() {
		return rtConfig;
	}
	//
	@Override
	public void setCountSucc(long countSucc) {
		this.countSucc = countSucc;
	}
	//
	@Override
	public long getCountSucc() {
		return countSucc;
	}
	//
	@Override
	public void setCountFail(long countFail) {
		this.countFail = countFail;
	}
	//
	@Override
	public long getCountFail() {
		return countFail;
	}
	//
	@Override
	public void setLoadElapsedTimeUnit(TimeUnit unit) {
		this.timeUnit = unit;
	}
	//
	@Override
	public TimeUnit getLoadElapsedTimeUnit() {
		return timeUnit;
	}
	//
	@Override
	public void setLoadElapsedTimeValue(long value) {
		this.timeValue = value;
	}
	//
	@Override
	public long getLoadElapsedTimeValue() {
		return timeValue;
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeInt(loadNumber);
		out.writeLong(countSucc);
		out.writeLong(countFail);
		out.writeLong(timeValue);
		out.writeObject(timeUnit);
		out.writeObject(rtConfig);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		loadNumber = in.readInt();
		countSucc = in.readLong();
		countFail = in.readLong();
		timeValue = in.readLong();
		timeUnit = TimeUnit.class.cast(in.readObject());
		rtConfig = RunTimeConfig.class.cast(rtConfig);
	}
	//
}
