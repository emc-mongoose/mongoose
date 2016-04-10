package com.emc.mongoose.client.api.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.client.api.load.executor.HttpDataLoadClient;
import com.emc.mongoose.core.api.v1.item.data.HttpDataItem;
import com.emc.mongoose.core.api.v1.load.builder.HttpDataLoadBuilder;
import com.emc.mongoose.server.api.load.executor.HttpDataLoadSvc;
/**
 Created by kurila on 01.10.14.
 */
public interface HttpDataLoadBuilderClient<
	T extends HttpDataItem, W extends HttpDataLoadSvc<T>, U extends HttpDataLoadClient<T, W>
> extends HttpDataLoadBuilder<T, U>, DataLoadBuilderClient<T, W, U> {
}
