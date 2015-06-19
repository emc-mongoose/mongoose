package com.emc.mongoose.core.api.models;

import com.emc.mongoose.common.conf.RunTimeConfig;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by gusakk on 19.06.15.
 */
public interface LoadState extends Serializable {
	//
	void setLoadNumber(final int loadNumber);
	//
	int getLoadNumber();
	//
	void setRunTimeConfig(final RunTimeConfig runTimeConfig);
	//
	RunTimeConfig getRunTimeConfig();
	//
	void setCountSucc(final long countSucc);
	//
	long getCountSucc();
	//
	void setCountFail(final long countFail);
	//
	long getCountFail();
	//
	void setLoadElapsedTimeUnit(final TimeUnit unit);
	//
	TimeUnit getLoadElapsedTimeUnit();
	//
	void setLoadElapsedTimeValue(final long value);
	//
	long getLoadElapsedTimeValue();
}
