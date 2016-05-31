package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
/**
 Created by kurila on 21.10.15.
 */
public interface HttpDataIoTask<T extends HttpDataItem>
extends DataIoTask<T>, HttpIoTask<T, HttpDataIoTask<T>> {
}
