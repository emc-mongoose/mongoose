package com.emc.mongoose.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.github.akurilov.commons.collection.Range;

import java.io.IOException;

public final class RangeDeserializer
extends JsonDeserializer<Range> {

	@Override
	public final Range deserialize(final JsonParser p, final DeserializationContext ctx)
	throws IOException, JsonProcessingException {
		return new Range(p.getValueAsString());
	}
}
