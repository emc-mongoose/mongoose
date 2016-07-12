package com.emc.mongoose.common.config.decoder;

import javax.json.JsonObject;

/**
 Created on 11.07.16.
 */
public interface Decoder<T> {

	void init();

	void destroy();

	T decode(final JsonObject json) throws DecodeException;
	
}
