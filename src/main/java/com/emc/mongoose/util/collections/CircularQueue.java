package com.emc.mongoose.util.collections;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gusakk on 12/12/14.
 */
public final class CircularQueue<E>
extends ConcurrentLinkedQueue<E> {
	//
	private final int capacity;
	//
	public CircularQueue(final int capacity) {
		super();
		this.capacity = capacity;
	}
	//
	@Override
	public final boolean add(final E e) {
		if(size() >= capacity) {
			poll();
		}
		return super.add(e);
	}

}
