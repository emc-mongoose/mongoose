package com.emc.mongoose.core.api.io.conf;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
/**
 Created by kurila on 19.12.15.
 */
public interface SSRequestConfig<T extends DataItem, C extends Container<T>>
extends RequestConfig<T, C> {
}
