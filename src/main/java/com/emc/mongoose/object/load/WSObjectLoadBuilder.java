package com.emc.mongoose.object.load;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.object.data.WSDataObject;
/**
 Created by kurila on 01.10.14.
 */
public interface WSObjectLoadBuilder<T extends WSDataObject, U extends LoadExecutor<T>>
extends ObjectLoadBuilder<T, U> {
}
