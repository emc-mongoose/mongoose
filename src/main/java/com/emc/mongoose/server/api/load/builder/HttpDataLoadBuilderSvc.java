package com.emc.mongoose.server.api.load.builder;
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.builder.HttpDataLoadBuilder;
import com.emc.mongoose.server.api.load.executor.HttpDataLoadSvc;
/**
 Created by kurila on 01.10.14.
 */
public interface HttpDataLoadBuilderSvc<T extends HttpDataItem, U extends HttpDataLoadSvc<T>>
extends HttpDataLoadBuilder<T, U>, DataLoadBuilderSvc<T, U> {
}
