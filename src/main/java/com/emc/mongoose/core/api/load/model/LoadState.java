package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
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
	long[] getLatencyValues();
	//
	TimeUnit getLoadElapsedTimeUnit();
	//
	long getLoadElapsedTimeValue();
}
