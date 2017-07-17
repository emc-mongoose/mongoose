package com.emc.mongoose.ui.config;

import com.emc.mongoose.api.common.SizeInBytes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 Created by andrey on 05.07.17.
 */
public final class SizeInBytesSerializer
extends JsonSerializer<SizeInBytes> {

	@Override
	public final void serialize(
		final SizeInBytes value, final JsonGenerator gen, final SerializerProvider serializers
	)
	throws IOException, JsonProcessingException {
		gen.writeString(value.toString());
	}
}
