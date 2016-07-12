package com.emc.mongoose.storage.mock;

import com.emc.mongoose.common.config.CommonConfig;
import com.emc.mongoose.common.config.CommonDecoder;
import com.emc.mongoose.common.config.decoder.Decoder;
import com.emc.mongoose.common.config.reader.ConfigReader;
import com.emc.mongoose.storage.mock.config.StorageMockConfig;
import com.emc.mongoose.storage.mock.config.StorageMockDecoder;
import com.emc.mongoose.storage.mock.http.Nagaina;

/**
 Created on 12.07.16.
 */
public class Main {

	@SuppressWarnings("ConstantConditions")
	public static void main(String[] args) {
		final Decoder<CommonConfig> commonDecoder = new CommonDecoder();
		final CommonConfig commonConfig = ConfigReader.loadConfig(commonDecoder);
		final Decoder<StorageMockConfig> storageMockDecoder = new StorageMockDecoder();
		final StorageMockConfig storageMockConfig = ConfigReader.loadConfig(storageMockDecoder);
		final Nagaina nagaina = new Nagaina(commonConfig, storageMockConfig);
		nagaina.start();
		nagaina.shutdown();
	}

}
