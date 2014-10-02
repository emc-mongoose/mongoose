package com.emc.mongoose.object.load.client;
//
import com.emc.mongoose.base.load.client.LoadClient;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.object.load.ObjectLoadExecutor;
/**
 Created by andrey on 30.09.14.
 */
public interface ObjectLoadClient<T extends DataObject>
extends LoadClient<T>, ObjectLoadExecutor<T> {
}
