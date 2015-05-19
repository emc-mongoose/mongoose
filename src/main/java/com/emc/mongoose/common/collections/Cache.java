package com.emc.mongoose.common.collections;
//
import java.util.LinkedHashMap;
import java.util.Map;
/**
 Created by kurila on 06.04.15.
 */
public class Cache<T, V>
extends LinkedHashMap<T, V> {
	//
	private final int capacity;
	//
	public Cache(final int capacity) {
		super(capacity + 1, 1.1f, true);
		this.capacity = capacity;
	}
	//
	@Override
	protected final boolean removeEldestEntry(final Map.Entry eldest) {
		return size() > capacity;
	}
	//
	public final int getCapacity() {
		return capacity;
	}
}
