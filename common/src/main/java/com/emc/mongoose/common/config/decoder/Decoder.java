package com.emc.mongoose.common.config.decoder;

import javax.json.JsonObject;

/**
 Created on 11.07.16.
 */
public interface Decoder<T> {

	void init();

	void destroy();

	T decode(final JsonObject json) throws DecodeException;

	default String getString(final JsonObject jsonObject, final String key)
	throws DecodeException {
		if (jsonObject.isNull(key)) {
			throw new DecodeException("", "The property '" + key + "' cannot be null");
		} else {
			return jsonObject.getString(key);
		}
	}

	default String getString(final JsonObject jsonObject, final String key, final String defaultValue) {
		return jsonObject.getString(key, defaultValue);
	}
	
}
