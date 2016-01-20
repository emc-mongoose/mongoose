package com.emc.mongoose.client.api.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.client.api.load.executor.WSDataLoadClient;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.builder.WSDataLoadBuilder;
import com.emc.mongoose.server.api.load.executor.WSDataLoadSvc;
/**
 Created by kurila on 01.10.14.
 */
public interface WSDataLoadBuilderClient<
	T extends HttpDataItem, W extends WSDataLoadSvc<T>, U extends WSDataLoadClient<T, W>
> extends WSDataLoadBuilder<T, U>, DataLoadBuilderClient<T, W, U> {
}
