package com.emc.mongoose.util.collections;

import java.util.Map;

/**
 * Created by gusakk on 12/12/14.
 */
public final class Pair<K, V>
implements Map.Entry<K, V> {

	private final K key;
	private V value;

	public Pair(final K key, final V value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public final K getKey() {
		return key;
	}

	@Override
	public final V getValue() {
		return value;
	}

	@Override
	public final V setValue(final V value) {
		V oldValue = this.value;
		this.value = value;
		return oldValue;
	}

	@Override
	public final boolean equals(final Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof Map.Entry)) {
			return false;
		}
		final Map.Entry<?, ?> converted = (Map.Entry<?, ?>) o;
		return (getKey() == null ? converted.getKey() == null : getKey().equals(converted.getKey()))
				&& (getValue() == null ? converted.getValue() == null : getValue().equals(converted.getValue()));
	}

	@Override
	public final int hashCode() {
		return (getKey() == null ? 0 : getKey().hashCode()) ^
				(getValue() == null ? 0 : getValue().hashCode());
	}
}
