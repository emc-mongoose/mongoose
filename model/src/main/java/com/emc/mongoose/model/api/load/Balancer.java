package com.emc.mongoose.model.api.load;

import com.emc.mongoose.model.api.io.Input;

/**
 Created by kurila on 08.12.15.
 */
public interface Balancer<S>
extends Input<S> {
	
	void lease(final S subject)
	throws NullPointerException;
	
	void leaseBatch(final S subject, final int n)
	throws NullPointerException;
	
	void release(final S subject)
	throws NullPointerException;
	
	void releaseBatch(final S subject, final int n)
	throws NullPointerException;
}
