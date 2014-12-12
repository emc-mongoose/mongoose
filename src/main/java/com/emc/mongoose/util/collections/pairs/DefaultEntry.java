package com.emc.mongoose.util.collections.pairs;

import java.util.Map;

/**
 * Created by gusakk on 12/12/14.
 */
public class DefaultEntry<K,V> implements Map.Entry<K,V> {

	private final K key;
	private V value;

	public DefaultEntry(final K key, final V value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public V setValue(V value) {
		V oldValue = this.value;
		this.value = value;
		return oldValue;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof Map.Entry)) {
			return false;
		}
		Map.Entry<?,?> converted = (Map.Entry<?,?>) o;
		return (getKey() == null ? converted.getKey() == null : getKey().equals(converted.getKey()))
				&& (getValue() == null ? converted.getValue() == null : getValue().equals(converted.getValue()));
	}

	@Override
	public int hashCode() {
		return (getKey() == null ? 0 : getKey().hashCode()) ^
				(getValue() == null ? 0 : getValue().hashCode());
	}
}
