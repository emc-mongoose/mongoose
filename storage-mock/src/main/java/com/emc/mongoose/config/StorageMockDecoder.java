package com.emc.mongoose.config;

import com.emc.mongoose.config.decoder.DecodeException;
import com.emc.mongoose.config.decoder.Decoder;

import javax.json.JsonObject;

/**
 Created on 11.07.16.
 */
public class StorageMockDecoder implements Decoder<StorageMockConfig> {
	
	@Override
	public StorageMockConfig decode(final JsonObject storageMockJson)
	throws DecodeException {
		final JsonObject containerJson = storageMockJson.getJsonObject(StorageMockConfig.KEY_CONTAINER);
		return new StorageMockConfig(
			storageMockJson.getInt(StorageMockConfig.KEY_HEAD_COUNT),
			storageMockJson.getInt(StorageMockConfig.KEY_CAPACITY),
			new StorageMockConfig.Container(
				containerJson.getInt(StorageMockConfig.Container.KEY_CAPACITY),
				containerJson.getInt(StorageMockConfig.Container.KEY_COUNT_LIMIT)
			)
		);
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}
}
