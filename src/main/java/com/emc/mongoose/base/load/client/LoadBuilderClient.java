package com.emc.mongoose.base.load.client;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadBuilder;
import com.emc.mongoose.base.load.LoadExecutor;
/**
 Created by andrey on 30.09.14.
 A client-side builder which may build remote (server-size) load executors.
 */
public interface LoadBuilderClient<T extends DataItem, U extends LoadExecutor<T>>
extends LoadBuilder<T, U> {
}
