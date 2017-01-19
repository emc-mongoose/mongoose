package com.emc.mongoose.tests.unit;

import com.emc.mongoose.common.env.PathUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

/**
 Created by andrey on 18.01.17.
 */
public class ValidateScenariosTest {

	@Test
	public final void testAllScenarios()
	throws Exception {

		final ObjectMapper m = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		final JsonNode jsonSchema = m.readTree(
			Paths.get(PathUtil.getBaseDir(), "scenario", "schema.json").toFile()
		);
		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		final JsonValidator validator = factory.getValidator();

		final List<Path> scenarioPaths = Files.walk(Paths.get(PathUtil.getBaseDir(), "scenario"))
			.filter(path -> path.toString().endsWith(".json"))
			.filter(path -> !path.endsWith("schema.json") && !path.endsWith("invalid.json"))
			.collect(Collectors.toList());

		JsonNode nextScenario;
		ProcessingReport report;
		for(final Path nextScenarioPath : scenarioPaths) {
			System.out.println("Validating the scenario file: " + nextScenarioPath.toString());
			nextScenario = m.readTree(nextScenarioPath.toFile());
			report = validator.validate(jsonSchema, nextScenario);
			assertTrue(report.toString(), report.isSuccess());
		}
	}
}
