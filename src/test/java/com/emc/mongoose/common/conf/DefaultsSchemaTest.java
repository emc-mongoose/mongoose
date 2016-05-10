package com.emc.mongoose.common.conf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.jayway.restassured.module.jsv.JsonSchemaValidator;
import org.junit.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.junit.Assert.assertTrue;
/**
 Created by kurila on 10.05.16.
 */
public class DefaultsSchemaTest {

	private final static String ROOT_DIR = System.getProperty("user.dir");

	@Test
	public void checkDefaultsMatchesSchema()
	throws Exception {
		final URL defaultsResource = CLASS_LOADER.getResource("defaults.json");
		assertNotNull(defaultsResource);
		final Path defaultsFilePath = Paths.get(defaultsResource.getFile());
		final StringBuilder strb = new StringBuilder();
		for(final String nextLine : Files.readAllLines(defaultsFilePath, StandardCharsets.UTF_8)) {
			strb.append(nextLine).append('\n');
		}
		final String defaultsText = strb.toString();
		assertFalse(defaultsText.isEmpty());
		final JsonSchemaValidator jsonSchemaValidator = matchesJsonSchemaInClasspath(
			"defaults-schema.json"
		);
		assertTrue(jsonSchemaValidator.matches(defaultsText));
	}

	@Test
	public void checkInvalidDefaultsDoesNotMatchSchema()
	throws Exception {
		final ObjectMapper objectMapper = new ObjectMapper()
			.enable(SerializationFeature.INDENT_OUTPUT);
		final JsonNode rootNode = objectMapper.readTree(CLASS_LOADER.getResource("defaults.json"));
		final ObjectNode itemNode = (ObjectNode) rootNode.get("config").get("item");
		itemNode.put("type", "yohoho");
		final String invalidDefaultsText = objectMapper.writeValueAsString(rootNode);
		final JsonSchemaValidator jsonSchemaValidator = matchesJsonSchemaInClasspath(
			"defaults-schema.json"
		);
		assertFalse(jsonSchemaValidator.matches(invalidDefaultsText));
	}

	@Test
	public void checkDefaultsDoesNotMatchInvalidSchema()
	throws Exception {
		final URL defaultsResource = CLASS_LOADER.getResource("defaults.json");
		assertNotNull(defaultsResource);
		final Path defaultsFilePath = Paths.get(defaultsResource.getFile());
		final StringBuilder strb = new StringBuilder();
		for(final String nextLine : Files.readAllLines(defaultsFilePath, StandardCharsets.UTF_8)) {
			strb.append(nextLine).append('\n');
		}
		final String defaultsText = strb.toString();
		assertFalse(defaultsText.isEmpty());
		final ObjectMapper objectMapper = new ObjectMapper()
			.enable(SerializationFeature.INDENT_OUTPUT);
		final JsonNode rootNode = objectMapper
			.readTree(CLASS_LOADER.getResource("defaults-schema.json"));
		final ObjectNode schemaCircularNode = (ObjectNode) rootNode
			.get("properties").get("config")
			.get("properties").get("load")
			.get("properties").get("circular");
		schemaCircularNode.put("type", "string");
		final String invalidDefaultsSchema = objectMapper.writeValueAsString(rootNode);
		final JsonSchemaValidator jsonSchemaValidator = matchesJsonSchema(invalidDefaultsSchema);
		assertFalse(defaultsText, jsonSchemaValidator.matches(defaultsText));
	}
}
