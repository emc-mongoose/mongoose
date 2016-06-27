package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.model.metrics.IoStats;
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
	AppConfig getAppConfig();
	//
	IoStats.Snapshot getStatsSnapshot();
	//
	IoStats.Snapshot getMedStatsSnapshot();
	//
	T getLastDataItem();
	//
	boolean isLimitReached(final AppConfig appConfig);
	//
	interface Builder<T extends Item, U extends LoadState<T>> {
		//
		Builder<T, U> setLoadNumber(final int loadNumber);
		//
		Builder<T, U> setAppConfig(final AppConfig appConfig);
		//
		Builder<T, U> setStatsSnapshot(final IoStats.Snapshot ioStatsSnapshot);
		//
		Builder<T, U> setMedStatsSnapshot(final IoStats.Snapshot medIoStatsSnapshot);
		//
		Builder<T, U> setLastDataItem(final T dataItem);
		//
		U build();
	}
}
