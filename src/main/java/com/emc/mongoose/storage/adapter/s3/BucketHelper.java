package com.emc.mongoose.storage.adapter.s3;
//
import com.emc.mongoose.core.api.v1.item.container.Container;
import com.emc.mongoose.core.api.v1.item.data.HttpDataItem;
import com.emc.mongoose.core.api.v1.item.data.ContainerHelper;
/**
 Created by kurila on 02.10.14.
 */
public interface BucketHelper<T extends HttpDataItem, C extends Container<T>>
extends ContainerHelper<T, C> {
	String
		URL_ARG_VERSIONING = "versioning",
		URL_ARG_MAX_KEYS = "max-keys",
		URL_ARG_MARKER = "marker";
}
