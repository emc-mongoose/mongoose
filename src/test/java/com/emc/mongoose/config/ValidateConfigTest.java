package com.emc.mongoose.config;

import static com.emc.mongoose.Constants.DIR_CONFIG;
import static com.emc.mongoose.Constants.PATH_DEFAULTS;

import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonValidator;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.net.URL;

/**
 Created by andrey on 18.01.17.
 */
public class ValidateConfigTest {

	@Test
	public final void testDefaultConfig()
	throws Exception {
		final ObjectMapper m = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
		final URL bundledDefaultsUrl = getClass().getResource("/install/" + PATH_DEFAULTS);
		final JsonNode jsonInput = m.readTree(bundledDefaultsUrl);
		final URL bundledValidationSchemaUrl = getClass().getResource(
			"/install/" + DIR_CONFIG + "/config-schema.json"
		);
		final JsonNode jsonSchema = m.readTree(bundledValidationSchemaUrl);
		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		final JsonValidator validator = factory.getValidator();
		final ProcessingReport report = validator.validate(jsonSchema, jsonInput);
		assertTrue(report.toString(), report.isSuccess());
	}
}
