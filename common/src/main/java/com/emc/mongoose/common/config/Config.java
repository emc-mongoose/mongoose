package com.emc.mongoose.common.config;

import com.emc.mongoose.common.util.DataRangesConfig;
import com.emc.mongoose.common.util.SizeInBytes;
import com.emc.mongoose.common.util.TimeUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created on 11.07.16.
 */
public final class Config {

	public final static class TimeStrToLongDeserializer
	extends JsonDeserializer<Long> {

		@Override
		public final Long deserialize(final JsonParser p, final DeserializationContext ctx)
		throws IOException, JsonProcessingException {
			final String rawValue = p.getValueAsString();
			final TimeUnit timeUnit = TimeUtil.getTimeUnit(rawValue);
			if(timeUnit == null) {
				return TimeUtil.getTimeValue(rawValue);
			} else {
				return timeUnit.toSeconds(TimeUtil.getTimeValue(rawValue));
			}
		}
	}

	public final static class SizeInBytesDeserializer
	extends JsonDeserializer<SizeInBytes> {

		@Override
		public final SizeInBytes deserialize(final JsonParser p, final DeserializationContext ctxt)
		throws IOException, JsonProcessingException {
			return new SizeInBytes(p.getValueAsString());
		}
	}

	public static final String KEY_NAME = "name";
	public static final String KEY_VERSION = "version";
	public static final String KEY_IO = "io";
	public static final String KEY_SOCKET = "socket";
	public static final String KEY_ITEM = "item";
	public static final String KEY_LOAD = "load";
	public static final String KEY_RUN = "run";
	public static final String KEY_STORAGE = "storage";
	public static final String KEY_ALIASING = "aliasing";

	@JsonProperty(KEY_NAME) private String name;
	@JsonProperty(KEY_VERSION) private String version;
	@JsonProperty(KEY_IO) private IoConfig ioConfig;
	@JsonProperty(KEY_SOCKET) private SocketConfig socketConfig;
	@JsonProperty(KEY_STORAGE) private StorageConfig storageConfig;
	@JsonProperty(KEY_LOAD) private LoadConfig loadConfig;
	@JsonProperty(KEY_RUN) private RunConfig runConfig;
	@JsonProperty(KEY_ITEM) private ItemConfig itemConfig;
	@JsonProperty(KEY_ALIASING) private Map<String, Object> aliasingConfig;

	private Config() {}

	public final String getName() {
		return name;
	}

	public final String getVersion() {
		return version;
	}

	public final IoConfig getIoConfig() {
		return ioConfig;
	}

	public final SocketConfig getSocketConfig() {
		return socketConfig;
	}

	public final StorageConfig getStorageConfig() {
		return storageConfig;
	}

	public final LoadConfig getLoadConfig() {
		return loadConfig;
	}

	public final RunConfig getRunConfig() {
		return runConfig;
	}

	public final ItemConfig getItemConfig() {
		return itemConfig;
	}

	public final Map<String, Object> getAliasingConfig() {
		return aliasingConfig;
	}

	public static ConfigBuilder newBuilder() {
		return new Config().new ConfigBuilder();
	}

	public final class ConfigBuilder {

		private ConfigBuilder() {}

		public final ConfigBuilder setName(final String name) {
			Config.this.name = name;
			return this;
		}

		public final ConfigBuilder setVersion(final String version) {
			Config.this.version = version;
			return this;
		}

		public final ConfigBuilder setIoConfig(final IoConfig ioConfig) {
			Config.this.ioConfig = ioConfig;
			return this;
		}

		public final ConfigBuilder setSocketConfig(final SocketConfig socketConfig) {
			Config.this.socketConfig = socketConfig;
			return this;
		}

		public final ConfigBuilder setItemConfig(final ItemConfig itemConfig) {
			Config.this.itemConfig = itemConfig;
			return this;
		}

		public final ConfigBuilder setLoadConfig(final LoadConfig loadConfig) {
			Config.this.loadConfig = loadConfig;
			return this;
		}

		public final ConfigBuilder setRunConfig(final RunConfig runConfig) {
			Config.this.runConfig = runConfig;
			return this;
		}

		public final ConfigBuilder setStorageConfig(final StorageConfig storageConfig) {
			Config.this.storageConfig = storageConfig;
			return this;
		}

		public final Config build() {
			return Config.this;
		}

	}

	public final static class IoConfig {

		public static final String KEY_BUFFER = "buffer";

		@JsonProperty(KEY_BUFFER)
		private BufferConfig bufferConfig;

		public IoConfig() {
		}

		public IoConfig(final BufferConfig bufferConfig) {
			this.bufferConfig = bufferConfig;
		}

		public static class BufferConfig {

			public static final String KEY_SIZE = "size";

			@JsonProperty(KEY_SIZE) @JsonDeserialize(using = SizeInBytesDeserializer.class)
			private SizeInBytes size;

			public BufferConfig() {
			}

			public BufferConfig(final String size) {
				this.size = new SizeInBytes(size);
			}

			public final SizeInBytes getSize() {
				return size;
			}
		}

		public final BufferConfig getBufferConfig() {
			return bufferConfig;
		}
	}

	public final static class SocketConfig {

		public static final String KEY_TIMEOUT_MILLISEC = "timeoutMilliSec";
		public static final String KEY_REUSE_ADDR = "reuseAddr";
		public static final String KEY_KEEP_ALIVE = "keepAlive";
		public static final String KEY_TCP_NO_DELAY = "tcpNoDelay";
		public static final String KEY_LINGER = "linger";
		public static final String KEY_BIND_BACKLOG_SIZE = "bindBacklogSize";
		public static final String KEY_INTEREST_OP_QUEUED = "interestOpQueued";
		public static final String KEY_SELECT_INTERVAL = "selectInterval";

		@JsonProperty(KEY_TIMEOUT_MILLISEC) private int timeoutMilliSec;
		@JsonProperty(KEY_REUSE_ADDR) private boolean reuseAddr;
		@JsonProperty(KEY_KEEP_ALIVE) private boolean keepAlive;
		@JsonProperty(KEY_TCP_NO_DELAY) private boolean tcpNoDelay;
		@JsonProperty(KEY_LINGER) private int linger;
		@JsonProperty(KEY_BIND_BACKLOG_SIZE) private int bindBackLogSize;
		@JsonProperty(KEY_INTEREST_OP_QUEUED) private boolean interestOpQueued;
		@JsonProperty(KEY_SELECT_INTERVAL) private int selectInterval;

		private SocketConfig() {}

		public static SocketConfigBuilder newBuilder() {
			return new SocketConfig().new SocketConfigBuilder();
		}

		public class SocketConfigBuilder {

			private SocketConfigBuilder() {}

			public final SocketConfigBuilder setTimeoutInMilliseconds(final int timeoutMilliSec) {
				SocketConfig.this.timeoutMilliSec = timeoutMilliSec;
				return this;
			}

			public final SocketConfigBuilder setReusableAddress(final boolean reuseAddr) {
				SocketConfig.this.reuseAddr = reuseAddr;
				return this;
			}

			public final SocketConfigBuilder setKeepAlive(final boolean keepAlive) {
				SocketConfig.this.keepAlive = keepAlive;
				return this;
			}

			public final SocketConfigBuilder setTcpNoDelay(final boolean tcpNoDelay) {
				SocketConfig.this.tcpNoDelay = tcpNoDelay;
				return this;
			}

			public final SocketConfigBuilder setLinger(final int linger) {
				SocketConfig.this.linger = linger;
				return this;
			}

			public final SocketConfigBuilder setBindBacklogSize(final int bindBacklogSize) {
				SocketConfig.this.bindBackLogSize = bindBacklogSize;
				return this;
			}

			public final SocketConfigBuilder setInterestOpQueued(final boolean interestOpQueued) {
				SocketConfig.this.interestOpQueued = interestOpQueued;
				return this;
			}

			public final SocketConfigBuilder setSelectInterval(final int selectInterval) {
				SocketConfig.this.selectInterval = selectInterval;
				return this;
			}

			public final SocketConfig build() {
				return SocketConfig.this;
			}

		}

		public final int getTimeoutInMilliseconds() {
			return timeoutMilliSec;
		}

		public final boolean getReusableAddress() {
			return reuseAddr;
		}

		public final boolean getKeepAlive() {
			return keepAlive;
		}

		public final boolean getTcpNoDelay() {
			return tcpNoDelay;
		}

		public final int getLinger() {
			return linger;
		}

		public final int getBindBackLogSize() {
			return bindBackLogSize;
		}

		public final boolean getInterestOpQueued() {
			return interestOpQueued;
		}

		public final int getSelectInterval() {
			return selectInterval;
		}
	}

	public final static class ItemConfig {

		public static final String KEY_TYPE = "type";
		public static final String KEY_DATA = "data";
		public static final String KEY_INPUT = "input";
		public static final String KEY_OUTPUT = "output";
		public static final String KEY_NAMING = "naming";

		@JsonProperty(KEY_TYPE) private String type;
		@JsonProperty(KEY_DATA) private DataConfig dataConfig;
		@JsonProperty(KEY_INPUT) private InputConfig input;
		@JsonProperty(KEY_OUTPUT) private OutputConfig output;
		@JsonProperty(KEY_NAMING) private NamingConfig namingConfig;

		public ItemConfig() {
		}

		public ItemConfig(
			final String type, final DataConfig dataConfig, final InputConfig inputConfig,
			final OutputConfig outputConfig, final NamingConfig namingConfig
		) {
			this.type = type;
			this.dataConfig = dataConfig;
			this.input = inputConfig;
			this.output = outputConfig;
			this.namingConfig = namingConfig;
		}

		public final String getType() {
			return type;
		}

		public final DataConfig getDataConfig() {
			return dataConfig;
		}

		public final InputConfig getInputConfig() {
			return input;
		}

		public final OutputConfig getOutputConfig() {
			return output;
		}

		public final NamingConfig getNamingConfig() {
			return namingConfig;
		}

		public final static class DataConfig {

			public static final String KEY_CONTENT = "content";
			public static final String KEY_RANGES = "ranges";
			public static final String KEY_SIZE = "size";
			public static final String KEY_VERIFY = "verify";
			
			@JsonProperty(KEY_CONTENT) private ContentConfig contentConfig;
			@JsonProperty(KEY_RANGES) private DataRangesConfig ranges;

			@JsonProperty(KEY_SIZE) @JsonDeserialize(using = SizeInBytesDeserializer.class)
			private SizeInBytes size;

			@JsonProperty(KEY_VERIFY) private boolean verify;

			public DataConfig() {
			}

			public DataConfig(
				final ContentConfig contentConfig, final String ranges, final String size,
				final boolean verify
			) throws DataRangesConfig.InvalidRangeException {
				this.contentConfig = contentConfig;
				this.ranges = new DataRangesConfig(ranges);
				this.size = new SizeInBytes(size);
				this.verify = verify;
			}

			public DataConfig(
				final ContentConfig contentConfig, final int ranges, final String size,
				final boolean verify
			) {
				this.contentConfig = contentConfig;
				this.ranges = new DataRangesConfig(ranges);
				this.size = new SizeInBytes(size);
				this.verify = verify;
			}

			public ContentConfig getContentConfig() {
				return contentConfig;
			}

			public final DataRangesConfig getRanges() {
				return ranges;
			}

			public final SizeInBytes getSize() {
				return size;
			}

			public final boolean getVerify() {
				return verify;
			}

			public final static class ContentConfig {

				public static final String KEY_FILE = "file";
				public static final String KEY_SEED = "seed";
				public static final String KEY_RING_SIZE = "ringSize";
				
				@JsonProperty(KEY_FILE) private String file;
				@JsonProperty(KEY_SEED) private String seed;

				@JsonProperty(KEY_RING_SIZE) @JsonDeserialize(using = SizeInBytesDeserializer.class)
				private SizeInBytes ringSize;

				public ContentConfig() {
				}

				public ContentConfig(final String file, final String seed, final String ringSize) {
					this.file = file;
					this.seed = seed;
					this.ringSize = new SizeInBytes(ringSize);
				}

				public final String getFile() {
					return file;
				}

				public final String getSeed() {
					return seed;
				}

				public final SizeInBytes getRingSize() {
					return ringSize;
				}
			}
		}

		public final static class InputConfig {

			public static final String KEY_CONTAINER = "container";
			public static final String KEY_FILE = "file";

			@JsonProperty(KEY_CONTAINER) private String container;
			@JsonProperty(KEY_FILE) private String file;

			public InputConfig() {
			}

			public InputConfig(final String file, final String container) {
				this.file = file;
				this.container = container;
			}

			public final String getContainer() {
				return container;
			}

			public final String getFile() {
				return file;
			}

		}

		public final static class OutputConfig {

			public static final String KEY_CONTAINER = "container";
			public static final String KEY_FILE = "file";

			@JsonProperty(KEY_CONTAINER) private String container;
			@JsonProperty(KEY_FILE) private String file;

			public OutputConfig() {
			}
			
			public OutputConfig(final String container, final String file) {
				this.container = container;
				this.file = file;
			}

			public String getContainer() {
				return container;
			}

			public String getFile() {
				return file;
			}
		}
		
		public final static class NamingConfig {

			public static final String KEY_TYPE = "type";
			public static final String KEY_PREFIX = "prefix";
			public static final String KEY_RADIX = "radix";
			public static final String KEY_OFFSET = "offset";
			public static final String KEY_LENGTH = "length";
			
			@JsonProperty(KEY_TYPE) private String type;
			@JsonProperty(KEY_PREFIX) private String prefix;
			@JsonProperty(KEY_RADIX) private int radix;
			@JsonProperty(KEY_OFFSET) private long offset;
			@JsonProperty(KEY_LENGTH) private int length;

			public NamingConfig() {
			}

			public NamingConfig(
				final String type, final String prefix, final int radix, final long offset,
				final int length
			) {
				this.type = type;
				this.radix = radix;
				this.offset = offset;
				this.length = length;
				this.prefix = prefix;
			}

			public final String getType() {
				return type;
			}

			public final String getPrefix() {
				return prefix;
			}

			public final int getRadix() {
				return radix;
			}

			public final long getOffset() {
				return offset;
			}

			public final int getLength() {
				return length;
			}
		}
	}

	public final static class LoadConfig {

		public static final String KEY_CIRCULAR = "circular";
		public static final String KEY_TYPE = "type";
		public static final String KEY_CONCURRENCY = "concurrency";
		public static final String KEY_LIMIT = "limit";
		public static final String KEY_METRICS = "metrics";
		
		@JsonProperty(KEY_CIRCULAR) private boolean circular;
		@JsonProperty(KEY_TYPE) private String type;
		@JsonProperty(KEY_CONCURRENCY) private int concurrency;
		@JsonProperty(KEY_LIMIT) private LimitConfig limitConfig;
		@JsonProperty(KEY_METRICS) private MetricsConfig metricsConfig;

		public LoadConfig() {
		}

		public LoadConfig(
			final boolean circular, final String type, final int concurrency,
			final LimitConfig limitConfig, final MetricsConfig metricsConfig
		) {
			this.circular = circular;
			this.type = type;
			this.concurrency = concurrency;
			this.limitConfig = limitConfig;
			this.metricsConfig = metricsConfig;
		}

		public final String getType() {
			return type;
		}

		public final boolean getCircular() {
			return circular;
		}

		public int getConcurrency() {
			return concurrency;
		}

		public final LimitConfig getLimitConfig() {
			return limitConfig;
		}

		public final MetricsConfig getMetricsConfig() {
			return metricsConfig;
		}

		public final static class LimitConfig {

			public static final String KEY_COUNT = "count";
			public static final String KEY_RATE = "rate";
			public static final String KEY_SIZE = "size";
			public static final String KEY_TIME = "time";

			@JsonProperty(KEY_COUNT) private long count;
			@JsonProperty(KEY_RATE) private double rate;
			@JsonProperty(KEY_SIZE) private int size;

			@JsonDeserialize(using = TimeStrToLongDeserializer.class) @JsonProperty(KEY_TIME)
			private long time;

			public LimitConfig() {
			}

			public LimitConfig(
				final long count, final double rate, final int size, final String time
			) {
				this.count = count;
				this.rate = rate;
				this.size = size;
				this.time = TimeUtil.getTimeUnit(time).toSeconds(TimeUtil.getTimeValue(time));
			}

			public final long getCount() {
				return count;
			}

			public final double getRate() {
				return rate;
			}

			public final int getSize() {
				return size;
			}

			public final long getTime() {
				return time;
			}
		}

		public final static class MetricsConfig {

			public static final String KEY_INTERMEDIATE = "intermediate";
			public static final String KEY_PERIOD = "period";
			public static final String KEY_PRECONDITION= "precondition";
			
			@JsonProperty(KEY_INTERMEDIATE) private boolean intermediate;

			@JsonDeserialize(using = TimeStrToLongDeserializer.class) @JsonProperty(KEY_PERIOD)
			private long period;

			@JsonProperty(KEY_PRECONDITION) private boolean precondition;

			public MetricsConfig() {
			}

			public MetricsConfig(
				final boolean intermediate, final String period, final boolean precondition
			) {
				this.intermediate = intermediate;
				this.period = TimeUtil.getTimeUnit(period).toSeconds(TimeUtil.getTimeValue(period));
				this.precondition = precondition;
			}

			public final boolean getIntermediate() {
				return intermediate;
			}

			public final long getPeriod() {
				return period;
			}

			public final boolean getPrecondition() {
				return precondition;
			}
		}
	}

	public final static class RunConfig {

		public static final String KEY_FILE = "file";
		public static final String KEY_ID = "id";
		
		@JsonProperty(KEY_FILE) private String file;
		@JsonProperty(KEY_ID) private String id;

		public RunConfig() {
		}

		public RunConfig(final String file, final String id) {
			this.file = file;
			this.id = id;
		}

		public final String getFile() {
			return file;
		}

		public final String getId() {
			return id;
		}
	}

	public final static class StorageConfig {

		public static final String KEY_ADDRS = "addrs";
		public static final String KEY_AUTH = "auth";
		public static final String KEY_HTTP = "http";
		public static final String KEY_PORT = "port";
		public static final String KEY_SSL = "ssl";
		public static final String KEY_TYPE = "type";
		public static final String KEY_MOCK = "mock";

		@JsonProperty(KEY_ADDRS) private List<String> addrs;
		@JsonProperty(KEY_AUTH) private AuthConfig authConfig;
		@JsonProperty(KEY_HTTP) private HttpConfig httpConfig;
		@JsonProperty(KEY_PORT) private int port;
		@JsonProperty(KEY_SSL) private boolean ssl;
		@JsonProperty(KEY_TYPE) private String type;
		@JsonProperty(KEY_MOCK) private MockConfig mockConfig;

		private StorageConfig() {}

		public static StorageConfigBuilder newBuilder() {
			return new StorageConfig().new StorageConfigBuilder();
		}

		public final class StorageConfigBuilder {

			private StorageConfigBuilder() {}

			public final StorageConfigBuilder setAddresses(final List<String> addrs) {
				StorageConfig.this.addrs = addrs;
				return this;
			}

			public final StorageConfigBuilder setAuthConfig(final AuthConfig authConfig) {
				StorageConfig.this.authConfig = authConfig;
				return this;
			}

			public final StorageConfigBuilder setHttpConfig(final HttpConfig httpConfig) {
				StorageConfig.this.httpConfig = httpConfig;
				return this;
			}

			public final StorageConfigBuilder setPort(final int port) {
				StorageConfig.this.port = port;
				return this;
			}

			public final StorageConfigBuilder setSsl(final boolean ssl) {
				StorageConfig.this.ssl = ssl;
				return this;
			}

			public final StorageConfigBuilder setType(final String type) {
				StorageConfig.this.type = type;
				return this;
			}

			public final StorageConfigBuilder setMockConfig(final MockConfig mockConfig) {
				StorageConfig.this.mockConfig = mockConfig;
				return this;
			}

			public final StorageConfig build() {
				return StorageConfig.this;
			}

		}

		public List<String> getAddresses() {
			return addrs;
		}

		public AuthConfig getAuthConfig() {
			return authConfig;
		}

		public HttpConfig getHttpConfig() {
			return httpConfig;
		}

		public int getPort() {
			return port;
		}

		public boolean isSsl() {
			return ssl;
		}

		public String getType() {
			return type;
		}

		public MockConfig getMockConfig() {
			return mockConfig;
		}

		public final static class AuthConfig {

			public static final String KEY_ID = "id";
			public static final String KEY_SECRET = "secret";
			public static final String KEY_TOKEN = "token";
			
			@JsonProperty(KEY_ID) private String id;
			@JsonProperty(KEY_SECRET) private String secret;
			@JsonProperty(KEY_TOKEN) private String token;

			public AuthConfig() {
			}

			public AuthConfig(final String id, final String secret, final String token) {
				this.id = id;
				this.secret = secret;
				this.token = token;
			}

			public String getId() {
				return id;
			}

			public String getSecret() {
				return secret;
			}

			public String getToken() {
				return token;
			}
		}

		public final static class HttpConfig {

			public static final String KEY_API = "api";
			public static final String KEY_FS_ACCESS = "fsAccess";
			public static final String KEY_HEADERS = "headers";
			public static final String KEY_HEADER_CONNECTION = "Connection";
			public static final String KEY_HEADER_USER_AGENT = "User-Agent";
			public static final String KEY_NAMESPACE = "namespace";
			public static final String KEY_VERSIONING = "versioning";
			
			@JsonProperty(KEY_API) private String api;
			@JsonProperty(KEY_FS_ACCESS) private boolean fsAccess;
			@JsonProperty(KEY_NAMESPACE) private String namespace;
			@JsonProperty(KEY_VERSIONING) private boolean versioning;
			@JsonProperty(KEY_HEADERS) private Map<String, String> headers;

			public HttpConfig() {
			}

			public HttpConfig(
				final String api, final boolean fsAccess, final String namespace,
				final boolean versioning, final Map<String, String> headers
			) {
				this.api = api;
				this.fsAccess = fsAccess;
				this.namespace = namespace;
				this.versioning = versioning;
				this.headers = Collections.unmodifiableMap(headers);
			}

			public String getApi() {
				return api;
			}

			public boolean getFsAccess() {
				return fsAccess;
			}

			public String getNamespace() {
				return namespace;
			}

			public boolean getVersioning() {
				return versioning;
			}

			public Map<String, String> getHeaders() {
				return headers;
			}
		}

		public final static class MockConfig {

			public static final String KEY_HEAD_COUNT = "headCount";
			public static final String KEY_CAPACITY = "capacity";
			public static final String KEY_CONTAINER = "container";
			
			@JsonProperty(KEY_HEAD_COUNT) private int headCount;
			@JsonProperty(KEY_CAPACITY) private int capacity;
			@JsonProperty(KEY_CONTAINER) private ContainerConfig containerConfig;

			public MockConfig() {
			}

			public MockConfig(
				final int headCount, final int capacity, final ContainerConfig containerConfig
			) {
				this.headCount = headCount;
				this.capacity = capacity;
				this.containerConfig = containerConfig;
			}

			public int getHeadCount() {
				return headCount;
			}

			public int getCapacity() {
				return capacity;
			}

			public ContainerConfig getContainerConfig() {
				return containerConfig;
			}

			public final static class ContainerConfig {

				public static final String KEY_CAPACITY = "capacity";
				public static final String KEY_COUNT_LIMIT = "countLimit";
				
				@JsonProperty(KEY_CAPACITY) private int capacity;
				@JsonProperty(KEY_COUNT_LIMIT) private int countLimit;

				public ContainerConfig() {
				}

				public ContainerConfig(final int capacity, final int countLimit) {
					this.capacity = capacity;
					this.countLimit = countLimit;
				}

				public int getCapacity() {
					return capacity;
				}

				public int getCountLimit() {
					return countLimit;
				}
			}

		}

	}
}
