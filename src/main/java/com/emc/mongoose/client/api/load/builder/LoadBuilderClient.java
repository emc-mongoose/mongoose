package com.emc.mongoose.client.api.load.builder;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
/**
 Created by andrey on 30.09.14.
 A client-side builder which may build remote (server-size) load executors.
 */
public interface LoadBuilderClient<T extends DataItem, U extends LoadExecutor<T>>
extends LoadBuilder<T, U> {
}
