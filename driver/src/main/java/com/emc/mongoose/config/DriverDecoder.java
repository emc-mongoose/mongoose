package com.emc.mongoose.config;

import com.emc.mongoose.config.decoder.DecodeException;
import com.emc.mongoose.config.decoder.Decoder;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 Created on 11.07.16.
 */
public class DriverDecoder implements Decoder<DriverConfig> {

	@Override
	public DriverConfig decode(final JsonObject driverJson)
	throws DecodeException {
		final DriverConfig.LoadConfig loadConfig = new DriverConfig.LoadConfig(
			driverJson.getJsonObject(DriverConfig.KEY_LOAD).getInt(
				DriverConfig.LoadConfig.KEY_CONCURRENCY));
		final JsonObject storageJson =
			driverJson.getJsonObject(DriverConfig.KEY_STORAGE);
		final JsonArray addressesJsonArr =
			storageJson.getJsonArray(DriverConfig.StorageConfig.KEY_ADDRESSES);
		final List<String> addresses =
			addressesJsonArr.getValuesAs(JsonString.class).stream().map(
				JsonString:: getString).collect(Collectors.toList());
		final JsonObject authJson =
			storageJson.getJsonObject(DriverConfig.StorageConfig.KEY_AUTH);
		DriverConfig.StorageConfig.AuthConfig
			authConfig = new DriverConfig.StorageConfig.AuthConfig(
			authJson.getString(DriverConfig.StorageConfig.AuthConfig.KEY_ID, null),
			authJson.getString(DriverConfig.StorageConfig.AuthConfig.KEY_ID, null),
			authJson.getString(DriverConfig.StorageConfig.AuthConfig.KEY_ID, null)
		);
		final JsonObject httpJson = storageJson.getJsonObject(DriverConfig.StorageConfig.KEY_HTTP);
		final Map<String, String> headers = new HashMap<>();
		httpJson.getJsonObject(DriverConfig.StorageConfig.HttpConfig.KEY_HEADERS)
			.forEach((name, value) -> headers.put(name, ((JsonString) value).getString()));
		final DriverConfig.StorageConfig.HttpConfig
			httpConfig = new DriverConfig.StorageConfig.HttpConfig(
			httpJson.getString(DriverConfig.StorageConfig.HttpConfig.KEY_API),
			httpJson.getBoolean(DriverConfig.StorageConfig.HttpConfig.KEY_FS_ACCESS),
			httpJson.getString(DriverConfig.StorageConfig.HttpConfig.KEY_NAMESPACE, null),
			httpJson.getBoolean(DriverConfig.StorageConfig.HttpConfig.KEY_VERSIONING), headers
		);
		final DriverConfig.StorageConfig storageConfig = new DriverConfig.StorageConfig(
			storageJson.getInt(DriverConfig.StorageConfig.KEY_PORT),
			storageJson.getString(DriverConfig.StorageConfig.KEY_TYPE), authConfig, httpConfig, addresses
		);
		return new DriverConfig(loadConfig, storageConfig);
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}

}
