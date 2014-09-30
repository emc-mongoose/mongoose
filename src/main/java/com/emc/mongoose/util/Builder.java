package com.emc.mongoose.util;
//
import java.io.IOException;
import java.net.URISyntaxException;
/**
 Created by kurila on 09.05.14.
 */
public interface Builder<T> {
	//
	T build()
	throws URISyntaxException, IOException;
	//
}
