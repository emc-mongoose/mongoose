package com.emc.mongoose.server.api.load.builder;
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.builder.WSDataLoadBuilder;
import com.emc.mongoose.server.api.load.executor.WSDataLoadSvc;
/**
 Created by kurila on 01.10.14.
 */
public interface WSDataLoadBuilderSvc<T extends HttpDataItem, U extends WSDataLoadSvc<T>>
extends WSDataLoadBuilder<T, U>, DataLoadBuilderSvc<T, U> {
}
