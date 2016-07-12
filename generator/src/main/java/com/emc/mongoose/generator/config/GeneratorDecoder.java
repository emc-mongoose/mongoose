package com.emc.mongoose.generator.config;

import com.emc.mongoose.common.config.decoder.DecodeException;
import com.emc.mongoose.common.config.decoder.Decoder;

import javax.json.JsonObject;

/**
 Created on 11.07.16.
 */
public class GeneratorDecoder implements Decoder<GeneratorConfig> {

	@Override
	public GeneratorConfig decode(final JsonObject generatorJson)
	throws DecodeException {
		final JsonObject itemJson = generatorJson.getJsonObject(GeneratorConfig.KEY_ITEM);
		final String type = itemJson.getString(GeneratorConfig.Item.KEY_TYPE);
		final JsonObject dataJson = itemJson.getJsonObject(GeneratorConfig.Item.KEY_DATA);
		final JsonObject contentJson =
			dataJson.getJsonObject(GeneratorConfig.Item.Data.KEY_CONTENT);
		final GeneratorConfig.Item.Data.Content content =
			new GeneratorConfig.Item.Data.Content(
				contentJson.getString(GeneratorConfig.Item.Data.Content.KEY_FILE, null),
				contentJson.getString(GeneratorConfig.Item.Data.Content.KEY_SEED),
				contentJson.getString(GeneratorConfig.Item.Data.Content.KEY_RING_SIZE)
			);
		final GeneratorConfig.Item.Data data = new GeneratorConfig.Item.Data(content,
			dataJson.getInt(GeneratorConfig.Item.Data.KEY_RANGES),
			dataJson.getString(GeneratorConfig.Item.Data.KEY_SIZE),
			dataJson.getBoolean(GeneratorConfig.Item.Data.KEY_VERIFY)
		);
		final JsonObject destinationJson =
			itemJson.getJsonObject(GeneratorConfig.Item.KEY_DESTINATION);
		final GeneratorConfig.Item.Destination destination = new GeneratorConfig.Item.Destination(
			destinationJson.getString(GeneratorConfig.Item.Destination.KEY_CONTAINER, null),
			destinationJson.getString(GeneratorConfig.Item.Destination.KEY_FILE, null)
		);
		final JsonObject sourceJson =
			itemJson.getJsonObject(GeneratorConfig.Item.KEY_SOURCE);
		final GeneratorConfig.Item.Source source = new GeneratorConfig.Item.Source(
			sourceJson.getString(GeneratorConfig.Item.Source.KEY_CONTAINER, null),
			sourceJson.getString(GeneratorConfig.Item.Source.KEY_FILE, null),
			sourceJson.getInt(GeneratorConfig.Item.Source.KEY_BATCH_SIZE)
		);
		final JsonObject namingJson =
			itemJson.getJsonObject(GeneratorConfig.Item.KEY_NAMING);
		final GeneratorConfig.Item.Naming naming = new GeneratorConfig.Item.Naming(
			namingJson.getString(GeneratorConfig.Item.Naming.KEY_TYPE),
			namingJson.getString(GeneratorConfig.Item.Naming.KEY_PREFIX, null),
			namingJson.getInt(GeneratorConfig.Item.Naming.KEY_RADIX),
			namingJson.getInt(GeneratorConfig.Item.Naming.KEY_OFFSET),
			namingJson.getInt(GeneratorConfig.Item.Naming.KEY_LENGTH)
		);
		return new GeneratorConfig(new GeneratorConfig.Item(
			type, data, destination, source, naming
		));
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}
}
