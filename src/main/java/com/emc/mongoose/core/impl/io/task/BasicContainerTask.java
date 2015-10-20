package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.io.req.RequestConfig;
/**
 Created by kurila on 20.10.15.
 */
public class BasicContainerTask<T extends DataItem, C extends Container<T>>
extends IOTaskBase<C> {
	//
	public BasicContainerTask(
		final C item, final String nodeAddr, final RequestConfig<C> reqConf
	) {
		super(item, nodeAddr, reqConf);
	}
}
