package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.IoTask;
/**
 Created by andrey on 09.04.16.
 */
public interface HttpMutableDataLoadExecutor<T extends HttpDataItem, A extends IoTask<T>>
extends MutableDataLoadExecutor<T, A> {
}
