package com.emc.mongoose.client.api.load.executor;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
/**
 Created by kurila on 21.10.15.
 */
public interface ContainerLoadClient<T extends DataItem, C extends Container<T>>
extends LoadClient<C> {
}
