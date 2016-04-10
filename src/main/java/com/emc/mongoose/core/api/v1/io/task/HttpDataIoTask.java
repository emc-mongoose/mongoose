package com.emc.mongoose.core.api.v1.io.task;
//
import com.emc.mongoose.core.api.v1.item.data.HttpDataItem;
/**
 Created by kurila on 21.10.15.
 */
public interface HttpDataIoTask<T extends HttpDataItem>
extends DataIoTask<T>, HttpIoTask<T, HttpDataIoTask<T>> {
}
