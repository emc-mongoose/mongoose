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
		final String type = getString(itemJson, GeneratorConfig.ItemConfig.KEY_TYPE);
		final JsonObject dataJson = itemJson.getJsonObject(GeneratorConfig.ItemConfig.KEY_DATA);
		final JsonObject contentJson =
			dataJson.getJsonObject(GeneratorConfig.ItemConfig.DataConfig.KEY_CONTENT);
		final GeneratorConfig.ItemConfig.DataConfig.ContentConfig contentConfig =
			new GeneratorConfig.ItemConfig.DataConfig.ContentConfig(
				getString(contentJson, GeneratorConfig.ItemConfig.DataConfig.ContentConfig.KEY_FILE, null),
				getString(contentJson, GeneratorConfig.ItemConfig.DataConfig.ContentConfig.KEY_SEED),
				getString(contentJson, GeneratorConfig.ItemConfig.DataConfig.ContentConfig.KEY_RING_SIZE)
			);
		final GeneratorConfig.ItemConfig.DataConfig
			dataConfig = new GeneratorConfig.ItemConfig.DataConfig(
			contentConfig,
			dataJson.getInt(GeneratorConfig.ItemConfig.DataConfig.KEY_RANGES, 0),
			getString(dataJson, GeneratorConfig.ItemConfig.DataConfig.KEY_SIZE),
			dataJson.getBoolean(GeneratorConfig.ItemConfig.DataConfig.KEY_VERIFY)
		);
		final JsonObject destinationJson =
			itemJson.getJsonObject(GeneratorConfig.ItemConfig.KEY_DESTINATION);
		final GeneratorConfig.ItemConfig.DestinationConfig
			destinationConfig = new GeneratorConfig.ItemConfig.DestinationConfig(
			getString(destinationJson, GeneratorConfig.ItemConfig.DestinationConfig.KEY_CONTAINER, null),
			getString(destinationJson, GeneratorConfig.ItemConfig.DestinationConfig.KEY_FILE, null)
		);
		final JsonObject sourceJson =
			itemJson.getJsonObject(GeneratorConfig.ItemConfig.KEY_SOURCE);
		final GeneratorConfig.ItemConfig.SourceConfig
			sourceConfig = new GeneratorConfig.ItemConfig.SourceConfig(
			getString(sourceJson, GeneratorConfig.ItemConfig.SourceConfig.KEY_CONTAINER, null),
			getString(sourceJson, GeneratorConfig.ItemConfig.SourceConfig.KEY_FILE, null),
			sourceJson.getInt(GeneratorConfig.ItemConfig.SourceConfig.KEY_BATCH_SIZE)
		);
		final JsonObject namingJson =
			itemJson.getJsonObject(GeneratorConfig.ItemConfig.KEY_NAMING);
		final GeneratorConfig.ItemConfig.NamingConfig
			namingConfig = new GeneratorConfig.ItemConfig.NamingConfig(
			getString(namingJson, GeneratorConfig.ItemConfig.NamingConfig.KEY_TYPE),
			getString(namingJson, GeneratorConfig.ItemConfig.NamingConfig.KEY_PREFIX, null),
			namingJson.getInt(GeneratorConfig.ItemConfig.NamingConfig.KEY_RADIX),
			namingJson.getInt(GeneratorConfig.ItemConfig.NamingConfig.KEY_OFFSET),
			namingJson.getInt(GeneratorConfig.ItemConfig.NamingConfig.KEY_LENGTH)
		);
		return new GeneratorConfig(new GeneratorConfig.ItemConfig(
			type, dataConfig, destinationConfig, sourceConfig, namingConfig
		));
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}
}
