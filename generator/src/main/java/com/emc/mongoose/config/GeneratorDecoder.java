package com.emc.mongoose.config;

import com.emc.mongoose.config.decoder.DecodeException;
import com.emc.mongoose.config.decoder.Decoder;

import javax.json.JsonObject;

/**
 Created on 11.07.16.
 */
public class GeneratorDecoder implements Decoder<GeneratorConfig> {
	
	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}

	@Override
	public GeneratorConfig decode(final JsonObject json)
	throws DecodeException {
		return null;
	}
}
