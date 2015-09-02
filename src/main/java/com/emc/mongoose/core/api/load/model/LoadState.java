package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
/**
 * Created by gusakk on 19.06.15.
 */
public interface LoadState extends Serializable {
	//
	int getLoadNumber();
	//
	RunTimeConfig getRunTimeConfig();
	//
	long getCountSucc();
	//
	long getCountFail();
	//
	long getCountBytes();
	//
	long getCountSubm();
	//
	long [] getLatencyValues();
	//
	long [] getDurationValues();
	//
	DataItem getLastDataItem();
	//
	long getLoadElapsedTimeValue();
	//
	TimeUnit getLoadElapsedTimeUnit();
	//
	boolean isLoadFinished(final RunTimeConfig rtConfig);
	//
	interface Builder<T> {
		//
		Builder<T> setLoadNumber(final int loadNumber);
		//
		Builder<T> setRunTimeConfig(final RunTimeConfig runTimeConfig);
		//
		Builder<T> setCountSucc(final long countSucc);
		//
		Builder<T> setCountFail(final long countFail);
		//
		Builder<T> setCountBytes(final long countBytes);
		//
		Builder<T> setCountSubm(final long countSubm);
		//
		Builder<T> setLatencyValues(final long latencyValues[]);
		//
		Builder<T> setDurationValues(final long durationValues[]);
		//
		Builder<T> setLoadElapsedTimeValue(final long timeValue);
		//
		Builder<T> setLastDataItem(final DataItem dataItem);
		//
		Builder<T> setLoadElapsedTimeUnit(final TimeUnit timeUnit);
		//
		T build();
		//
	}
}
