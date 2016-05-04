package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.common.conf.BasicConfig;
//
import java.util.Collection;
import java.util.Map;
/**
 Created by kurila on 31.07.15.
 */
public interface ObjectContainerMock<T extends MutableDataItemMock>
extends Map<String, T>/*, ItemBuffer<T>*/ {
	//
	String DEFAULT_NAME = BasicConfig.THREAD_CONTEXT.get().getRunName();
	//
	int size();
	//
	T list(final String afterOid, final Collection<T> buffDst, final int limit);
}
