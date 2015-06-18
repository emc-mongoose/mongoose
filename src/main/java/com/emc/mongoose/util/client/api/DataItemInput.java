package com.emc.mongoose.util.client.api;
//
import com.emc.mongoose.core.api.data.DataItem;
/**
 Created by kurila on 17.06.15.
 */
public interface DataItemInput<T extends DataItem> {
	T read();
}
