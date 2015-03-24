package com.emc.mongoose.client.api.load.executor;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
/**
 Created by andrey on 30.09.14.
 */
public interface ObjectLoadClient<T extends DataObject>
extends LoadClient<T>, ObjectLoadExecutor<T> {
}
