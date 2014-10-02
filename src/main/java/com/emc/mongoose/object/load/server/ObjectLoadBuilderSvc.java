package com.emc.mongoose.object.load.server;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.server.LoadBuilderSvc;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.object.load.ObjectLoadBuilder;
/**
 Created by kurila on 29.09.14.
 */
public interface ObjectLoadBuilderSvc<T extends DataObject, U extends LoadExecutor<T>>
extends LoadBuilderSvc<T, U>, ObjectLoadBuilder<T, U> {
}
