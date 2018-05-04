package com.emc.mongoose.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.akurilov.commons.collection.Range;

import java.io.IOException;

public final class RangeSerializer
extends JsonSerializer<Range> {

	@Override
	public final void serialize(
		final Range value, final JsonGenerator gen, final SerializerProvider serializers
	) throws IOException {
		gen.writeString(value.toString());
	}
}
