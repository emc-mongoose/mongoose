package com.emc.mongoose.common.collections;
//
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 Created by kurila on 06.04.15.
 */
public class AsyncCache<K, V>
extends LinkedHashMap<K, V> {
	//
	private final int capacity;
	private final ExecutorService putExecutor = Executors.newSingleThreadExecutor(
		new NamingWorkerFactory("cachePutWorker")
	);
	private final InstancePool<AsyncCachePutTask>
		putTaskPool = new InstancePool<>(AsyncCachePutTask.class);
	//
	public AsyncCache(final int capacity) {
		super(capacity + 1, 1.1f, true);
		this.capacity = capacity;
	}
	//
	private AsyncCachePutTask getPutTaskInstance(final K key, final V value) {
		return putTaskPool.take(this, key, value);
	}
	//
	public final void release(final AsyncCachePutTask<K, V> putTask) {
		putTaskPool.release(putTask);
	}
	//
	@Override
	public final V put(K key, V value) {
		putExecutor.submit(getPutTaskInstance(key, value));
		return value;
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
