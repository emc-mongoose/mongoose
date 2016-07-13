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
public class CommonDecoder
	implements Decoder<CommonConfig> {

	@Override
	public CommonConfig decode(final JsonObject commonJson)
	throws DecodeException {
		final CommonConfig.IoConfig ioConfig = new CommonConfig.IoConfig(
			new CommonConfig.IoConfig.BufferConfig(getString(
				getJsonObject(getJsonObject(commonJson, CommonConfig.KEY_IO),
					CommonConfig.IoConfig.KEY_BUFFER
				), CommonConfig.IoConfig.BufferConfig.KEY_SIZE)));
		final JsonObject socketConfigJson = getJsonObject(commonJson, CommonConfig.KEY_SOCKET);
		final CommonConfig.SocketConfig.SocketConfigBuilder socketConfigBuilder =
			CommonConfig.SocketConfig.newBuilder();
		socketConfigBuilder.setTimeoutInMilliseconds(
			socketConfigJson.getInt(CommonConfig.SocketConfig.KEY_TIMEOUT_IN_MILLISECONDS));
		socketConfigBuilder.setReusableAddress(
			socketConfigJson.getBoolean(CommonConfig.SocketConfig.KEY_REUSABLE_ADDRESS));
		socketConfigBuilder.setKeepAlive(
			socketConfigJson.getBoolean(CommonConfig.SocketConfig.KEY_KEEP_ALIVE));
		socketConfigBuilder.setTcpNoDelay(
			socketConfigJson.getBoolean(CommonConfig.SocketConfig.KEY_TCP_NO_DELAY));
		socketConfigBuilder.setLinger(
			socketConfigJson.getInt(CommonConfig.SocketConfig.KEY_LINGER));
		socketConfigBuilder.setBindBacklogSize(
			socketConfigJson.getInt(CommonConfig.SocketConfig.KEY_BIND_BACKLOG_SIZE));
		socketConfigBuilder.setInterestOpQueued(
			socketConfigJson.getBoolean(CommonConfig.SocketConfig.KEY_INTEREST_OP_QUEUED));
		socketConfigBuilder.setSelectInterval(
			socketConfigJson.getInt(CommonConfig.SocketConfig.KEY_SELECT_INTERVAL));
		final CommonConfig.SocketConfig socketConfig = socketConfigBuilder.build();

		final JsonObject itemJson = getJsonObject(commonJson, CommonConfig.KEY_ITEM);
		final String type = getString(itemJson, CommonConfig.ItemConfig.KEY_TYPE);
		final JsonObject dataJson = getJsonObject(itemJson, CommonConfig.ItemConfig.KEY_DATA);
		final JsonObject contentJson =
			getJsonObject(dataJson, CommonConfig.ItemConfig.DataConfig.KEY_CONTENT);
		final CommonConfig.ItemConfig.DataConfig.ContentConfig contentConfig =
			new CommonConfig.ItemConfig.DataConfig.ContentConfig(getString(
				contentJson, CommonConfig.ItemConfig.DataConfig.ContentConfig.KEY_FILE, null),
				getString(contentJson, CommonConfig.ItemConfig.DataConfig.ContentConfig.KEY_SEED),
				getString(contentJson,
					CommonConfig.ItemConfig.DataConfig.ContentConfig.KEY_RING_SIZE
				)
			);
		final CommonConfig.ItemConfig.DataConfig dataConfig =
			new CommonConfig.ItemConfig.DataConfig(contentConfig,
				dataJson.getInt(CommonConfig.ItemConfig.DataConfig.KEY_RANGES, 0),
				getString(dataJson, CommonConfig.ItemConfig.DataConfig.KEY_SIZE),
				dataJson.getBoolean(CommonConfig.ItemConfig.DataConfig.KEY_VERIFY)
			);
		final JsonObject inputJson = getJsonObject(itemJson, CommonConfig.ItemConfig.KEY_INPUT);
		final CommonConfig.ItemConfig.InputConfig inputConfig =
			new CommonConfig.ItemConfig.InputConfig(
				getString(inputJson, CommonConfig.ItemConfig.InputConfig.KEY_CONTAINER, null),
				getString(inputJson, CommonConfig.ItemConfig.InputConfig.KEY_FILE, null)
			);
		final JsonObject outputJson =
			getJsonObject(itemJson, CommonConfig.ItemConfig.KEY_OUTPUT);
		final CommonConfig.ItemConfig.OutputConfig outputConfig =
			new CommonConfig.ItemConfig.OutputConfig(getString(
				outputJson, CommonConfig.ItemConfig.OutputConfig.KEY_CONTAINER, null),
				getString(outputJson, CommonConfig.ItemConfig.OutputConfig.KEY_FILE, null)
			);
		final JsonObject namingJson = getJsonObject(itemJson, CommonConfig.ItemConfig.KEY_NAMING);
		final CommonConfig.ItemConfig.NamingConfig namingConfig =
			new CommonConfig.ItemConfig.NamingConfig(
				getString(namingJson, CommonConfig.ItemConfig.NamingConfig.KEY_TYPE),
				getString(namingJson, CommonConfig.ItemConfig.NamingConfig.KEY_PREFIX, null),
				namingJson.getInt(CommonConfig.ItemConfig.NamingConfig.KEY_RADIX),
				namingJson.getInt(CommonConfig.ItemConfig.NamingConfig.KEY_OFFSET),
				namingJson.getInt(CommonConfig.ItemConfig.NamingConfig.KEY_LENGTH)
			);
		final CommonConfig.ItemConfig itemConfig =
			new CommonConfig.ItemConfig(type, dataConfig, inputConfig, outputConfig, namingConfig);
		final JsonObject loadConfigJson = getJsonObject(commonJson, CommonConfig.KEY_LOAD);
		final JsonObject limitConfigJson =
			getJsonObject(loadConfigJson, CommonConfig.LoadConfig.KEY_LIMIT);
		final CommonConfig.LoadConfig.LimitConfig limitConfig =
			new CommonConfig.LoadConfig.LimitConfig(
				limitConfigJson.getInt(CommonConfig.LoadConfig.LimitConfig.KEY_COUNT),
				limitConfigJson.getInt(CommonConfig.LoadConfig.LimitConfig.KEY_RATE),
				limitConfigJson.getInt(CommonConfig.LoadConfig.LimitConfig.KEY_SIZE),
				getString(limitConfigJson, CommonConfig.LoadConfig.LimitConfig.KEY_TIME)
			);
		final JsonObject metricsConfigJson =
			getJsonObject(loadConfigJson, CommonConfig.LoadConfig.KEY_METRICS);
		final CommonConfig.LoadConfig.MetricsConfig metricsConfig =
			new CommonConfig.LoadConfig.MetricsConfig(
				metricsConfigJson.getBoolean(CommonConfig.LoadConfig.MetricsConfig.KEY_INTERMEDIATE),
				getString(metricsConfigJson, CommonConfig.LoadConfig.MetricsConfig.KEY_PERIOD),
				metricsConfigJson.getBoolean(CommonConfig.LoadConfig.MetricsConfig.KEY_PRECONDITION)
			);
		final CommonConfig.LoadConfig loadConfig = new CommonConfig.LoadConfig(
			loadConfigJson.getBoolean(CommonConfig.LoadConfig.KEY_CIRCULAR),
			getString(loadConfigJson, CommonConfig.LoadConfig.KEY_TYPE),
			loadConfigJson.getInt(CommonConfig.LoadConfig.KEY_CONCURRENCY), limitConfig,
			metricsConfig
		);
		final JsonObject runConfigJson = getJsonObject(commonJson, CommonConfig.KEY_RUN);
		final CommonConfig.RunConfig runConfig = new CommonConfig.RunConfig(
			getString(runConfigJson, CommonConfig.RunConfig.KEY_ID, null),
			getString(runConfigJson, CommonConfig.RunConfig.KEY_FILE, null)
		);
		final JsonObject storageConfigJson = getJsonObject(commonJson, CommonConfig.KEY_STORAGE);
		final JsonArray addressesJsonArr =
			storageConfigJson.getJsonArray(CommonConfig.StorageConfig.KEY_ADDRESSES);
		final List<String> addresses = addressesJsonArr.getValuesAs(JsonString.class).stream().map(
			JsonString:: getString).collect(Collectors.toList());
		final JsonObject authJson = getJsonObject(storageConfigJson, CommonConfig.StorageConfig.KEY_AUTH);
		final CommonConfig.StorageConfig.AuthConfig authConfig =
			new CommonConfig.StorageConfig.AuthConfig(
				getString(authJson, CommonConfig.StorageConfig.AuthConfig.KEY_ID, null),
				getString(authJson, CommonConfig.StorageConfig.AuthConfig.KEY_SECRET, null),
				getString(authJson, CommonConfig.StorageConfig.AuthConfig.KEY_TOKEN, null)
			);
		final JsonObject httpJson = getJsonObject(storageConfigJson, CommonConfig.StorageConfig.KEY_HTTP);
		final Map<String, String> headers = new HashMap<>();
		getJsonObject(httpJson, CommonConfig.StorageConfig.HttpConfig.KEY_HEADERS).forEach(
			(headerName, headerValue) -> headers.put(headerName, ((JsonString) headerValue).getString()));
		final CommonConfig.StorageConfig.HttpConfig httpConfig =
			new CommonConfig.StorageConfig.HttpConfig(
				getString(httpJson, CommonConfig.StorageConfig.HttpConfig.KEY_API),
				httpJson.getBoolean(CommonConfig.StorageConfig.HttpConfig.KEY_FS_ACCESS),
				getString(httpJson, CommonConfig.StorageConfig.HttpConfig.KEY_NAMESPACE, null),
				httpJson.getBoolean(CommonConfig.StorageConfig.HttpConfig.KEY_VERSIONING), headers
			);
		final JsonObject mockConfigJson =
			getJsonObject(storageConfigJson, CommonConfig.StorageConfig.KEY_MOCK);
		final JsonObject containerConfigJson =
			getJsonObject(mockConfigJson, CommonConfig.StorageConfig.MockConfig.KEY_CONTAINER);
		final CommonConfig.StorageConfig.MockConfig mockConfig =
			new CommonConfig.StorageConfig.MockConfig(
				mockConfigJson.getInt(CommonConfig.StorageConfig.MockConfig.KEY_HEAD_COUNT),
				mockConfigJson.getInt(CommonConfig.StorageConfig.MockConfig.KEY_CAPACITY),
				new CommonConfig.StorageConfig.MockConfig.ContainerConfig(
					containerConfigJson.getInt(
						CommonConfig.StorageConfig.MockConfig.ContainerConfig.KEY_CAPACITY),
					containerConfigJson.getInt(
						CommonConfig.StorageConfig.MockConfig.ContainerConfig.KEY_COUNT_LIMIT)
				)
			);
		final CommonConfig.StorageConfig.StorageConfigBuilder storageConfigBuilder =
			CommonConfig.StorageConfig.newBuilder();
		storageConfigBuilder.setAddresses(addresses);
		storageConfigBuilder.setAuthConfig(authConfig);
		storageConfigBuilder.setHttpConfig(httpConfig);
		storageConfigBuilder.setPort(storageConfigJson.getInt(CommonConfig.StorageConfig.KEY_PORT));
		storageConfigBuilder.setSsl(storageConfigJson.getBoolean(CommonConfig.StorageConfig.KEY_SSL));
		storageConfigBuilder.setType(getString(storageConfigJson, CommonConfig.StorageConfig.KEY_TYPE));
		storageConfigBuilder.setMockConfig(mockConfig);
		final CommonConfig.StorageConfig storageConfig = storageConfigBuilder.build();
		final CommonConfig.CommonConfigBuilder commonConfigBuilder = CommonConfig.newBuilder();
		commonConfigBuilder.setName(getString(commonJson, CommonConfig.KEY_NAME));
		commonConfigBuilder.setVersion(getString(commonJson, CommonConfig.KEY_VERSION));
		commonConfigBuilder.setIoConfig(ioConfig);
		commonConfigBuilder.setSocketConfig(socketConfig);
		commonConfigBuilder.setItemConfig(itemConfig);
		commonConfigBuilder.setLoadConfig(loadConfig);
		commonConfigBuilder.setRunConfig(runConfig);
		commonConfigBuilder.setStorageConfig(storageConfig);
		return commonConfigBuilder.build();
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}
}
