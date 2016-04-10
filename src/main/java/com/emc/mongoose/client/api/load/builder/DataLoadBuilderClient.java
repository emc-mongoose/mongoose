package com.emc.mongoose.client.api.load.builder;
//
import com.emc.mongoose.client.api.load.executor.LoadClient;
//
import com.emc.mongoose.core.api.v1.item.data.DataItem;
import com.emc.mongoose.core.api.v1.load.builder.DataLoadBuilder;
import com.emc.mongoose.server.api.load.executor.DataLoadSvc;
/**
 Created by kurila on 20.10.15.
 */
public interface DataLoadBuilderClient<
	T extends DataItem, W extends DataLoadSvc<T>, U extends LoadClient<T, W>
> extends LoadBuilderClient<T, W, U>, DataLoadBuilder<T, U> {
}
