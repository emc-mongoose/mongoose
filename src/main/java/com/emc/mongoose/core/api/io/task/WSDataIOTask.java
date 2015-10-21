package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.data.WSObject;
/**
 Created by kurila on 21.10.15.
 */
public interface WSDataIOTask<T extends WSObject>
extends DataIOTask<T>, WSIOTask<T, WSDataIOTask<T>> {
}
