package com.emc.mongoose.common.config;

import com.emc.mongoose.common.util.SizeInBytes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 Created on 11.07.16.
 */
public class CommonConfig {

	public static final String KEY_NAME = "name";
	public static final String KEY_VERSION = "version";
	public static final String KEY_IO = "io";
	public static final String KEY_SOCKET = "socket";
	public static final String KEY_ITEM = "item";
	public static final String KEY_LOAD = "load";
	public static final String KEY_RUN = "run";
	public static final String KEY_STORAGE = "storage";
	private String name;
	private String version;
	private IoConfig ioConfig;
	private SocketConfig socketConfig;
	private StorageConfig storageConfig;
	private LoadConfig loadConfig;
	private RunConfig runConfig;
	private ItemConfig itemConfig;

	private CommonConfig() {}

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

	public static final CommonConfigBuilder newBuilder() {
		return new CommonConfig().new CommonConfigBuilder();
	}

	public final class CommonConfigBuilder {

		private CommonConfigBuilder() {}

		public final CommonConfigBuilder setName(final String name) {
			CommonConfig.this.name = name;
			return this;
		}

		public final CommonConfigBuilder setVersion(final String version) {
			CommonConfig.this.version = version;
			return this;
		}

		public final CommonConfigBuilder setIoConfig(final IoConfig ioConfig) {
			CommonConfig.this.ioConfig = ioConfig;
			return this;
		}

		public final CommonConfigBuilder setSocketConfig(final SocketConfig socketConfig) {
			CommonConfig.this.socketConfig = socketConfig;
			return this;
		}

		public final CommonConfigBuilder setItemConfig(final ItemConfig itemConfig) {
			CommonConfig.this.itemConfig = itemConfig;
			return this;
		}

		public final CommonConfigBuilder setLoadConfig(final LoadConfig loadConfig) {
			CommonConfig.this.loadConfig = loadConfig;
			return this;
		}

		public final CommonConfigBuilder setRunConfig(final RunConfig runConfig) {
			CommonConfig.this.runConfig = runConfig;
			return this;
		}

		public final CommonConfigBuilder setStorageConfig(final StorageConfig storageConfig) {
			CommonConfig.this.storageConfig = storageConfig;
			return this;
		}

		public final CommonConfig build() {
			return CommonConfig.this;
		}

	}

	public static class IoConfig {

		public static final String KEY_BUFFER = "buffer";

		private final BufferConfig bufferConfig;

		public IoConfig(final BufferConfig bufferConfig) {
			this.bufferConfig = bufferConfig;
		}

		public static class BufferConfig {
			public static final String KEY_SIZE = "size";

			private final SizeInBytes size;

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

	public static class SocketConfig {

		public static final String KEY_TIMEOUT_IN_MILLISECONDS = "timeoutMilliSec";
		public static final String KEY_REUSABLE_ADDRESS = "reuseAddr";
		public static final String KEY_KEEP_ALIVE = "keepAlive";
		public static final String KEY_TCP_NO_DELAY = "tcpNoDelay";
		public static final String KEY_LINGER = "linger";
		public static final String KEY_BIND_BACKLOG_SIZE = "bindBacklogSize";
		public static final String KEY_INTEREST_OP_QUEUED = "interestOpQueued";
		public static final String KEY_SELECT_INTERVAL = "selectInterval";
		private int timeoutMilliSec;
		private boolean reuseAddr;
		private boolean keepAlive;
		private boolean tcpNoDelay;
		private int linger;
		private int bindBackLogSize;
		private boolean interestOpQueued;
		private int selectInterval;

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

	public static class ItemConfig {

		public static final String KEY_TYPE = "type";
		public static final String KEY_DATA = "data";
		public static final String KEY_INPUT = "input";
		public static final String KEY_OUTPUT = "output";
		public static final String KEY_NAMING = "naming";
		private final String type;
		private final DataConfig dataConfig;
		private final InputConfig input;
		private final OutputConfig output;
		private final NamingConfig namingConfig;

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

		public static class DataConfig {

			public static final String KEY_CONTENT = "content";
			public static final String KEY_RANGES = "ranges";
			public static final String KEY_SIZE = "size";
			public static final String KEY_VERIFY = "verify";
			private final ContentConfig contentConfig;
			private final int ranges;
			private final SizeInBytes size;
			private final boolean verify;

			public DataConfig(
				final ContentConfig contentConfig, final int ranges, final String size,
				final boolean verify
			) {
				this.contentConfig = contentConfig;
				this.ranges = ranges;
				this.size = new SizeInBytes(size);
				this.verify = verify;
			}

			public ContentConfig getContentConfig() {
				return contentConfig;
			}

			public final int getRanges() {
				return ranges;
			}

			public final SizeInBytes getSize() {
				return size;
			}

			public final boolean getVerify() {
				return verify;
			}

			public static class ContentConfig {

				public static final String KEY_FILE = "file";
				public static final String KEY_SEED = "seed";
				public static final String KEY_RING_SIZE = "ringSize";
				private final String file;
				private final String seed;
				private final SizeInBytes ringSize;

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

		public static class InputConfig {

			public static final String KEY_CONTAINER = "container";
			public static final String KEY_FILE = "file";
			private final String container;
			private final String file;

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

		public static class OutputConfig {

			public static final String KEY_CONTAINER = "container";
			public static final String KEY_FILE = "file";
			private final String container;
			private final String file;

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



		public static class NamingConfig {

			public static final String KEY_TYPE = "type";
			public static final String KEY_PREFIX = "prefix";
			public static final String KEY_RADIX = "radix";
			public static final String KEY_OFFSET = "offset";
			public static final String KEY_LENGTH = "length";
			private final String type;
			private final String prefix;
			private final int radix;
			private final int offset;
			private final int length;

			public NamingConfig(
				final String type, final String prefix, final int radix, final int offset,
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

			public final int getOffset() {
				return offset;
			}

			public final int getLength() {
				return length;
			}
		}
	}

	public static class LoadConfig {

		public static final String KEY_CIRCULAR = "circular";
		public static final String KEY_TYPE = "type";
		public static final String KEY_CONCURRENCY = "concurrency";
		public static final String KEY_LIMIT = "limit";
		public static final String KEY_METRICS = "metrics";
		private final boolean circular;
		private final String type;
		private final int concurrency;
		private final LimitConfig limitConfig;
		private final MetricsConfig metricsConfig;

		public LoadConfig(final boolean circular, final String type,
			final int concurrency, final LimitConfig limitConfig,
			final MetricsConfig metricsConfig) {
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

		public static class LimitConfig {

			public static final String KEY_COUNT = "count";
			public static final String KEY_RATE = "rate";
			public static final String KEY_SIZE = "size";
			public static final String KEY_TIME = "time";
			private final int count;
			private final int rate;
			private final int size;
			private final String time;

			public LimitConfig(final int count, final int rate, final int size, final String time) {
				this.count = count;
				this.rate = rate;
				this.size = size;
				this.time = time;
			}

			public final int getCount() {
				return count;
			}

			public final int getRate() {
				return rate;
			}

			public final int getSize() {
				return size;
			}

			public final String getTime() {
				return time;
			}
		}

		public static class MetricsConfig {

			public static final String KEY_INTERMEDIATE = "intermediate";
			public static final String KEY_PERIOD = "period";
			public static final String KEY_PRECONDITION= "precondition";
			private final boolean intermediate;
			private final String period;
			private final boolean precondition;

			public MetricsConfig(
				final boolean intermediate, final String period, final boolean precondition
			) {
				this.intermediate = intermediate;
				this.period = period;
				this.precondition = precondition;
			}

			public final boolean getIntermediate() {
				return intermediate;
			}

			public final String getPeriod() {
				return period;
			}

			public final boolean getPrecondition() {
				return precondition;
			}
		}
	}

	public static class RunConfig {

		public static final String KEY_FILE = "file";
		public static final String KEY_ID = "id";
		private final String file;
		private final String id;

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

	public static class StorageConfig {

		public static final String KEY_ADDRESSES = "addrs";
		public static final String KEY_AUTH = "auth";
		public static final String KEY_HTTP = "http";
		public static final String KEY_PORT = "port";
		public static final String KEY_SSL = "ssl";
		public static final String KEY_TYPE = "type";
		public static final String KEY_MOCK = "mock";
		private List<String> addrs;
		private AuthConfig authConfig;
		private HttpConfig httpConfig;
		private int port;
		private boolean ssl;
		private String type;
		private MockConfig mockConfig;

		private StorageConfig() {}

		public static final StorageConfigBuilder newBuilder() {
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

		public static class AuthConfig {

			public static final String KEY_ID = "id";
			public static final String KEY_SECRET = "secret";
			public static final String KEY_TOKEN = "token";
			private final String id;
			private final String secret;
			private final String token;

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

		public static class HttpConfig {

			public static final String KEY_API = "api";
			public static final String KEY_FS_ACCESS = "fsAccess";
			public static final String KEY_HEADERS = "headers";
			public static final String KEY_HEADER_CONNECTION = "Connection";
			public static final String KEY_HEADER_USER_AGENT = "User-Agent";
			public static final String KEY_NAMESPACE = "namespace";
			public static final String KEY_VERSIONING = "versioning";
			private final String api;
			private final boolean fsAccess;
			private final String namespace;
			private final boolean versioning;
			private final Map<String, String> headers;

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

		public static class MockConfig {

			public static final String KEY_HEAD_COUNT = "headCount";
			public static final String KEY_CAPACITY = "capacity";
			public static final String KEY_CONTAINER = "container";
			private final int headCount;
			private final int capacity;
			private final ContainerConfig containerConfig;

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

			public static class ContainerConfig {

				public static final String KEY_CAPACITY = "capacity";
				public static final String KEY_COUNT_LIMIT = "countLimit";
				private final int capacity;
				private final int countLimit;

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
