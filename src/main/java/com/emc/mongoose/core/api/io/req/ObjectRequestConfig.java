package com.emc.mongoose.core.api.io.req;
//
import com.emc.mongoose.core.api.data.DataObject;
/**
 Created by kurila on 29.09.14.
 A request configuration regarding data objects.
 */
public interface ObjectRequestConfig<T extends DataObject>
extends RequestConfig<T> {
	String getIdPrefix();
	ObjectRequestConfig<T> setIdPrefix(final String idPrefix);
}
