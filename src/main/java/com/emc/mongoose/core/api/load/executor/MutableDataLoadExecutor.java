package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.item.data.MutableDataItem;
import com.emc.mongoose.core.api.load.IoTask;
/**
 Created by andrey on 08.04.16.
 */
public interface MutableDataLoadExecutor<T extends MutableDataItem, A extends IoTask<T>>
extends LoadExecutor<T, A> {
}
