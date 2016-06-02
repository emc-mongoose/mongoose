package com.emc.mongoose.run.scenario.engine;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import static com.emc.mongoose.run.scenario.engine.Scenario.DIR_SCENARIO;
import static java.io.File.separatorChar;
import static org.junit.Assert.*;

/**
 Created by andrey on 11.05.16.
 */
public class ScenarioSchemaTest {
	private final static String ROOT_DIR = System.getProperty("user.dir");
	private final static ObjectMapper OBJ_MAPPER = new ObjectMapper()
		.configure(SerializationFeature.INDENT_OUTPUT, true)
		.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
	@Test
	public void checkAllScenariosMatchSchema()
	throws Exception {
		final URI scenarioSchemaUri = new File(
			ROOT_DIR + separatorChar + DIR_SCENARIO + separatorChar + "scenario-schema.json"
		).toURI();
		final JsonSchema scenarioSchema = JsonSchemaFactory
			.newBuilder().freeze().getJsonSchema(scenarioSchemaUri.toString());
		Files.walkFileTree(
			Paths.get(ROOT_DIR).resolve(DIR_SCENARIO),
			new FileVisitor<Path>() {
				@Override
				public final FileVisitResult preVisitDirectory(
					final Path dir, final BasicFileAttributes attrs
				) throws IOException {
					return FileVisitResult.CONTINUE;
				}
				@Override
				public final FileVisitResult visitFile(
					final Path file, final BasicFileAttributes attrs
				) throws IOException {
					if(file.toString().contains("schema")) {
						return FileVisitResult.CONTINUE;
					}
					final JsonNode scenarioTree = OBJ_MAPPER.readTree(file.toFile());
					try {
						final ProcessingReport report = scenarioSchema.validate(scenarioTree, true);
						if(!report.isSuccess()) {
							System.err.println(report.toString());
							fail("Failed to validate: " + file.toString());
						}
					} catch(final ProcessingException e) {
						e.printStackTrace(System.err);
						fail("Failed to validate: " + file.toString());
					}
					System.out.println("Validation done: " + file);
					return FileVisitResult.CONTINUE;
				}
				@Override
				public final FileVisitResult visitFileFailed(final Path file, final IOException e)
				throws IOException {
					return FileVisitResult.CONTINUE;
				}
				@Override
				public final FileVisitResult postVisitDirectory(final Path dir, final IOException e)
				throws IOException {
					return FileVisitResult.CONTINUE;
				}
			}
		);

	}
}
