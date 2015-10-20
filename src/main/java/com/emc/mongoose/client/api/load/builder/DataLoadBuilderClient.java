package com.emc.mongoose.client.api.load.builder;
//
import com.emc.mongoose.client.api.load.executor.LoadClient;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.builder.DataLoadBuilder;
/**
 Created by kurila on 20.10.15.
 */
public interface DataLoadBuilderClient<T extends DataItem, U extends LoadClient<T>>
extends LoadBuilderClient<T, U>, DataLoadBuilder<T, U> {
}
