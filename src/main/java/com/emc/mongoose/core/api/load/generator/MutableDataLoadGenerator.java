package com.emc.mongoose.core.api.load.generator;
//
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.data.MutableDataItem;
/**
 Created by kurila on 12.04.16.
 */
public interface MutableDataLoadGenerator<T extends MutableDataItem, A extends IoTask<T>>
extends LoadGenerator<T, A> {
}
