package com.emc.mongoose.client.api.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.client.api.load.executor.WSDataLoadClient;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.builder.WSDataLoadBuilder;
/**
 Created by kurila on 01.10.14.
 */
public interface WSDataLoadBuilderClient<T extends WSObject, U extends WSDataLoadClient<T>>
extends WSDataLoadBuilder<T, U>, DataLoadBuilderClient<T, U> {
}
