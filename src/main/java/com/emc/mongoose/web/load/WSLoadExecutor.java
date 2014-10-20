package com.emc.mongoose.web.load;
//
import com.emc.mongoose.object.load.ObjectLoadExecutor;
import com.emc.mongoose.web.data.WSObject;
/**
 Created by kurila on 01.10.14.
 */
public interface WSLoadExecutor<T extends WSObject>
extends ObjectLoadExecutor<T> {
}
