package com.emc.mongoose.config;

import com.emc.mongoose.config.decoder.DecodeException;
import com.emc.mongoose.config.decoder.Decoder;

import javax.json.JsonObject;

/**
 Created on 11.07.16.
 */
public class StorageMockDecoder implements Decoder<StorageMockConfig> {
	
	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}

	@Override
	public StorageMockConfig decode(final JsonObject json)
	throws DecodeException {
		return null;
	}
}
