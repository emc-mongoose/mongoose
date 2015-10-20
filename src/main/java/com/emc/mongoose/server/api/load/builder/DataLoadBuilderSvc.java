package com.emc.mongoose.server.api.load.builder;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.builder.DataLoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
/**
 Created by kurila on 20.10.15.
 */
public interface DataLoadBuilderSvc<T extends DataItem, U extends LoadExecutor<T>>
extends DataLoadBuilder<T, U>, LoadBuilderSvc<T, U> {
}
