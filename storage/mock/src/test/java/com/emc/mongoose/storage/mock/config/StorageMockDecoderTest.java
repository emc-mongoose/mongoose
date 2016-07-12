package com.emc.mongoose.storage.mock.config;

import com.emc.mongoose.common.config.decoder.Decoder;
import com.emc.mongoose.common.config.reader.ConfigReader;
import org.junit.Test;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 Created on 11.07.16.
 */
public class StorageMockDecoderTest {

	private static String parameterErrorMessage(final String content) {
		return "Wrong " + content + " parameter";
	}

	@Test
	public void shouldCreateConfig() throws Exception {
		final Decoder<StorageMockConfig> storageMockDecoder = new StorageMockDecoder();
		final StorageMockConfig storageMockConfig = ConfigReader.loadConfig(storageMockDecoder);
		assertNotNull(storageMockConfig);
		assertEquals(parameterErrorMessage("headCount"), storageMockConfig.getHeadCount(), 1);
		assertEquals(parameterErrorMessage("capacity"), storageMockConfig.getCapacity(), 1_000_000);
		final StorageMockConfig.ContainerConfig containerConfig = storageMockConfig.container();
		assertEquals(parameterErrorMessage("container.capacity"), containerConfig.getCapacity(), 1_000_000);
		assertEquals(parameterErrorMessage("container.count"), containerConfig.getCountLimit(), 1_000_000);
	}

}
