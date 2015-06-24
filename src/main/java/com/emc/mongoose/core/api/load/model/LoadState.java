package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import java.io.Externalizable;
import java.util.concurrent.TimeUnit;
/**
 * Created by gusakk on 19.06.15.
 */
public interface LoadState
extends Externalizable {
	//
	void setLoadNumber(final int loadNumber);
	//
	int getLoadNumber();
	//
	void setConfig(final RunTimeConfig runTimeConfig);
	//
	RunTimeConfig getConfig();
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
