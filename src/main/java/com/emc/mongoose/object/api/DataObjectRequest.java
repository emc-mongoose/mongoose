package com.emc.mongoose.object.api;
/**
 Created by kurila on 29.09.14.
 */
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.object.data.DataObject;
//
public interface DataObjectRequest<T extends DataObject>
extends Request<T> {
}
