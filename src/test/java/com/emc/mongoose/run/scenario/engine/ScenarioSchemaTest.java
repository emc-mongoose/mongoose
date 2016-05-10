package com.emc.mongoose.run.scenario.engine;
import com.jayway.restassured.module.jsv.JsonSchemaValidator;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.Assert.assertTrue;
/**
 Created by andrey on 11.05.16.
 */
public class ScenarioSchemaTest {
	private final String ROOT_DIR = System.getProperty("user.dir");
	@Test
	public void checkAllScenariosMatchSchema()
	throws Exception {
		final Path schemaPath = Paths.get(ROOT_DIR)
			.resolve(Scenario.DIR_SCENARIO)
			.resolve("scenario-schema.json");
		assertTrue(Files.exists(schemaPath));
		final JsonSchemaValidator jsonSchemaValidator = JsonSchemaValidator
			.matchesJsonSchema(schemaPath.toFile());
		Files.walkFileTree(
			Paths.get(ROOT_DIR).resolve(Scenario.DIR_SCENARIO),
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
					final StringBuilder strb = new StringBuilder();
					for(final String nextLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
						strb.append(nextLine).append('\n');
					}
					assertTrue(file.toString(), jsonSchemaValidator.matches(strb.toString()));
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
