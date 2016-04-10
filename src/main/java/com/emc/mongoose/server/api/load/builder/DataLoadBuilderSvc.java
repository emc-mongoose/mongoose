package com.emc.mongoose.server.api.load.builder;
//
import com.emc.mongoose.core.api.v1.item.data.DataItem;
import com.emc.mongoose.core.api.v1.load.builder.DataLoadBuilder;
import com.emc.mongoose.server.api.load.executor.DataLoadSvc;
/**
 Created by kurila on 20.10.15.
 */
public interface DataLoadBuilderSvc<T extends DataItem, U extends DataLoadSvc<T>>
extends DataLoadBuilder<T, U>, LoadBuilderSvc<T, U> {
}
