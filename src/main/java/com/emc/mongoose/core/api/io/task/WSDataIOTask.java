package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
/**
 Created by kurila on 21.10.15.
 */
public interface WSDataIOTask<T extends HttpDataItem>
extends DataIOTask<T>, WSIOTask<T, WSDataIOTask<T>> {
}
