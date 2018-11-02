package com.emc.mongoose.control;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.akurilov.confuse.io.json.TypeNames;

import java.io.IOException;

/**
 @author veronika K. on 02.11.18 */
public class TypeJsonSerializer<T extends Class>
	extends StdSerializer<T> {

	protected TypeJsonSerializer(final T t) {
		super(t);
	}

	@Override
	public void serialize(
		final T value, final JsonGenerator gen, final SerializerProvider provider
	)
	throws IOException {
		if(TypeNames.MAP.containsValue(value)) {
			gen.writeString(TypeNames.MAP.entrySet()
										 .stream()
										 .filter(entry -> value == entry.getValue())
										 .map(entry -> entry.getKey())
										 .findFirst()
										 .get());
		} else {
			gen.writeString(value.toString());
		}
	}
}
