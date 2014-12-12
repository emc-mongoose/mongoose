package com.emc.mongoose.util.pool;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gusakk on 12/1/14.
 */
public class CircularConcurrentLinkedQueue<E>
	extends ConcurrentLinkedQueue<E> {
	//
	private final int capacity;
	//
	public CircularConcurrentLinkedQueue(int capacity) {
		super();
		this.capacity = capacity;
	}
	//
	@Override
	public boolean add(E e) {
		if (size() >= capacity) {
			super.poll();
		}
		return super.add(e);
	}
}
