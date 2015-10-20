package com.emc.mongoose.client.api.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.builder.WSLoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadBuilderClient<T extends WSObject, U extends LoadExecutor<T>>
extends WSLoadBuilder<T, U>, LoadBuilderClient<T, U> {
}
