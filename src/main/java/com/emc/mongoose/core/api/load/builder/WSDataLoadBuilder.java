package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface WSDataLoadBuilder<T extends HttpDataItem, U extends LoadExecutor<T>>
extends DataLoadBuilder<T, U> {
}
