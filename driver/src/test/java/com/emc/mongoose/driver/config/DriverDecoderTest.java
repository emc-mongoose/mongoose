package com.emc.mongoose.driver.config;

import com.emc.mongoose.common.config.reader.ConfigReader;
import com.emc.mongoose.driver.config.DriverConfig;
import com.emc.mongoose.driver.config.DriverDecoder;
import org.junit.Test;

import javax.json.JsonObject;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 Created on 11.07.16.
 */
public class DriverDecoderTest {

	private static String parameterErrorMessage(final String content) {
		return "Wrong " + content + " parameter";
	}

	@Test
	public void shouldCreateConfig() throws Exception{
		final DriverDecoder driverDecoder = new DriverDecoder();
		final JsonObject defaults = ConfigReader.readJson("defaults.json");
		assertNotNull("The configuration file was read wrong", defaults);
		final DriverConfig driverConfig =
			driverDecoder.decode(defaults);
		assertEquals(parameterErrorMessage("load.concurrency"),
			driverConfig.getLoadConfig().getConcurrency(), 1);
		final DriverConfig.StorageConfig storage = driverConfig.getStorageConfig();
		assertEquals(parameterErrorMessage("storage.addrs"),
			storage.getAddresses().get(0), "127.0.0.1");
		final DriverConfig.StorageConfig.AuthConfig auth = storage.getAuthConfig();
		assertNull(parameterErrorMessage("storage.auth.id"), auth.getId());
		assertNull(parameterErrorMessage("storage.auth.secret"), auth.getSecret());
		assertNull(parameterErrorMessage("storage.auth.token"), auth.getToken());
		final DriverConfig.StorageConfig.HttpConfig http = storage.getHttpConfig();
		assertEquals(parameterErrorMessage("storage.http.api"), http.getApi(), "S3");
		assertEquals(parameterErrorMessage("storage.http.fsAccess"), http.getFsAccess(), false);
		final Map<String, String> headers = http.getHeaders();
		assertEquals(parameterErrorMessage("storage.http.headers[\"Connection\"]"), headers.get(
			DriverConfig.StorageConfig.HttpConfig.KEY_HEADER_CONNECTION), "keep-alive");
		assertEquals(parameterErrorMessage("storage.http.headers[\"User-Agent\"]"), headers.get(
			DriverConfig.StorageConfig.HttpConfig.KEY_HEADER_USER_AGENT), "mongoose/3.0.0");
		assertNull("storage.http.namespace", http.getNamespace());
		assertEquals("storage.http.versioning", http.getVersioning(), false);
		assertEquals("storage.port", storage.getPort(), 9020);
		assertEquals("storage.type", storage.getType(), "http");
	}
}
