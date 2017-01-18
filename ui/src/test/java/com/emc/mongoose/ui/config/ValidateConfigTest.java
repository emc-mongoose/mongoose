package com.emc.mongoose.ui.config;

import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonValidator;
import org.junit.Test;

import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jackson.JsonLoader;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.Assert.assertTrue;
/**
 Created by andrey on 18.01.17.
 */
public class ValidateConfigTest {

	@Test
	public final void testDefaultConfig()
	throws Exception {
		final JsonNode jsonInput = JsonLoader.fromPath("../config/defaults.json");
		final JsonNode jsonSchema = JsonLoader.fromPath("../config/config-schema.json");
		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		final JsonValidator validator = factory.getValidator();
		final ProcessingReport report = validator.validate(jsonSchema, jsonInput);
		assertTrue(report.isSuccess());
	}
}
