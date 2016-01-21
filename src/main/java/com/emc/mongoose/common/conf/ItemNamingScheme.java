package com.emc.mongoose.common.conf;
/**
 Created by kurila on 18.12.15.
 */
public interface ItemNamingScheme {
	//
	enum Type { RANDOM, ASC, DESC }
	//
	long getNext();
}
