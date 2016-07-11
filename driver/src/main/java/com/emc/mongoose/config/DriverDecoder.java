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
		final DriverConfig.Load load = new DriverConfig.Load(
			driverJson.getJsonObject(DriverConfig.KEY_LOAD).getInt(
				DriverConfig.Load.KEY_CONCURRENCY));
		final JsonObject storageJson =
			driverJson.getJsonObject(DriverConfig.KEY_STORAGE);
		final JsonArray addressesJsonArr =
			storageJson.getJsonArray(DriverConfig.Storage.KEY_ADDRESSES);
		final List<String> addresses =
			addressesJsonArr.getValuesAs(JsonString.class).stream().map(
				JsonString:: getString).collect(Collectors.toList());
		final JsonObject authJson =
			storageJson.getJsonObject(DriverConfig.Storage.KEY_AUTH);
		DriverConfig.Storage.Auth auth = new DriverConfig.Storage.Auth(
			authJson.getString(DriverConfig.Storage.Auth.KEY_ID, null),
			authJson.getString(DriverConfig.Storage.Auth.KEY_SECRET, null),
			authJson.getString(DriverConfig.Storage.Auth.KEY_TOKEN, null)
		);
		final JsonObject httpJson = storageJson.getJsonObject(DriverConfig.Storage.KEY_HTTP);
		final Map<String, String> headers = new HashMap<>();
		httpJson.getJsonObject(DriverConfig.Storage.Http.KEY_HEADERS)
			.forEach((name, value) -> headers.put(name, ((JsonString) value).getString()));
		final DriverConfig.Storage.Http http = new DriverConfig.Storage.Http(
			httpJson.getString(DriverConfig.Storage.Http.KEY_API),
			httpJson.getBoolean(DriverConfig.Storage.Http.KEY_FS_ACCESS),
			httpJson.getString(DriverConfig.Storage.Http.KEY_NAMESPACE, null),
			httpJson.getBoolean(DriverConfig.Storage.Http.KEY_VERSIONING), headers
		);
		final DriverConfig.Storage storage = new DriverConfig.Storage(
			storageJson.getInt(DriverConfig.Storage.KEY_PORT),
			storageJson.getString(DriverConfig.Storage.KEY_TYPE), auth, http, addresses
		);
		return new DriverConfig(load, storage);
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}

}
