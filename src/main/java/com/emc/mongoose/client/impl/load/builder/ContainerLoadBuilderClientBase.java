package com.emc.mongoose.client.impl.load.builder;
//
import com.emc.mongoose.client.api.load.builder.ContainerLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.ContainerLoadClient;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.server.api.load.builder.ContainerLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.ContainerLoadSvc;

import java.io.IOException;
/**
 Created by kurila on 21.10.15.
 */
public abstract class ContainerLoadBuilderClientBase<
	T extends DataItem,
	C extends Container<T>,
	W extends ContainerLoadSvc<T, C>,
	U extends ContainerLoadClient<T, C, W>,
	V extends ContainerLoadBuilderSvc<T, C, W>
> extends LoadBuilderClientBase<C, W, U, V>
implements ContainerLoadBuilderClient<T, C, W, U> {
	//
	protected ContainerLoadBuilderClientBase()
	throws IOException {
		super();
	}
	//
	protected ContainerLoadBuilderClientBase(final RunTimeConfig rtConfig)
	throws IOException {
		super(rtConfig);
	}
}
