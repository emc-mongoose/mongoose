package com.emc.mongoose.ui.config;

import com.emc.mongoose.common.api.SizeInBytes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 Created by andrey on 05.07.17.
 */
public final class SizeInBytesDeserializer
extends JsonDeserializer<SizeInBytes> {

	@Override
	public final SizeInBytes deserialize(
		final JsonParser p, final DeserializationContext ctx
	)
	throws IOException {
		return new SizeInBytes(p.getValueAsString());
	}
}
