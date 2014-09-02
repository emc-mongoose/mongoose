package com.emc.mongoose;
//
import java.io.IOException;
import java.net.URISyntaxException;
/**
 Created by kurila on 09.05.14.
 */
public interface Builder<T> {
	//
	final static String
		MSG_TMPL_NOT_SPECIFIED = "\"{}\" parameter is not specified nor in configuration files neither in command line",
		MSG_TMPL_INVALID_VALUE = "illegal value specified for \"{}\" parameter: {}";
	//
	T build()
	throws URISyntaxException, IOException;
	//
}
