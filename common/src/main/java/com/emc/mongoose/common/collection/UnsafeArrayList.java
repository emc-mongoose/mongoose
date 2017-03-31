package com.emc.mongoose.common.collection;

import java.util.ArrayList;

/**
 Created by andrey on 31.03.17.
 */
public final class UnsafeArrayList<T>
extends ArrayList<T>
implements UnsafeList<T> {

	public UnsafeArrayList(final int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public final void removeRange(int fromIndex, int toIndex) {
		super.removeRange(fromIndex, toIndex);
	}
}
