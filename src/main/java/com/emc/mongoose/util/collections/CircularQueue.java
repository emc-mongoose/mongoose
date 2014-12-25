package com.emc.mongoose.util.collections;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gusakk on 12/12/14.
 */
public class CircularQueue<E>
extends ConcurrentLinkedQueue<E> {
	//
	private final int capacity;
	//
	public CircularQueue(int capacity) {
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
