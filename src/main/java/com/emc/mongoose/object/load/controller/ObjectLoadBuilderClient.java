package com.emc.mongoose.object.load.controller;
//
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.object.load.ObjectLoadBuilder;
import com.emc.mongoose.object.load.ObjectLoadExecutor;
/**
 Created by andrey on 30.09.14.
 */
public interface ObjectLoadBuilderClient<T extends DataObject, U extends ObjectLoadExecutor<T>>
extends ObjectLoadBuilder<T, U> {
}
