package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import java.util.Collection;
import java.util.concurrent.ConcurrentNavigableMap;
/**
 Created by kurila on 31.07.15.
 */
public interface ObjectContainerMock<T extends DataObjectMock>
extends ConcurrentNavigableMap<String, T> {
	//
	String DEFAULT_NAME = RunTimeConfig.getContext().getRunName();
	//
	String getName();
	//
	String list(final String marker, final Collection<T> buffDst, final int maxCount);
}
