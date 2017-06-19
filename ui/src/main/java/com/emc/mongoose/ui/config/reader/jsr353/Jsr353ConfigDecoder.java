package com.emc.mongoose.ui.config.reader.jsr353;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.reader.jsr353.decoder.Decoder;

//import javax.json.JsonArray;
//import javax.json.JsonNumber;
//import javax.json.JsonObject;
//import javax.json.JsonString;
//import javax.json.JsonValue;

/**
 Created on 11.07.16.
 */
public class Jsr353ConfigDecoder
implements Decoder<Config> {

	/*@Override
	public Config decode(final JsonObject commonConfigJson)
	throws DecodeException {
		final Config.IoConfig ioConfig = new Config.IoConfig(
			new Config.IoConfig.BufferConfig(getString(
				getJsonObject(getJsonObject(commonConfigJson, Config.KEY_IO),
					Config.IoConfig.KEY_BUFFER
				), Config.IoConfig.BufferConfig.KEY_SIZE)));
		final JsonObject socketConfigJson = getJsonObject(commonConfigJson, Config.KEY_SOCKET);
		final Config.SocketConfig.SocketConfigBuilder socketConfigBuilder =
			Config.SocketConfig.newBuilder();
		socketConfigBuilder.setTimeoutInMilliseconds(
			socketConfigJson.getInt(Config.SocketConfig.KEY_TIMEOUT_MILLI_SEC));
		socketConfigBuilder.setReusableAddress(
			socketConfigJson.getBoolean(Config.SocketConfig.KEY_REUSE_ADDR));
		socketConfigBuilder.setKeepAlive(
			socketConfigJson.getBoolean(Config.SocketConfig.KEY_KEEP_ALIVE));
		socketConfigBuilder.setTcpNoDelay(
			socketConfigJson.getBoolean(Config.SocketConfig.KEY_TCP_NO_DELAY));
		socketConfigBuilder.setLinger(
			socketConfigJson.getInt(Config.SocketConfig.KEY_LINGER));
		socketConfigBuilder.setBindBacklogSize(
			socketConfigJson.getInt(Config.SocketConfig.KEY_BIND_BACKLOG_SIZE));
		socketConfigBuilder.setInterestOpQueued(
			socketConfigJson.getBoolean(Config.SocketConfig.KEY_INTEREST_OP_QUEUED));
		socketConfigBuilder.setSelectInterval(
			socketConfigJson.getInt(Config.SocketConfig.KEY_SELECT_INTERVAL));
		final Config.SocketConfig socketConfig = socketConfigBuilder.build();

		final JsonObject itemConfigJson = getJsonObject(commonConfigJson, Config.KEY_ITEM);
		final String type = getString(itemConfigJson, Config.ItemConfig.KEY_TYPE);
		final JsonObject dataConfigJson = getJsonObject(itemConfigJson, Config.ItemConfig.KEY_DATA);
		final JsonObject contentConfigJson =
			getJsonObject(dataConfigJson, Config.ItemConfig.DataConfig.KEY_CONTENT);
		final Config.ItemConfig.DataConfig.ContentConfig contentConfig =
			new Config.ItemConfig.DataConfig.ContentConfig(getString(
				contentConfigJson, Config.ItemConfig.DataConfig.ContentConfig.KEY_FILE, null),
				getString(contentConfigJson, Config.ItemConfig.DataConfig.ContentConfig.KEY_SEED),
				getString(contentConfigJson,
					Config.ItemConfig.DataConfig.ContentConfig.KEY_RING_SIZE
				)
			);
		final String size =
			getString(dataConfigJson, Config.ItemConfig.DataConfig.KEY_SIZE);
		final boolean verify =
			dataConfigJson.getBoolean(Config.ItemConfig.DataConfig.KEY_VERIFY);
		final JsonValue rawRanges =
			dataConfigJson.get(Config.ItemConfig.DataConfig.KEY_RANGES);
		Config.ItemConfig.DataConfig dataConfig = null;
		switch(rawRanges.getValueType()) {
			case NUMBER:
				dataConfig = new Config.ItemConfig.DataConfig(contentConfig,
					((JsonNumber) rawRanges).intValue(),
					size, verify
				);
				break;
			case STRING:
				dataConfig = new Config.ItemConfig.DataConfig(contentConfig,
					((JsonString) rawRanges).getString(),
					size, verify
				);
				break;
		}
		final JsonObject inputConfigJson = getJsonObject(itemConfigJson, Config.ItemConfig.KEY_INPUT);
		final Config.ItemConfig.InputConfig inputConfig =
			new Config.ItemConfig.InputConfig(
				getString(inputConfigJson, Config.ItemConfig.InputConfig.KEY_PATH, null),
				getString(inputConfigJson, Config.ItemConfig.InputConfig.KEY_FILE, null)
			);
		final JsonObject outputConfigJson =
			getJsonObject(itemConfigJson, Config.ItemConfig.KEY_OUTPUT);
		final Config.ItemConfig.OutputConfig outputConfig =
			new Config.ItemConfig.OutputConfig(getString(
				outputConfigJson, Config.ItemConfig.OutputConfig.KEY_PATH, null),
				getString(outputConfigJson, Config.ItemConfig.OutputConfig.KEY_FILE, null)
			);
		final JsonObject namingConfigJson = getJsonObject(itemConfigJson, Config.ItemConfig.KEY_NAMING);
		final Config.ItemConfig.NamingConfig namingConfig =
			new Config.ItemConfig.NamingConfig(
				getString(namingConfigJson, Config.ItemConfig.NamingConfig.KEY_TYPE),
				getString(namingConfigJson, Config.ItemConfig.NamingConfig.KEY_PREFIX, null),
				namingConfigJson.getInt(Config.ItemConfig.NamingConfig.KEY_RADIX),
				namingConfigJson.getJsonNumber(Config.ItemConfig.NamingConfig.KEY_OFFSET).longValue(),
				namingConfigJson.getInt(Config.ItemConfig.NamingConfig.KEY_LENGTH)
			);
		final Config.ItemConfig itemConfig =
			new Config.ItemConfig(type, dataConfig, inputConfig, outputConfig, namingConfig);
		final JsonObject loadConfigJson = getJsonObject(commonConfigJson, Config.KEY_LOAD);
		final JsonObject limitConfigJson =
			getJsonObject(loadConfigJson, Config.LoadConfig.KEY_LIMIT);

		final Config.LoadConfig.LimitConfig limitConfig =
			new Config.LoadConfig.LimitConfig(
				limitConfigJson.getJsonNumber(Config.LoadConfig.LimitConfig.KEY_COUNT).longValue(),
				limitConfigJson.getJsonNumber(Config.LoadConfig.LimitConfig.KEY_RATE).doubleValue(),
				limitConfigJson.getInt(Config.LoadConfig.LimitConfig.KEY_SIZE),
				getString(limitConfigJson, Config.LoadConfig.LimitConfig.KEY_TIME)
			);
		final JsonObject metricsConfigJson =
			getJsonObject(loadConfigJson, Config.LoadConfig.KEY_METRICS);
		final Config.LoadConfig.MetricsConfig metricsConfig =
			new Config.LoadConfig.MetricsConfig(
				metricsConfigJson.getBoolean(Config.LoadConfig.MetricsConfig.KEY_THRESHOLD),
				getString(metricsConfigJson, Config.LoadConfig.MetricsConfig.KEY_PERIOD),
				metricsConfigJson.getBoolean(Config.LoadConfig.MetricsConfig.KEY_PRECONDITION)
			);
		final Config.LoadConfig loadConfig = new Config.LoadConfig(
			loadConfigJson.getBoolean(Config.LoadConfig.KEY_CIRCULAR),
			getString(loadConfigJson, Config.LoadConfig.KEY_TYPE),
			loadConfigJson.getInt(Config.LoadConfig.KEY_CONCURRENCY), limitConfig,
			metricsConfig
		);
		final JsonObject runConfigJson = getJsonObject(commonConfigJson, Config.SCENARIO);
		final Config.RunConfig runConfig = new Config.RunConfig(
			getString(runConfigJson, Config.RunConfig.KEY_ID, null),
			getString(runConfigJson, Config.RunConfig.KEY_FILE, null)
		);
		final JsonObject storageConfigJson = getJsonObject(commonConfigJson, Config.KEY_STORAGE);
		final JsonArray addressesJsonArr =
			storageConfigJson.getJsonArray(Config.StorageConfig.KEY_ADDRS);
		final List<String> addresses = addressesJsonArr.getValuesAs(JsonString.class).stream().map(
			JsonString:: getString).collect(Collectors.toList());
		final JsonObject authJson = getJsonObject(storageConfigJson, Config.StorageConfig.KEY_AUTH);
		final Config.StorageConfig.AuthConfig authConfig =
			new Config.StorageConfig.AuthConfig(
				getString(authJson, Config.StorageConfig.AuthConfig.KEY_UID, null),
				getString(authJson, Config.StorageConfig.AuthConfig.KEY_SECRET, null),
				getString(authJson, Config.StorageConfig.AuthConfig.KEY_TOKEN, null)
			);
		final JsonObject httpConfigJson = getJsonObject(storageConfigJson, Config.StorageConfig.KEY_HTTP);
		final Map<String, String> headers = new HashMap<>();
		getJsonObject(httpConfigJson, Config.StorageConfig.HttpConfig.KEY_HEADERS).forEach(
			(headerName, headerValue) -> headers.put(headerName, ((JsonString) headerValue).getString()));
		final Config.StorageConfig.HttpConfig httpConfig =
			new Config.StorageConfig.HttpConfig(
				getString(httpConfigJson, Config.StorageConfig.HttpConfig.KEY_API),
				httpConfigJson.getBoolean(Config.StorageConfig.HttpConfig.KEY_FS_ACCESS),
				getString(httpConfigJson, Config.StorageConfig.HttpConfig.KEY_NAMESPACE, null),
				httpConfigJson.getBoolean(Config.StorageConfig.HttpConfig.KEY_VERSIONING), headers
			);
		final JsonObject mockConfigJson =
			getJsonObject(storageConfigJson, Config.StorageConfig.KEY_MOCK);
		final JsonObject containerConfigJson =
			getJsonObject(mockConfigJson, Config.StorageConfig.MockConfig.KEY_PATH);
		final Config.StorageConfig.MockConfig mockConfig =
			new Config.StorageConfig.MockConfig(
				mockConfigJson.getInt(Config.StorageConfig.MockConfig.KEY_HEAD_COUNT),
				mockConfigJson.getInt(Config.StorageConfig.MockConfig.KEY_CAPACITY),
				new Config.StorageConfig.MockConfig.ContainerConfig(
					containerConfigJson.getInt(
						Config.StorageConfig.MockConfig.ContainerConfig.KEY_CAPACITY),
					containerConfigJson.getInt(
						Config.StorageConfig.MockConfig.ContainerConfig.KEY_COUNT_LIMIT)
				)
			);
		final Config.StorageConfig.StorageConfigBuilder storageConfigBuilder =
			Config.StorageConfig.newBuilder();
		storageConfigBuilder.setAddresses(addresses);
		storageConfigBuilder.setAuthConfig(authConfig);
		storageConfigBuilder.setHttpConfig(httpConfig);
		storageConfigBuilder.setPort(storageConfigJson.getInt(Config.StorageConfig.KEY_PORT));
		storageConfigBuilder.setSsl(storageConfigJson.getBoolean(Config.StorageConfig.KEY_SSL));
		storageConfigBuilder.setType(getString(storageConfigJson, Config.StorageConfig.KEY_TYPE));
		storageConfigBuilder.setMockConfig(mockConfig);
		final Config.StorageConfig storageConfig = storageConfigBuilder.build();
		final Config.ConfigBuilder configBuilder = Config.newBuilder();
		configBuilder.setName(getString(commonConfigJson, Config.NAME));
		configBuilder.setVersion(getString(commonConfigJson, Config.KEY_VERSION));
		configBuilder.setIoConfig(ioConfig);
		configBuilder.setSocketConfig(socketConfig);
		configBuilder.setItemConfig(itemConfig);
		configBuilder.setLoadConfig(loadConfig);
		configBuilder.setRunConfig(runConfig);
		configBuilder.setStorageConfig(storageConfig);
		return configBuilder.build();
	}*/

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}
}
