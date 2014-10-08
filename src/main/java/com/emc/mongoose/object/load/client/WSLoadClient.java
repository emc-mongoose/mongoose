package com.emc.mongoose.object.load.client;
//
import com.emc.mongoose.object.data.WSObject;
import com.emc.mongoose.object.load.WSLoadExecutor;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadClient<T extends WSObject>
extends ObjectLoadClient<T>, WSLoadExecutor<T> {
}
