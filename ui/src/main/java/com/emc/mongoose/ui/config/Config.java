package com.emc.mongoose.ui.config;

import com.emc.mongoose.model.data.DataRangesConfig;
import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.api.TimeUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.emc.mongoose.ui.cli.CliArgParser.ARG_PREFIX;
import static com.emc.mongoose.ui.cli.CliArgParser.ARG_SEP;
import static org.apache.commons.lang.WordUtils.capitalize;
/**
 Created on 11.07.16.
 */
@SuppressWarnings("unused")
public final class Config {

	private static final class TimeStrToLongDeserializer
	extends JsonDeserializer<Long> {
		@Override
		public final Long deserialize(
			final JsonParser p, final DeserializationContext ctx
		) throws IOException {
			return TimeUtil.getTimeInSeconds(p.getValueAsString());
		}
	}

	private static final class SizeInBytesDeserializer
	extends JsonDeserializer<SizeInBytes> {
		@Override
		public final SizeInBytes deserialize(
			final JsonParser p, final DeserializationContext ctx
		) throws IOException {
			return new SizeInBytes(p.getValueAsString());
		}
	}

	private static final class DataRangesConfigDeserializer
	extends JsonDeserializer<DataRangesConfig> {
		@Override
		public final DataRangesConfig deserialize(
			final JsonParser p, final DeserializationContext ctx
		) throws IOException {
			try {
				return new DataRangesConfig(p.getValueAsInt());
			} catch(final IOException ignored) {
				return new DataRangesConfig(p.getValueAsString());
			}
		}
	}

	public static final String KEY_NAME = "name";
	public static final String KEY_VERSION = "version";
	public static final String KEY_SOCKET = "socket";
	public static final String KEY_ITEM = "item";
	public static final String KEY_LOAD = "load";
	public static final String KEY_RUN = "run";
	public static final String KEY_STORAGE = "storage";
	public static final String KEY_ALIASING = "aliasing";
	
	@JsonProperty(KEY_NAME) private String name;
	@JsonProperty(KEY_VERSION) private String version;
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
	
	public static final class SocketConfig
	implements Serializable {

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

		public final int getTimeoutMilliSec() {
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

	public static final class ItemConfig
	implements Serializable {

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

		public static final class DataConfig
		implements Serializable {

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

			public static final class ContentConfig
			implements Serializable {

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

		public static final class InputConfig
		implements Serializable {

			public static final String KEY_PATH = "path";
			public static final String KEY_FILE = "file";
			
			public final void setPath(final String path) {
				this.path = path;
			}
			
			public final void setFile(final String file) {
				this.file = file;
			}
			
			@JsonProperty(KEY_PATH) private String path;
			@JsonProperty(KEY_FILE) private String file;

			public InputConfig() {
			}

			public final String getPath() {
				return path;
			}

			public final String getFile() {
				return file;
			}

		}

		public static final class OutputConfig
		implements Serializable {

			public static final String KEY_PATH = "path";
			public static final String KEY_FILE = "file";
			
			public final void setPath(final String path) {
				this.path = path;
			}
			
			public final void setFile(final String file) {
				this.file = file;
			}
			
			@JsonProperty(KEY_PATH) private String path;
			@JsonProperty(KEY_FILE) private String file;

			public OutputConfig() {
			}

			public String getPath() {
				return path;
			}

			public String getFile() {
				return file;
			}
		}
		
		public static final class NamingConfig
		implements Serializable {

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

	public static final class LoadConfig
	implements Serializable {

		public static final String KEY_CIRCULAR = "circular";
		public static final String KEY_CONCURRENCY = "concurrency";
		public static final String KEY_LIMIT = "limit";
		public static final String KEY_GENERATOR = "generator";
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

		public final void setGeneratorConfig(final GeneratorConfig generatorConfig) {
			this.generatorConfig = generatorConfig;
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
		@JsonProperty(KEY_GENERATOR) private GeneratorConfig generatorConfig;
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

		public final GeneratorConfig getGeneratorConfig() {
			return generatorConfig;
		}

		public final MetricsConfig getMetricsConfig() {
			return metricsConfig;
		}
		
		public final QueueConfig getQueueConfig() {
			return queueConfig;
		}

		public static final class LimitConfig
		implements Serializable {

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
			
			public final void setSize(final SizeInBytes size) {
				this.size = size;
			}
			
			public final void setTime(final long time) {
				this.time = time;
			}
			
			@JsonProperty(KEY_COUNT) private long count;
			@JsonProperty(KEY_RATE) private double rate;
			
			@JsonDeserialize(using = SizeInBytesDeserializer.class) @JsonProperty(KEY_SIZE)
			private SizeInBytes size;

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

			public final SizeInBytes getSize() {
				return size;
			}

			public final long getTime() {
				return time;
			}
		}

		public static final class GeneratorConfig
		implements Serializable {

			public static final String KEY_REMOTE = "remote";
			public static final String KEY_ADDRS = "addrs";

			public final void setAddrs(final List<String> addrs) {
				this.addrs = addrs;
			}

			public final void setRemote(final boolean remote) {
				this.remote = remote;
			}

			@JsonProperty(KEY_ADDRS) private List<String> addrs;
			@JsonProperty(KEY_REMOTE) private boolean remote;

			public GeneratorConfig() {
			}

			public List<String> getAddrs() {
				return addrs;
			}

			public boolean getRemote() {
				return remote;
			}
		}

		public static final class MetricsConfig
		implements Serializable {

			public static final String KEY_THRESHOLD = "threshold";
			public static final String KEY_PERIOD = "period";
			public static final String KEY_PRECONDITION= "precondition";
			
			public final void setThreshold(final double threshold) {
				this.threshold = threshold;
			}
			
			public final void setPeriod(final long period) {
				this.period = period;
			}
			
			public final void setPrecondition(final boolean precondition) {
				this.precondition = precondition;
			}
			
			@JsonProperty(KEY_THRESHOLD) private double threshold;

			@JsonDeserialize(using = TimeStrToLongDeserializer.class) @JsonProperty(KEY_PERIOD)
			private long period;

			@JsonProperty(KEY_PRECONDITION) private boolean precondition;

			public MetricsConfig() {
			}

			public final double getThreshold() {
				return threshold;
			}

			public final long getPeriod() {
				return period;
			}

			public final boolean getPrecondition() {
				return precondition;
			}
		}
		
		public static final class QueueConfig
		implements Serializable {
			
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

	public static final class RunConfig
	implements Serializable {

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

	public static final class StorageConfig
	implements Serializable {

		public static final String KEY_AUTH = "auth";
		public static final String KEY_HTTP = "http";
		public static final String KEY_NODE = "node";
		public static final String KEY_DRIVER = "driver";
		public static final String KEY_PORT = "port";
		public static final String KEY_SSL = "ssl";
		public static final String KEY_TYPE = "type";
		public static final String KEY_MOCK = "mock";
		
		public final void setAuthConfig(final AuthConfig authConfig) {
			this.authConfig = authConfig;
		}
		
		public final void setHttpConfig(final HttpConfig httpConfig) {
			this.httpConfig = httpConfig;
		}
		
		public final void setNodeConfig(final NodeConfig nodeConfig) {
			this.nodeConfig = nodeConfig;
		}
		
		public final void setDriverConfig(final DriverConfig driverConfig) {
			this.driverConfig = driverConfig;
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

		@JsonProperty(KEY_AUTH) private AuthConfig authConfig;
		@JsonProperty(KEY_HTTP) private HttpConfig httpConfig;
		@JsonProperty(KEY_NODE) private NodeConfig nodeConfig;
		@JsonProperty(KEY_DRIVER) private DriverConfig driverConfig;
		@JsonProperty(KEY_PORT) private int port;
		@JsonProperty(KEY_SSL) private boolean ssl;
		@JsonProperty(KEY_TYPE) private String type;
		@JsonProperty(KEY_MOCK) private MockConfig mockConfig;


		public StorageConfig() {
		}

		public AuthConfig getAuthConfig() {
			return authConfig;
		}

		public HttpConfig getHttpConfig() {
			return httpConfig;
		}

		public NodeConfig getNodeConfig() {
			return nodeConfig;
		}

		public DriverConfig getDriverConfig() {
			return driverConfig;
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

		public static final class AuthConfig
		implements Serializable {

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

		public static final class HttpConfig
		implements Serializable {

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
		
		public static final class NodeConfig
		implements Serializable {

			public static final String KEY_ADDRS = "addrs";

			public final void setAddrs(final List<String> addrs) {
				this.addrs = addrs;
			}

			@JsonProperty(KEY_ADDRS) private List<String> addrs;
			
			public NodeConfig() {
			}
			
			public List<String> getAddrs() {
				return addrs;
			}
		}
		
		public static final class DriverConfig
		implements Serializable {
			
			public static final String KEY_REMOTE = "remote";
			public static final String KEY_ADDRS = "addrs";

			public final void setAddrs(final List<String> addrs) {
				this.addrs = addrs;
			}

			public final void setRemote(final boolean remote) {
				this.remote = remote;
			}

			@JsonProperty(KEY_ADDRS) private List<String> addrs;
			@JsonProperty(KEY_REMOTE) private boolean remote;

			public DriverConfig() {
			}

			public List<String> getAddrs() {
				return addrs;
			}

			public boolean getRemote() {
				return remote;
			}
		}

		public static final class MockConfig
		implements Serializable {

			public static final String KEY_HEAD_COUNT = "headCount";
			public static final String KEY_CAPACITY = "capacity";
			public static final String KEY_CONTAINER = "container";
			public static final String KEY_NODE = "node";
			
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

			public final void setNode(final boolean node) {
				this.node = node;
			}
			
			@JsonProperty(KEY_HEAD_COUNT) private int headCount;
			@JsonProperty(KEY_CAPACITY) private int capacity;
			@JsonProperty(KEY_CONTAINER) private ContainerConfig containerConfig;
			@JsonProperty(KEY_NODE) private boolean node;

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

			public boolean getNode() {
				return node;
			}

			public static final class ContainerConfig
			implements Serializable {

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
	
	public void apply(final Map<String, Object> tree)
	throws InvocationTargetException, IllegalAccessException, IllegalStateException {
		try {
			applyRecursively(this, tree);
		} catch(final IllegalArgumentNameException e) {
			throw new IllegalArgumentNameException(ARG_PREFIX + e.getMessage());
		}
	}

	private static void applyRecursively(final Object config, final Map<String, Object> branch)
	throws InvocationTargetException, IllegalAccessException {
		final Class configCls = config.getClass();
		for(final String key : branch.keySet()) {
			final Object node = branch.get(key);
			if(node instanceof Map) {
				final Map<String, Object> childBranch = (Map<String, Object>) node;
				try {
					final Method subConfigGetter = configCls.getMethod(
						"get" + capitalize(key) + "Config"
					);
					final Object subConfig = subConfigGetter.invoke(config);
					try {
						applyRecursively(subConfig, childBranch);
					} catch(final IllegalArgumentNameException e) {
						throw new IllegalArgumentNameException(key + ARG_SEP + e.getMessage());
					}
				} catch(final NoSuchMethodException e) {
					throw new IllegalArgumentNameException(key);
				}
			} else if(node instanceof String) {
				applyField(config, key, (String) node);
			} else {
				throw new IllegalStateException();
			}
		}
	}

	private static void applyField(final Object config, final String key, final String value)
	throws InvocationTargetException, IllegalAccessException {
		final Class configCls = config.getClass();
		try {
			final Method fieldGetter = configCls.getMethod("get" + capitalize(key));
			final Class fieldType = fieldGetter.getReturnType();
			if(fieldType.equals(String.class)) {
				configCls.getMethod("set" + capitalize(key), String.class).invoke(config, value);
			} else if(fieldType.equals(List.class)) {
				final List<String> listValue = Arrays.asList(value.split(","));
				configCls.getMethod("set" + capitalize(key), List.class).invoke(config, listValue);
			} else if(fieldType.equals(Map.class)) {
				final Map<String, String> field = (Map<String, String>) fieldGetter.invoke(config);
				final String keyValuePair[] = value.split(":", 2);
				if(keyValuePair.length == 1) {
					field.remove(keyValuePair[0]);
				} else if(keyValuePair.length == 2) {
					field.put(keyValuePair[0], keyValuePair[1]);
				}
			} else if(fieldType.equals(Integer.TYPE)) {
				final int intValue = Integer.parseInt(value);
				configCls.getMethod("set" + capitalize(key), Integer.TYPE).invoke(config, intValue);
			} else if(fieldType.equals(Long.TYPE)) {
				try {
					final long longValue = Long.parseLong(value);
					configCls.getMethod("set" + capitalize(key), Long.TYPE).invoke(config, longValue);
				} catch(final NumberFormatException e) {
					final long timeValue = TimeUtil.getTimeInSeconds(value);
					configCls.getMethod("set" + capitalize(key), Long.TYPE).invoke(config, timeValue);
				}
			} else if(fieldType.equals(Double.TYPE)) {
				final double doubleValue = Double.parseDouble(value);
				configCls.getMethod("set" + capitalize(key), Double.TYPE).invoke(config, doubleValue);
			} else if(fieldType.equals(Boolean.TYPE)) {
				final boolean flagValue = Boolean.parseBoolean(value);
				configCls.getMethod("set" + capitalize(key), Boolean.TYPE).invoke(config, flagValue);
			} else if(fieldType.equals(SizeInBytes.class)) {
				final SizeInBytes sizeValue = new SizeInBytes(value);
				configCls.getMethod("set" + capitalize(key), SizeInBytes.class).invoke(config, sizeValue);
			} else if(fieldType.equals(DataRangesConfig.class)) {
				try {
					final int rangesCount = Integer.parseInt(value);
					configCls.getMethod("set" + capitalize(key), DataRangesConfig.class).invoke(
						config, new DataRangesConfig(rangesCount));
				} catch(final NumberFormatException e) {
					final DataRangesConfig rangesValue = new DataRangesConfig(value);
					configCls.getMethod("set" + capitalize(key), DataRangesConfig.class).invoke(
						config, rangesValue);
				}
			} else {
				throw new IllegalStateException(
					"Field type is \"" + fieldType.getName() + "\" for key: " + key);
			}
		} catch(final NoSuchMethodException e) {
			throw new IllegalArgumentNameException(key);
		}
	}
}
