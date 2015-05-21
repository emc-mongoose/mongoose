package com.emc.mongoose.common.collections;
//
import com.emc.mongoose.common.collections.AsyncCache;
/**
 Created by andrey on 21.05.15.
 */
public final class AsyncCachePutTask<K, V>
implements Reusable<AsyncCachePutTask<K, V>>, Runnable {
	//
	private AsyncCache<K, V> cache = null;
	private K key = null;
	private V value = null;
	////////////////////////////////////////////////////////////////////////////////////////////
	// Reusable implementation
	////////////////////////////////////////////////////////////////////////////////////////////
	@Override @SuppressWarnings("unchecked")
	public final Reusable<AsyncCachePutTask<K, V>> reuse(final Object... args)
		throws IllegalArgumentException, IllegalStateException {
		if(args != null) {
			if(args.length > 0) {
				cache = (AsyncCache<K, V>) args[0];
			}
			if(args.length > 1) {
				key = (K) args[1];
			}
			if(args.length > 2) {
				value = (V) args[2];
			}
		}
		return this;
	}
	//
	@Override
	public final void release() {
		cache.release(this);
	}
	////////////////////////////////////////////////////////////////////////////////////////////
	// Runnable implementation
	////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void run() {
		cache.put(key, value);
		release();
	}
}
