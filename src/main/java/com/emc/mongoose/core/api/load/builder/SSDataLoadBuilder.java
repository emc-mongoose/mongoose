package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.load.executor.SSDataLoadExecutor;
/**
 Created by kurila on 19.12.15.
 */
public interface SSDataLoadBuilder<T extends DataItem, U extends SSDataLoadExecutor<T>>
extends DataLoadBuilder<T, U> {
}
