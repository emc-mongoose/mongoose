package com.emc.mongoose.object.load.server;
//
import com.emc.mongoose.base.load.server.LoadSvc;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.object.load.ObjectLoadExecutor;
/**
 Created by kurila on 29.09.14.
 */
public interface ObjectLoadSvc<T extends DataObject>
extends ObjectLoadExecutor<T>, LoadSvc<T> {
}
