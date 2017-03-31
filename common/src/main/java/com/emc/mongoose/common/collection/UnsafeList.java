package com.emc.mongoose.common.collection;

import java.util.List;

/**
 Created by andrey on 31.03.17.
 */
public interface UnsafeList<T>
extends List<T> {
	void removeRange(int fromIndex, int toIndex);
}
