package com.emc.mongoose.common.config;

import com.emc.mongoose.common.config.decoder.DecodeException;
import com.emc.mongoose.common.config.decoder.Decoder;

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
public class CommonDecoder implements Decoder<CommonConfig> {

	@Override
	public CommonConfig decode(final JsonObject commonJson)
	throws DecodeException {
		final JsonObject socketJson = commonJson.getJsonObject(CommonConfig.KEY_NETWORK)
 			.getJsonObject(CommonConfig.NetworkConfig.KEY_SOCKET);
		final CommonConfig.NetworkConfig.SocketConfig
			socketConfig = new CommonConfig.NetworkConfig.SocketConfig(
			socketJson.getInt(CommonConfig.NetworkConfig.SocketConfig.KEY_TIMEOUT_IN_MILLISECONDS),
			socketJson.getBoolean(CommonConfig.NetworkConfig.SocketConfig.KEY_REUSABLE_ADDRESS),
			socketJson.getBoolean(CommonConfig.NetworkConfig.SocketConfig.KEY_KEEP_ALIVE),
			socketJson.getBoolean(CommonConfig.NetworkConfig.SocketConfig.KEY_TCP_NO_DELAY),
			socketJson.getInt(CommonConfig.NetworkConfig.SocketConfig.KEY_LINGER),
			socketJson.getInt(CommonConfig.NetworkConfig.SocketConfig.KEY_BIND_BACK_LOG_SIZE),
			socketJson.getBoolean(CommonConfig.NetworkConfig.SocketConfig.KEY_INTEREST_OP_QUEUED),
			socketJson.getInt(CommonConfig.NetworkConfig.SocketConfig.KEY_SELECT_INTERVAL)
		);
		final JsonObject storageJson =
			commonJson.getJsonObject(CommonConfig.KEY_STORAGE);
		final JsonArray addressesJsonArr =
			storageJson.getJsonArray(CommonConfig.StorageConfig.KEY_ADDRESSES);
		final List<String> addresses =
			addressesJsonArr.getValuesAs(JsonString.class).stream().map(
				JsonString::getString).collect(Collectors.toList());
		final JsonObject authJson =
			storageJson.getJsonObject(CommonConfig.StorageConfig.KEY_AUTH);
		final CommonConfig.StorageConfig.AuthConfig
			authConfig = new CommonConfig.StorageConfig.AuthConfig(
			getString(authJson, CommonConfig.StorageConfig.AuthConfig.KEY_ID, null),
			getString(authJson,CommonConfig.StorageConfig.AuthConfig.KEY_SECRET, null),
			getString(authJson,CommonConfig.StorageConfig.AuthConfig.KEY_TOKEN, null)
		);
		final JsonObject httpJson = storageJson.getJsonObject(CommonConfig.StorageConfig.KEY_HTTP);
		final Map<String, String> headers = new HashMap<>();
		httpJson.getJsonObject(CommonConfig.StorageConfig.HttpConfig.KEY_HEADERS)
			.forEach((name, value) -> headers.put(name, ((JsonString) value).getString()));
		final CommonConfig.StorageConfig.HttpConfig
			httpConfig = new CommonConfig.StorageConfig.HttpConfig(
			getString(httpJson, CommonConfig.StorageConfig.HttpConfig.KEY_API),
			httpJson.getBoolean(CommonConfig.StorageConfig.HttpConfig.KEY_FS_ACCESS),
			getString(httpJson, CommonConfig.StorageConfig.HttpConfig.KEY_NAMESPACE, null),
			httpJson.getBoolean(CommonConfig.StorageConfig.HttpConfig.KEY_VERSIONING), headers
		);
		final CommonConfig.StorageConfig storageConfig = new CommonConfig.StorageConfig(
			storageJson.getInt(CommonConfig.StorageConfig.KEY_PORT),
			getString(storageJson, CommonConfig.StorageConfig.KEY_TYPE), authConfig, httpConfig, addresses
		);
		final JsonObject itemJson = commonJson.getJsonObject(CommonConfig.KEY_ITEM);
		final String type = getString(itemJson, CommonConfig.ItemConfig.KEY_TYPE);
		final JsonObject dataJson = itemJson.getJsonObject(CommonConfig.ItemConfig.KEY_DATA);
		final JsonObject contentJson =
			dataJson.getJsonObject(CommonConfig.ItemConfig.DataConfig.KEY_CONTENT);
		final CommonConfig.ItemConfig.DataConfig.ContentConfig contentConfig =
			new CommonConfig.ItemConfig.DataConfig.ContentConfig(
				getString(contentJson, CommonConfig.ItemConfig.DataConfig.ContentConfig.KEY_FILE, null),
				getString(contentJson, CommonConfig.ItemConfig.DataConfig.ContentConfig.KEY_SEED),
				getString(contentJson, CommonConfig.ItemConfig.DataConfig.ContentConfig.KEY_RING_SIZE)
			);
		final CommonConfig.ItemConfig.DataConfig
			dataConfig = new CommonConfig.ItemConfig.DataConfig(
			contentConfig,
			dataJson.getInt(CommonConfig.ItemConfig.DataConfig.KEY_RANGES, 0),
			getString(dataJson, CommonConfig.ItemConfig.DataConfig.KEY_SIZE),
			dataJson.getBoolean(CommonConfig.ItemConfig.DataConfig.KEY_VERIFY)
		);
		final JsonObject destinationJson =
			itemJson.getJsonObject(CommonConfig.ItemConfig.KEY_DESTINATION);
		final CommonConfig.ItemConfig.DestinationConfig
			destinationConfig = new CommonConfig.ItemConfig.DestinationConfig(
			getString(destinationJson, CommonConfig.ItemConfig.DestinationConfig.KEY_CONTAINER, null),
			getString(destinationJson, CommonConfig.ItemConfig.DestinationConfig.KEY_FILE, null)
		);
		final JsonObject sourceJson =
			itemJson.getJsonObject(CommonConfig.ItemConfig.KEY_SOURCE);
		final CommonConfig.ItemConfig.SourceConfig
			sourceConfig = new CommonConfig.ItemConfig.SourceConfig(
			getString(sourceJson, CommonConfig.ItemConfig.SourceConfig.KEY_CONTAINER, null),
			getString(sourceJson, CommonConfig.ItemConfig.SourceConfig.KEY_FILE, null),
			sourceJson.getInt(CommonConfig.ItemConfig.SourceConfig.KEY_BATCH_SIZE)
		);
		final JsonObject namingJson =
			itemJson.getJsonObject(CommonConfig.ItemConfig.KEY_NAMING);
		final CommonConfig.ItemConfig.NamingConfig
			namingConfig = new CommonConfig.ItemConfig.NamingConfig(
			getString(namingJson, CommonConfig.ItemConfig.NamingConfig.KEY_TYPE),
			getString(namingJson, CommonConfig.ItemConfig.NamingConfig.KEY_PREFIX, null),
			namingJson.getInt(CommonConfig.ItemConfig.NamingConfig.KEY_RADIX),
			namingJson.getInt(CommonConfig.ItemConfig.NamingConfig.KEY_OFFSET),
			namingJson.getInt(CommonConfig.ItemConfig.NamingConfig.KEY_LENGTH)
		);
		return new CommonConfig(
			getString(commonJson, CommonConfig.KEY_NAME),
			new CommonConfig.NetworkConfig(socketConfig),
			storageConfig,
			new CommonConfig.ItemConfig(
				type, dataConfig, destinationConfig, sourceConfig, namingConfig
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
