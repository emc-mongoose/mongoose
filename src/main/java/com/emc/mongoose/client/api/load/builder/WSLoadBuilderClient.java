package com.emc.mongoose.client.api.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.client.api.load.executor.WSLoadClient;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.builder.WSLoadBuilder;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadBuilderClient<T extends WSObject, U extends WSLoadClient<T>>
extends WSLoadBuilder<T, U>, DataLoadBuilderClient<T, U> {
}
