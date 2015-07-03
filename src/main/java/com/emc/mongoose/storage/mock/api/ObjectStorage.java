package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.core.api.data.DataObject;
/**
 Created by kurila on 03.07.15.
 */
public interface ObjectStorage<T extends DataObject>
extends Storage<T> {
	T get(final String id);
}
