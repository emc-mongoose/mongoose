package com.emc.mongoose.model.api.load;

/**
 Created by kurila on 14.09.16.
 */
public interface Registry<S> {
	
	void register(S subject)
	throws IllegalStateException;
}
