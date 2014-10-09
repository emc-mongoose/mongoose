package com.emc.mongoose.object.load;
//
import com.emc.mongoose.base.load.StorageNodeExecutor;
import com.emc.mongoose.object.data.DataObject;
/**
 Created by kurila on 09.10.14.
 */
public interface ObjectNodeExecutor<T extends DataObject>
extends StorageNodeExecutor<T> {
}
