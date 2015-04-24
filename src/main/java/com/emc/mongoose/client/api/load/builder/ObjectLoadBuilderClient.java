package com.emc.mongoose.client.api.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.builder.ObjectLoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.data.DataObject;
/**
 Created by andrey on 30.09.14.
 */
public interface ObjectLoadBuilderClient<T extends DataObject, U extends LoadExecutor<T>>
extends ObjectLoadBuilder<T, U>, LoadBuilderClient<T, U> {
}
