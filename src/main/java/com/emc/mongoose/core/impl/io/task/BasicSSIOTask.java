package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.core.api.io.conf.SSRequestConfig;
import com.emc.mongoose.core.api.io.task.SSIOTask;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.MutableDataItem;
/**
 Created by kurila on 22.12.15.
 */
public class BasicSSIOTask<
	T extends MutableDataItem, C extends Container<T>, X extends SSRequestConfig<T, C>
> extends BasicDataIOTask<T, C, X>
implements SSIOTask<T> {
	//
	public BasicSSIOTask(final T item, final String nodeAddr, final X reqConfig) {
		super(item, nodeAddr, reqConfig);
	}
	//
}
