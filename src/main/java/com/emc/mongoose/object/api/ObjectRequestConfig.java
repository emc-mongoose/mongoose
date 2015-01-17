package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.data.DataObject;
/**
 Created by kurila on 29.09.14.
 A request configuration regarding data objects.
 */
public interface ObjectRequestConfig<T extends DataObject>
extends RequestConfig<T> {
	//
	String REL_PKG_PROVIDERS = "provider";
	//
}
