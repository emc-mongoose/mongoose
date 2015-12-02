package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
/**
 Created by kurila on 21.10.15.
 */
public interface ContainerIOTask<T extends DataItem, C extends Container<T>>
extends IOTask<C> {
}
