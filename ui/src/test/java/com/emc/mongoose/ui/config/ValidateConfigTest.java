package com.emc.mongoose.ui.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonValidator;
import org.junit.Test;

import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;

import static org.junit.Assert.assertTrue;
/**
 Created by andrey on 18.01.17.
 */
public class ValidateConfigTest {

	@Test
	public final void testDefaultConfig()
	throws Exception {
		final ObjectMapper m = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		final JsonNode jsonInput = m.readTree(new File("../config/defaults.json"));
		final JsonNode jsonSchema = m.readTree(new File("../config/config-schema.json"));
		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		final JsonValidator validator = factory.getValidator();
		final ProcessingReport report = validator.validate(jsonSchema, jsonInput);
		assertTrue(report.isSuccess());
	}
}
