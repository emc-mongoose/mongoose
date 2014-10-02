package com.emc.mongoose.object.load.client;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.client.LoadBuilderClient;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.object.load.ObjectLoadBuilder;
import com.emc.mongoose.object.load.ObjectLoadExecutor;
/**
 Created by andrey on 30.09.14.
 */
public interface ObjectLoadBuilderClient<T extends DataObject, U extends LoadExecutor<T>>
extends ObjectLoadBuilder<T, U>, LoadBuilderClient<T, U> {
}
