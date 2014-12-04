package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.AsyncIOClient;
import com.emc.mongoose.object.data.DataObject;
/**
 Created by kurila on 02.12.14.
 */
public interface ObjectIOClient<T extends DataObject>
extends AsyncIOClient<T> {
}
