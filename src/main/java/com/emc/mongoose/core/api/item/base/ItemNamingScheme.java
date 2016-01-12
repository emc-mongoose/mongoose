package com.emc.mongoose.core.api.item.base;
/**
 Created by kurila on 18.12.15.
 */
public interface ItemNamingScheme {
	//
	enum Type {
		RANDOM, ASC, DESC
	}
	//
	long getNext();
}
