package com.emc.mongoose.ui.config;

import com.emc.mongoose.model.util.SizeInBytes;
import com.emc.mongoose.model.util.TimeUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created on 11.07.16.
 */
@SuppressWarnings("unused")
public final class Config {

	private static final class TimeStrToLongDeserializer
	extends JsonDeserializer<Long> {

		@Override
		public final Long deserialize(final JsonParser p, final DeserializationContext ctx)
		throws JsonProcessingException, IOException {
			final String rawValue = p.getValueAsString();
			final TimeUnit timeUnit = TimeUtil.getTimeUnit(rawValue);
			if(timeUnit == null) {
				return TimeUtil.getTimeValue(rawValue);
			} else {
				return timeUnit.toSeconds(TimeUtil.getTimeValue(rawValue));
			}
		}
	}

	private static final class SizeInBytesDeserializer
	extends JsonDeserializer<SizeInBytes> {
		@Override
		public final SizeInBytes deserialize(final JsonParser p, final DeserializationContext ctx)
		throws JsonProcessingException, IOException{
			return new SizeInBytes(p.getValueAsString());
		}
	}

	private static final class DataRangesConfigDeserializer
	extends JsonDeserializer<DataRangesConfig> {
		@Override
		public final DataRangesConfig deserialize(final JsonParser p, final DeserializationContext ctx)
		throws JsonProcessingException, IOException{
			try {
				return new DataRangesConfig(p.getValueAsInt());
			} catch(final IOException ignored) {
				return new DataRangesConfig(p.getValueAsString());
			}
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

	public Config() {}

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
	
	public final void setName(final String name) {
		this.name = name;
	}
	
	public final void setVersion(final String version) {
		this.version = version;
	}
	
	public final void setIoConfig(final IoConfig ioConfig) {
		this.ioConfig = ioConfig;
	}
	
	public final void setSocketConfig(final SocketConfig socketConfig) {
		this.socketConfig = socketConfig;
	}
	
	public final void setStorageConfig(final StorageConfig storageConfig) {
		this.storageConfig = storageConfig;
	}
	
	public final void setLoadConfig(final LoadConfig loadConfig) {
		this.loadConfig = loadConfig;
	}
	
	public final void setRunConfig(final RunConfig runConfig) {
		this.runConfig = runConfig;
	}
	
	public final void setItemConfig(final ItemConfig itemConfig) {
		this.itemConfig = itemConfig;
	}
	
	public final void setAliasingConfig(final Map<String, Object> aliasingConfig) {
		this.aliasingConfig = aliasingConfig;
	}
	
	public final static class IoConfig {
		
		public static final String KEY_BUFFER = "buffer";
		
		@JsonProperty(KEY_BUFFER)
		private BufferConfig bufferConfig;

		public IoConfig() {
		}

		public static class BufferConfig {
			
			public static final String KEY_SIZE = "size";
			
			@JsonProperty(KEY_SIZE) @JsonDeserialize(using = SizeInBytesDeserializer.class)
			private SizeInBytes size;

			public BufferConfig() {
			}

			public final SizeInBytes getSize() {
				return size;
			}
			
			public final void setSize(final SizeInBytes size) {
				this.size = size;
			}
		}

		public final BufferConfig getBufferConfig() {
			return bufferConfig;
		}
		
		public final void setBufferConfig(final BufferConfig bufferConfig) {
			this.bufferConfig = bufferConfig;
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
		
		public final void setTimeoutMilliSec(final int timeoutMilliSec) {
			this.timeoutMilliSec = timeoutMilliSec;
		}
		
		public final void setReuseAddr(final boolean reuseAddr) {
			this.reuseAddr = reuseAddr;
		}
		
		public final void setKeepAlive(final boolean keepAlive) {
			this.keepAlive = keepAlive;
		}
		
		public final void setTcpNoDelay(final boolean tcpNoDelay) {
			this.tcpNoDelay = tcpNoDelay;
		}
		
		public final void setLinger(final int linger) {
			this.linger = linger;
		}
		
		public final void setBindBackLogSize(final int bindBackLogSize) {
			this.bindBackLogSize = bindBackLogSize;
		}
		
		public final void setInterestOpQueued(final boolean interestOpQueued) {
			this.interestOpQueued = interestOpQueued;
		}
		
		public final void setSelectInterval(final int selectInterval) {
			this.selectInterval = selectInterval;
		}
		
		@JsonProperty(KEY_TIMEOUT_MILLISEC) private int timeoutMilliSec;
		@JsonProperty(KEY_REUSE_ADDR) private boolean reuseAddr;
		@JsonProperty(KEY_KEEP_ALIVE) private boolean keepAlive;
		@JsonProperty(KEY_TCP_NO_DELAY) private boolean tcpNoDelay;
		@JsonProperty(KEY_LINGER) private int linger;
		@JsonProperty(KEY_BIND_BACKLOG_SIZE) private int bindBackLogSize;
		@JsonProperty(KEY_INTEREST_OP_QUEUED) private boolean interestOpQueued;
		@JsonProperty(KEY_SELECT_INTERVAL) private int selectInterval;

		public SocketConfig() {}

		public final int getTimeoutMillisec() {
			return timeoutMilliSec;
		}

		public final boolean getReuseAddr() {
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
		
		public final void setType(final String type) {
			this.type = type;
		}
		
		public final void setDataConfig(final DataConfig dataConfig) {
			this.dataConfig = dataConfig;
		}
		
		public final void setInput(final InputConfig input) {
			this.input = input;
		}
		
		public final void setOutput(final OutputConfig output) {
			this.output = output;
		}
		
		public final void setNamingConfig(final NamingConfig namingConfig) {
			this.namingConfig = namingConfig;
		}
		
		@JsonProperty(KEY_TYPE) private String type;
		@JsonProperty(KEY_DATA) private DataConfig dataConfig;
		@JsonProperty(KEY_INPUT) private InputConfig input;
		@JsonProperty(KEY_OUTPUT) private OutputConfig output;
		@JsonProperty(KEY_NAMING) private NamingConfig namingConfig;

		public ItemConfig() {
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
			
			public final void setContentConfig(final ContentConfig contentConfig) {
				this.contentConfig = contentConfig;
			}
			
			public final void setRanges(final DataRangesConfig ranges) {
				this.ranges = ranges;
			}
			
			public final void setSize(final SizeInBytes size) {
				this.size = size;
			}
			
			public final void setVerify(final boolean verify) {
				this.verify = verify;
			}
			
			@JsonProperty(KEY_CONTENT) private ContentConfig contentConfig;
			@JsonProperty(KEY_RANGES) @JsonDeserialize(using = DataRangesConfigDeserializer.class)
			private DataRangesConfig ranges;
			@JsonProperty(KEY_SIZE) @JsonDeserialize(using = SizeInBytesDeserializer.class)
			private SizeInBytes size;
			@JsonProperty(KEY_VERIFY) private boolean verify;

			public DataConfig() {
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
				
				public final void setFile(final String file) {
					this.file = file;
				}
				
				public final void setSeed(final String seed) {
					this.seed = seed;
				}
				
				public final void setRingSize(final SizeInBytes ringSize) {
					this.ringSize = ringSize;
				}
				
				@JsonProperty(KEY_FILE) private String file;
				@JsonProperty(KEY_SEED) private String seed;
				@JsonProperty(KEY_RING_SIZE) @JsonDeserialize(using = SizeInBytesDeserializer.class)
				private SizeInBytes ringSize;

				public ContentConfig() {
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
			
			public final void setContainer(final String container) {
				this.container = container;
			}
			
			public final void setFile(final String file) {
				this.file = file;
			}
			
			@JsonProperty(KEY_CONTAINER) private String container;
			@JsonProperty(KEY_FILE) private String file;

			public InputConfig() {
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
			
			public final void setContainer(final String container) {
				this.container = container;
			}
			
			public final void setFile(final String file) {
				this.file = file;
			}
			
			@JsonProperty(KEY_CONTAINER) private String container;
			@JsonProperty(KEY_FILE) private String file;

			public OutputConfig() {
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
			
			public final void setType(final String type) {
				this.type = type;
			}
			
			public final void setPrefix(final String prefix) {
				this.prefix = prefix;
			}
			
			public final void setRadix(final int radix) {
				this.radix = radix;
			}
			
			public final void setOffset(final long offset) {
				this.offset = offset;
			}
			
			public final void setLength(final int length) {
				this.length = length;
			}
			
			@JsonProperty(KEY_TYPE) private String type;
			@JsonProperty(KEY_PREFIX) private String prefix;
			@JsonProperty(KEY_RADIX) private int radix;
			@JsonProperty(KEY_OFFSET) private long offset;
			@JsonProperty(KEY_LENGTH) private int length;

			public NamingConfig() {
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
		public static final String KEY_CONCURRENCY = "concurrency";
		public static final String KEY_LIMIT = "limit";
		public static final String KEY_METRICS = "metrics";
		public static final String KEY_QUEUE = "queue";
		public static final String KEY_TYPE = "type";
		
		public final void setCircular(final boolean circular) {
			this.circular = circular;
		}
		
		public final void setConcurrency(final int concurrency) {
			this.concurrency = concurrency;
		}
		
		public final void setLimitConfig(final LimitConfig limitConfig) {
			this.limitConfig = limitConfig;
		}
		
		public final void setMetricsConfig(
			final MetricsConfig metricsConfig
		) {
			this.metricsConfig = metricsConfig;
		}
		
		public final void setQueueConfig(final QueueConfig queueConfig) {
			this.queueConfig = queueConfig;
		}
		
		public final void setType(final String type) {
			this.type = type;
		}
		
		@JsonProperty(KEY_CIRCULAR) private boolean circular;
		@JsonProperty(KEY_CONCURRENCY) private int concurrency;
		@JsonProperty(KEY_LIMIT) private LimitConfig limitConfig;
		@JsonProperty(KEY_METRICS) private MetricsConfig metricsConfig;
		@JsonProperty(KEY_QUEUE) private QueueConfig queueConfig;
		@JsonProperty(KEY_TYPE) private String type;
		
		public LoadConfig() {
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
		
		public final QueueConfig getQueueConfig() {
			return queueConfig;
		}

		public final static class LimitConfig {

			public static final String KEY_COUNT = "count";
			public static final String KEY_RATE = "rate";
			public static final String KEY_SIZE = "size";
			public static final String KEY_TIME = "time";
			
			public final void setCount(final long count) {
				this.count = count;
			}
			
			public final void setRate(final double rate) {
				this.rate = rate;
			}
			
			public final void setSize(final int size) {
				this.size = size;
			}
			
			public final void setTime(final long time) {
				this.time = time;
			}
			
			@JsonProperty(KEY_COUNT) private long count;
			@JsonProperty(KEY_RATE) private double rate;
			@JsonProperty(KEY_SIZE) private int size;

			@JsonDeserialize(using = TimeStrToLongDeserializer.class) @JsonProperty(KEY_TIME)
			private long time;

			public LimitConfig() {
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
			
			public final void setIntermediate(final boolean intermediate) {
				this.intermediate = intermediate;
			}
			
			public final void setPeriod(final long period) {
				this.period = period;
			}
			
			public final void setPrecondition(final boolean precondition) {
				this.precondition = precondition;
			}
			
			@JsonProperty(KEY_INTERMEDIATE) private boolean intermediate;

			@JsonDeserialize(using = TimeStrToLongDeserializer.class) @JsonProperty(KEY_PERIOD)
			private long period;

			@JsonProperty(KEY_PRECONDITION) private boolean precondition;

			public MetricsConfig() {
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
		
		public final static class QueueConfig {
			
			public static final String KEY_SIZE = "size";
			
			public final void setSize(final int size) {
				this.size = size;
			}
			
			@JsonProperty(KEY_SIZE) private int size;
			
			public QueueConfig() {
			}
			
			public final int getSize() {
				return size;
			}
		}
	}

	public final static class RunConfig {

		public static final String KEY_FILE = "file";
		public static final String KEY_ID = "id";
		
		public final void setFile(final String file) {
			this.file = file;
		}
		
		public final void setId(final String id) {
			this.id = id;
		}
		
		@JsonProperty(KEY_FILE) private String file;
		@JsonProperty(KEY_ID) private String id;

		public RunConfig() {
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
		
		public final void setAddrs(final List<String> addrs) {
			this.addrs = addrs;
		}
		
		public final void setAuthConfig(final AuthConfig authConfig) {
			this.authConfig = authConfig;
		}
		
		public final void setHttpConfig(final HttpConfig httpConfig) {
			this.httpConfig = httpConfig;
		}
		
		public final void setPort(final int port) {
			this.port = port;
		}
		
		public final void setSsl(final boolean ssl) {
			this.ssl = ssl;
		}
		
		public final void setType(final String type) {
			this.type = type;
		}
		
		public final void setMockConfig(final MockConfig mockConfig) {
			this.mockConfig = mockConfig;
		}
		
		@JsonProperty(KEY_ADDRS) private List<String> addrs;
		@JsonProperty(KEY_AUTH) private AuthConfig authConfig;
		@JsonProperty(KEY_HTTP) private HttpConfig httpConfig;
		@JsonProperty(KEY_PORT) private int port;
		@JsonProperty(KEY_SSL) private boolean ssl;
		@JsonProperty(KEY_TYPE) private String type;
		@JsonProperty(KEY_MOCK) private MockConfig mockConfig;

		public StorageConfig() {
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

		public boolean getSsl() {
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
			
			public final void setId(final String id) {
				this.id = id;
			}
			
			public final void setSecret(final String secret) {
				this.secret = secret;
			}
			
			public final void setToken(final String token) {
				this.token = token;
			}
			
			@JsonProperty(KEY_ID) private String id;
			@JsonProperty(KEY_SECRET) private String secret;
			@JsonProperty(KEY_TOKEN) private String token;

			public AuthConfig() {
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
			
			public final void setApi(final String api) {
				this.api = api;
			}
			
			public final void setFsAccess(final boolean fsAccess) {
				this.fsAccess = fsAccess;
			}
			
			public final void setNamespace(final String namespace) {
				this.namespace = namespace;
			}
			
			public final void setVersioning(final boolean versioning) {
				this.versioning = versioning;
			}
			
			public final void setHeaders(final Map<String, String> headers) {
				this.headers = headers;
			}
			
			@JsonProperty(KEY_API) private String api;
			@JsonProperty(KEY_FS_ACCESS) private boolean fsAccess;
			@JsonProperty(KEY_NAMESPACE) private String namespace;
			@JsonProperty(KEY_VERSIONING) private boolean versioning;
			@JsonProperty(KEY_HEADERS) private Map<String, String> headers;

			public HttpConfig() {
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
			
			public final void setHeadCount(final int headCount) {
				this.headCount = headCount;
			}
			
			public final void setCapacity(final int capacity) {
				this.capacity = capacity;
			}
			
			public final void setContainerConfig(
				final ContainerConfig containerConfig
			) {
				this.containerConfig = containerConfig;
			}
			
			@JsonProperty(KEY_HEAD_COUNT) private int headCount;
			@JsonProperty(KEY_CAPACITY) private int capacity;
			@JsonProperty(KEY_CONTAINER) private ContainerConfig containerConfig;

			public MockConfig() {
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
				
				public final void setCapacity(final int capacity) {
					this.capacity = capacity;
				}
				
				public final void setCountLimit(final int countLimit) {
					this.countLimit = countLimit;
				}
				
				@JsonProperty(KEY_CAPACITY) private int capacity;
				@JsonProperty(KEY_COUNT_LIMIT) private int countLimit;

				public ContainerConfig() {
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
