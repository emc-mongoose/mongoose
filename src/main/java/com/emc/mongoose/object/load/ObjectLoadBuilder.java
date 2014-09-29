package com.emc.mongoose.object.load;
//
import com.emc.mongoose.base.load.LoadBuilder;
import com.emc.mongoose.base.load.LoadExecutor;
//
import com.emc.mongoose.object.data.DataObject;
/**
 Created by kurila on 29.09.14.
 */
public interface ObjectLoadBuilder<T extends DataObject, U extends LoadExecutor<T>>
extends LoadBuilder<T, U> {
}
