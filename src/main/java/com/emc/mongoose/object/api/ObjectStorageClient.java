package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.StorageClient;
import com.emc.mongoose.object.data.DataObject;
/**
 Created by kurila on 02.12.14.
 */
public interface ObjectStorageClient<T extends DataObject>
extends StorageClient<T> {
}
