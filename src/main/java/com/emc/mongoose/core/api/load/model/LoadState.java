package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
//
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
/**
 * Created by gusakk on 19.06.15.
 */
public interface LoadState<T extends DataItem>
extends Serializable {
	//
	int getLoadNumber();
	//
	RunTimeConfig getRunTimeConfig();
	//
	IOStats.Snapshot getStatsSnapshot();
	//
	T getLastDataItem();
	//
	long getLoadElapsedTimeValue();
	//
	TimeUnit getLoadElapsedTimeUnit();
	//
	boolean isLoadFinished(final RunTimeConfig rtConfig);
	//
	interface Builder<T extends DataItem, U extends LoadState<T>> {
		//
		Builder<T, U> setLoadNumber(final int loadNumber);
		//
		Builder<T, U> setRunTimeConfig(final RunTimeConfig runTimeConfig);
		//
		Builder<T, U> setStatsSnapshot(final IOStats.Snapshot ioStatsSnapshot);
		//
		Builder<T, U> setLoadElapsedTimeValue(final long timeValue);
		//
		Builder<T, U> setLastDataItem(final T dataItem);
		//
		Builder<T, U> setLoadElapsedTimeUnit(final TimeUnit timeUnit);
		//
		U build();
		//
	}
}
