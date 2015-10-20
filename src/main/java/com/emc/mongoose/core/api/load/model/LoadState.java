package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
//
import java.io.Serializable;
/**
 * Created by gusakk on 19.06.15.
 */
public interface LoadState<T extends Item>
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
	boolean isLimitReached(final RunTimeConfig rtConfig);
	//
	interface Builder<T extends Item, U extends LoadState<T>> {
		//
		Builder<T, U> setLoadNumber(final int loadNumber);
		//
		Builder<T, U> setRunTimeConfig(final RunTimeConfig runTimeConfig);
		//
		Builder<T, U> setStatsSnapshot(final IOStats.Snapshot ioStatsSnapshot);
		//
		Builder<T, U> setLastDataItem(final T dataItem);
		//
		U build();
	}
}
