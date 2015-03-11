package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.data.DataObject;
/**
 Created by kurila on 29.09.14.
 A request regarding a data object.
 */
public interface DataObjectIOTask<T extends DataObject>
extends IOTask<T> {
}
