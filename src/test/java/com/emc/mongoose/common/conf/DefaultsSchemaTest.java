package com.emc.mongoose.common.conf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.junit.Test;

import java.io.File;

import static com.emc.mongoose.common.conf.Constants.DIR_CONF;
import static java.io.File.separatorChar;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
/**
 Created by kurila on 10.05.16.
 */
public class DefaultsSchemaTest {

	private final static String ROOT_DIR = System.getProperty("user.dir");
	private final static ObjectMapper OBJ_MAPPER = new ObjectMapper()
		.configure(SerializationFeature.INDENT_OUTPUT, true);

	@Test
	public void checkDefaultsMatchesSchema()
	throws Exception {
		final JsonNode defaultsTree = OBJ_MAPPER.readTree(
			new File(ROOT_DIR + separatorChar + DIR_CONF + separatorChar + "defaults.json")
		);
		final JsonNode defaultsSchemaTree = OBJ_MAPPER.readTree(
			new File(ROOT_DIR + separatorChar + DIR_CONF + separatorChar + "defaults-schema.json")
		);
		final JsonSchema defaultsSchema = JsonSchemaFactory
			.newBuilder().freeze().getJsonSchema(defaultsSchemaTree);
		final ProcessingReport report = defaultsSchema.validate(defaultsTree, true);
		assertTrue(report.isSuccess());
	}

	@Test
	public void checkInvalidDefaultsDoesNotMatchSchema()
	throws Exception {
		final JsonNode defaultsTree = OBJ_MAPPER.readTree(
			new File(ROOT_DIR + separatorChar + DIR_CONF + separatorChar + "defaults.json")
		);
		final JsonNode defaultsSchemaTree = OBJ_MAPPER.readTree(
			new File(ROOT_DIR + separatorChar + DIR_CONF + separatorChar + "defaults-schema.json")
		);
		final JsonSchema defaultsSchema = JsonSchemaFactory
			.newBuilder().freeze().getJsonSchema(defaultsSchemaTree);
		final ObjectNode itemNode = (ObjectNode) defaultsTree.get("config").get("item");
		itemNode.put("type", "yohoho");
		final ProcessingReport report = defaultsSchema.validate(defaultsTree, true);
		assertFalse(report.isSuccess());
	}

	@Test
	public void checkDefaultsDoesNotMatchInvalidSchema()
	throws Exception {
		final JsonNode defaultsTree = OBJ_MAPPER.readTree(
			new File(ROOT_DIR + separatorChar + DIR_CONF + separatorChar + "defaults.json")
		);
		final JsonNode defaultsSchemaTree = OBJ_MAPPER.readTree(
			new File(ROOT_DIR + separatorChar + DIR_CONF + separatorChar + "defaults-schema.json")
		);
		final JsonSchema defaultsSchema = JsonSchemaFactory
			.newBuilder().freeze().getJsonSchema(defaultsSchemaTree);
		final ObjectNode schemaCircularNode = (ObjectNode) defaultsSchemaTree
			.get("properties").get("config")
			.get("properties").get("load")
			.get("properties").get("circular");
		schemaCircularNode.put("type", "string");
		final ProcessingReport report = defaultsSchema.validate(defaultsTree, true);
		assertFalse(report.isSuccess());
	}
}
