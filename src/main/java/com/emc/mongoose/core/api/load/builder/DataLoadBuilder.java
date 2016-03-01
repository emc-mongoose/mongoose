package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;

import java.rmi.RemoteException;
/**
 Created by kurila on 20.10.15.
 */
public interface DataLoadBuilder<T extends DataItem, U extends LoadExecutor<T>>
extends LoadBuilder<T, U> {
	//
	DataLoadBuilder<T, U> setDataSize(final SizeInBytes dataSize)
	throws IllegalArgumentException, RemoteException;
	//
	DataLoadBuilder<T, U> setDataRanges(final DataRangesConfig rangesConfig)
	throws IllegalArgumentException, RemoteException;
}
