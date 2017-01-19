package com.emc.mongoose.tests.unit;

import com.emc.mongoose.common.env.PathUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonValidator;
import org.junit.Test;

import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;
/**
 Created by andrey on 18.01.17.
 */
public class ValidateConfigTest {

	@Test
	public final void testDefaultConfig()
	throws Exception {
		final ObjectMapper m = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		final JsonNode jsonInput = m.readTree(
			Paths.get(PathUtil.getBaseDir(), "config", "defaults.json").toFile()
		);
		final JsonNode jsonSchema = m.readTree(
			Paths.get(PathUtil.getBaseDir(), "config", "config-schema.json").toFile()
		);
		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		final JsonValidator validator = factory.getValidator();
		final ProcessingReport report = validator.validate(jsonSchema, jsonInput);
		assertTrue(report.isSuccess());
	}
}
