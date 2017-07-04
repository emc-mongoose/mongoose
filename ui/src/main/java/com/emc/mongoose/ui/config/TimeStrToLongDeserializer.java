package com.emc.mongoose.ui.config;

import com.emc.mongoose.common.api.TimeUtil;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 Created by andrey on 05.07.17.
 */
public final class TimeStrToLongDeserializer
extends JsonDeserializer<Long> {

	@Override
	public final Long deserialize(
		final JsonParser p, final DeserializationContext ctx
	)
	throws IOException {
		return TimeUtil.getTimeInSeconds(p.getValueAsString());
	}
}
