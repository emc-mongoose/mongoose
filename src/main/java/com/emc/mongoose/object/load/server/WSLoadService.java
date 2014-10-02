package com.emc.mongoose.object.load.server;
//
import com.emc.mongoose.object.data.WSDataObject;
import com.emc.mongoose.object.load.ObjectLoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadService<T extends WSDataObject>
extends ObjectLoadExecutor<T> {
}
