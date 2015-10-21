package com.emc.mongoose.client.api.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.client.api.load.executor.LoadClient;
//
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
/**
 Created by andrey on 30.09.14.
 A client-side builder which may build remote (server-size) load executors.
 */
public interface LoadBuilderClient<T extends Item, U extends LoadClient<T>>
extends LoadBuilder<T, U> {
}
