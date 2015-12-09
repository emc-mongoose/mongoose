package com.emc.mongoose.core.api.load.balancer;
/**
 Created by kurila on 08.12.15.
 */
public interface Balancer<S> {
	//
	void markTaskStart(final S subject)
	throws NullPointerException;
	//
	void markTasksStart(final S subject, final int n)
	throws NullPointerException;
	//
	void markTaskFinish(final S subject)
	throws NullPointerException;
	//
	void markTasksFinish(final S subject, final int n)
	throws NullPointerException;
	//
	S getNext();
}
