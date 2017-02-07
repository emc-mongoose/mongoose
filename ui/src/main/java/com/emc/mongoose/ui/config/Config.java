package com.emc.mongoose.ui.config;

import com.emc.mongoose.common.reflection.TypeUtil;
import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.api.TimeUtil;
import static com.emc.mongoose.ui.cli.CliArgParser.ARG_PREFIX;
import static com.emc.mongoose.ui.cli.CliArgParser.ARG_SEP;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static org.apache.commons.lang.WordUtils.capitalize;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 Created on 11.07.16.
 */
public final class Config
implements Serializable {

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

	private static final class SizeInBytesSerializer
	extends JsonSerializer<SizeInBytes> {
		@Override
		public final void serialize(
			final SizeInBytes value, final JsonGenerator gen, final SerializerProvider serializers
		) throws IOException, JsonProcessingException {
			gen.writeString(value.toString());
		}
	}

	public static final String KEY_VERSION = "version";
	public static final String KEY_ITEM = "item";
	public static final String KEY_LOAD = "load";
	public static final String SCENARIO = "scenario";
	public static final String KEY_STORAGE = "storage";
	public static final String KEY_ALIASING = "aliasing";
	
	@JsonProperty(KEY_ITEM) private ItemConfig itemConfig;
	@JsonProperty(KEY_LOAD) private LoadConfig loadConfig;
	@JsonProperty(SCENARIO) private ScenarioConfig scenarioConfig;
	@JsonProperty(KEY_STORAGE) private StorageConfig storageConfig;
	@JsonProperty(KEY_VERSION) private String version;
	@JsonProperty(KEY_ALIASING) private List<Map<String, Object>> aliasingConfig;

	public Config() {}

	public Config(final Config config) {
		this.version = config.getVersion();
		this.itemConfig = new ItemConfig(config.getItemConfig());
		this.loadConfig = new LoadConfig(config.getLoadConfig());
		this.scenarioConfig = new ScenarioConfig(config.getScenarioConfig());
		this.storageConfig = new StorageConfig(config.getStorageConfig());
		final List<Map<String, Object>> ac = config.getAliasingConfig();
		this.aliasingConfig = ac == null ? null : new ArrayList<>(config.getAliasingConfig());
	}

	public final String getVersion() {
		return version;
	}

	public final StorageConfig getStorageConfig() {
		return storageConfig;
	}

	public final LoadConfig getLoadConfig() {
		return loadConfig;
	}

	public final ScenarioConfig getScenarioConfig() {
		return scenarioConfig;
	}

	public final ItemConfig getItemConfig() {
		return itemConfig;
	}

	public final List<Map<String, Object>> getAliasingConfig() {
		return aliasingConfig;
	}
	
	public final void setVersion(final String version) {
		this.version = version;
	}
	
	public final void setStorageConfig(final StorageConfig storageConfig) {
		this.storageConfig = storageConfig;
	}
	
	public final void setLoadConfig(final LoadConfig loadConfig) {
		this.loadConfig = loadConfig;
	}
	
	public final void setScenarioConfig(final ScenarioConfig scenarioConfig) {
		this.scenarioConfig = scenarioConfig;
	}
	
	public final void setItemConfig(final ItemConfig itemConfig) {
		this.itemConfig = itemConfig;
	}
	
	public final void setAliasingConfig(final List<Map<String, Object>> aliasingConfig) {
		this.aliasingConfig = aliasingConfig;
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
		
		public final void setInputConfig(final InputConfig inputConfig) {
			this.inputConfig = inputConfig;
		}
		
		public final void setOutputConfig(final OutputConfig outputConfig) {
			this.outputConfig = outputConfig;
		}
		
		public final void setNamingConfig(final NamingConfig namingConfig) {
			this.namingConfig = namingConfig;
		}
		
		@JsonProperty(KEY_TYPE) private String type;
		@JsonProperty(KEY_DATA) private DataConfig dataConfig;
		@JsonProperty(KEY_INPUT) private InputConfig inputConfig;
		@JsonProperty(KEY_OUTPUT) private OutputConfig outputConfig;
		@JsonProperty(KEY_NAMING) private NamingConfig namingConfig;

		public ItemConfig() {
		}

		public ItemConfig(final ItemConfig other) {
			this.type = other.getType();
			this.dataConfig = new DataConfig(other.getDataConfig());
			this.inputConfig = new InputConfig(other.getInputConfig());
			this.outputConfig = new OutputConfig(other.getOutputConfig());
			this.namingConfig = new NamingConfig(other.getNamingConfig());
		}

		public final String getType() {
			return type;
		}

		public final DataConfig getDataConfig() {
			return dataConfig;
		}

		public final InputConfig getInputConfig() {
			return inputConfig;
		}

		public final OutputConfig getOutputConfig() {
			return outputConfig;
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
			
			public final void setRangesConfig(final RangesConfig rangesConfig) {
				this.rangesConfig = rangesConfig;
			}
			
			public final void setSize(final SizeInBytes size) {
				this.size = size;
			}

			public final void setVerify(final boolean verify) {
				this.verify = verify;
			}
			
			@JsonProperty(KEY_CONTENT) private ContentConfig contentConfig;

			@JsonProperty(KEY_RANGES) private RangesConfig rangesConfig;

			@JsonProperty(KEY_SIZE)
			@JsonDeserialize(using = SizeInBytesDeserializer.class)
			@JsonSerialize(using = SizeInBytesSerializer.class)
			private SizeInBytes size;

			@JsonProperty(KEY_VERIFY) private boolean verify;

			public DataConfig() {
			}

			public DataConfig(final DataConfig other) {
				this.contentConfig = new ContentConfig(other.getContentConfig());
				this.rangesConfig = new RangesConfig(other.getRangesConfig());
				this.size = new SizeInBytes(other.getSize());
				this.verify = other.getVerify();
			}

			public ContentConfig getContentConfig() {
				return contentConfig;
			}

			public final RangesConfig getRangesConfig() {
				return rangesConfig;
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

				@JsonProperty(KEY_RING_SIZE)
				@JsonDeserialize(using = SizeInBytesDeserializer.class)
				@JsonSerialize(using = SizeInBytesSerializer.class)
				private SizeInBytes ringSize;

				public ContentConfig() {
				}

				public ContentConfig(final ContentConfig other) {
					this.file = other.getFile();
					this.seed = other.getSeed();
					this.ringSize = new SizeInBytes(other.getRingSize());
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

			public static final class RangesConfig
			implements Serializable {

				public static final String KEY_FIXED = "fixed";
				public static final String KEY_RANDOM = "random";
				public static final String KEY_THRESHOLD = "threshold";

				@JsonProperty(KEY_FIXED) private List<String> fixed;

				@JsonProperty(KEY_RANDOM) private int random;

				@JsonProperty(KEY_THRESHOLD)
				@JsonDeserialize(using = SizeInBytesDeserializer.class)
				@JsonSerialize(using = SizeInBytesSerializer.class)
				private SizeInBytes threshold;

				public RangesConfig() {
				}

				public RangesConfig(final RangesConfig other) {
					final List<String> otherRanges = other.getFixed();
					this.fixed = otherRanges == null ? null : new ArrayList<>(otherRanges);
					this.random = other.getRandom();
					this.threshold = new SizeInBytes(other.getThreshold());
				}

				public final List<String> getFixed() {
					return fixed;
				}

				public final void setFixed(final List<String> fixed) {
					this.fixed = fixed;
				}

				public final int getRandom() {
					return random;
				}

				public final void setRandom(final int random) {
					this.random = random;
				}

				public final SizeInBytes getThreshold() {
					return threshold;
				}

				public final void setThreshold(final SizeInBytes threshold) {
					this.threshold = threshold;
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

			public InputConfig(final InputConfig other) {
				this.path = other.getPath();
				this.file = other.getFile();
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

			public static final String KEY_DELAY = "delay";
			public static final String KEY_FILE = "file";
			public static final String KEY_PATH = "path";

			public final void setDelay(final long delay) {
				this.delay = delay;
			}

			public final void setFile(final String file) {
				this.file = file;
			}

			public final void setPath(final String path) {
				this.path = path;
			}

			@JsonProperty(KEY_DELAY)
			@JsonDeserialize(using=TimeStrToLongDeserializer.class)
			private long delay;

			@JsonProperty(KEY_FILE)
			private String file;

			@JsonProperty(KEY_PATH)
			private String path;

			public OutputConfig() {
			}

			public OutputConfig(final OutputConfig other) {
				this.delay = other.getDelay();
				this.file = other.getFile();
				this.path = other.getPath();
			}

			public long getDelay() {
				return delay;
			}

			public String getFile() {
				return file;
			}

			public String getPath() {
				return path;
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

			public NamingConfig(final NamingConfig other) {
				this.type = other.getType();
				this.prefix = other.getPrefix();
				this.radix = other.getRadix();
				this.offset = other.getOffset();
				this.length = other.getLength();
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
		public static final String KEY_GENERATOR = "generator";
		public static final String KEY_JOB = "job";
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

		public final void setGeneratorConfig(final GeneratorConfig generatorConfig) {
			this.generatorConfig = generatorConfig;
		}

		public final void setJobConfig(final JobConfig jobConfig) {
			this.jobConfig = jobConfig;
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
		@JsonProperty(KEY_GENERATOR) private GeneratorConfig generatorConfig;
		@JsonProperty(KEY_JOB) private JobConfig jobConfig;
		@JsonProperty(KEY_LIMIT) private LimitConfig limitConfig;
		@JsonProperty(KEY_METRICS) private MetricsConfig metricsConfig;
		@JsonProperty(KEY_QUEUE) private QueueConfig queueConfig;
		@JsonProperty(KEY_TYPE) private String type;
		
		public LoadConfig() {
		}

		public LoadConfig(final LoadConfig other) {
			this.circular = other.getCircular();
			this.concurrency = other.getConcurrency();
			this.generatorConfig = new GeneratorConfig(other.getGeneratorConfig());
			this.jobConfig = new JobConfig(other.getJobConfig());
			this.limitConfig = new LimitConfig(other.getLimitConfig());
			this.metricsConfig = new MetricsConfig(other.getMetricsConfig());
			this.queueConfig = new QueueConfig(other.getQueueConfig());
			this.type = other.getType();
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

		public final GeneratorConfig getGeneratorConfig() {
			return generatorConfig;
		}

		public final JobConfig getJobConfig() {
			return jobConfig;
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

		public static final class GeneratorConfig
			implements Serializable {

			public static final String KEY_ADDRS = "addrs";
			public static final String KEY_REMOTE = "remote";
			public static final String KEY_SHUFFLE = "shuffle";

			public final void setAddrs(final List<String> addrs) {
				this.addrs = addrs;
			}

			public final void setRemote(final boolean remote) {
				this.remote = remote;
			}

			public final void setShuffle(final boolean shuffle) {
				this.shuffle = shuffle;
			}

			@JsonProperty(KEY_ADDRS) private List<String> addrs;
			@JsonProperty(KEY_REMOTE) private boolean remote;
			@JsonProperty(KEY_SHUFFLE) private boolean shuffle;

			public GeneratorConfig() {
			}

			public GeneratorConfig(final GeneratorConfig other) {
				this.addrs = new ArrayList<>(other.getAddrs());
				this.remote = other.getRemote();
				this.shuffle = other.getShuffle();
			}

			public List<String> getAddrs() {
				return addrs;
			}

			public boolean getRemote() {
				return remote;
			}

			public boolean getShuffle() {
				return shuffle;
			}
		}

		public static final class JobConfig
		implements Serializable {

			public JobConfig() {
			}

			public JobConfig(final JobConfig other) {
				this.name = other.getName();
			}

			public static final String KEY_NAME = "name";

			public final void setName(final String name) {
				this.name = name;
			}

			@JsonProperty(KEY_NAME) private String name;

			public final String getName() {
				return name;
			}
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
			
			@JsonDeserialize(using = SizeInBytesDeserializer.class)
			@JsonSerialize(using = SizeInBytesSerializer.class)
			@JsonProperty(KEY_SIZE)
			private SizeInBytes size;

			@JsonDeserialize(using = TimeStrToLongDeserializer.class) @JsonProperty(KEY_TIME)
			private long time;

			public LimitConfig() {
			}

			public LimitConfig(final LimitConfig other) {
				this.count = other.getCount();
				this.time = other.getTime();
				this.rate = other.getRate();
				this.size = new SizeInBytes(other.getSize());
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

		public static final class MetricsConfig
		implements Serializable {

			public static final String KEY_PERIOD = "period";
			public static final String KEY_PRECONDITION= "precondition";
			public static final String KEY_THRESHOLD = "threshold";
			public static final String KEY_TRACE = "trace";

			public final void setPeriod(final long period) {
				this.period = period;
			}
			
			public final void setPrecondition(final boolean precondition) {
				this.precondition = precondition;
			}

			public final void setThreshold(final double threshold) {
				this.threshold = threshold;
			}

			public final void setKeyTrace(final TraceConfig traceConfig) {
				this.traceConfig = traceConfig;
			}

			@JsonDeserialize(using = TimeStrToLongDeserializer.class) @JsonProperty(KEY_PERIOD)
			private long period;

			@JsonProperty(KEY_PRECONDITION) private boolean precondition;

			@JsonProperty(KEY_THRESHOLD) private double threshold;

			@JsonProperty(KEY_TRACE) private TraceConfig traceConfig;

			public MetricsConfig() {
			}

			public MetricsConfig(final MetricsConfig other) {
				this.threshold = other.getThreshold();
				this.period = other.getPeriod();
				this.precondition = other.getPrecondition();
				this.traceConfig = new TraceConfig(other.getTraceConfig());
			}

			public final long getPeriod() {
				return period;
			}

			public final boolean getPrecondition() {
				return precondition;
			}

			public final double getThreshold() {
				return threshold;
			}

			public final TraceConfig getTraceConfig() {
				return traceConfig;
			}

			public final static class TraceConfig
			implements Serializable {

				public static final String KEY_STORAGE_DRIVER = "storageDriver";
				public static final String KEY_STORAGE_NODE = "storageNode";
				public static final String KEY_ITEM_PATH = "itemPath";
				public static final String KEY_ITEM_INFO = "itemInfo";
				public static final String KEY_IO_TYPE_CODE = "ioTypeCode";
				public static final String KEY_STATUS_CODE = "statusCode";
				public static final String KEY_REQ_TIME_START = "reqTimeStart";
				public static final String KEY_DURATION = "duration";
				public static final String KEY_RESP_LATENCY = "respLatency";
				public static final String KEY_DATA_LATENCY = "dataLatency";
				public static final String KEY_TRANSFER_SIZE = "transferSize";

				@JsonProperty(KEY_STORAGE_DRIVER) private boolean storageDriver;
				@JsonProperty(KEY_STORAGE_NODE) private boolean storageNode;
				@JsonProperty(KEY_ITEM_PATH) private boolean itemPath;
				@JsonProperty(KEY_ITEM_INFO) private boolean itemInfo;
				@JsonProperty(KEY_IO_TYPE_CODE) private boolean ioTypeCode;
				@JsonProperty(KEY_STATUS_CODE) private boolean statusCode;
				@JsonProperty(KEY_REQ_TIME_START) private boolean reqTimeStart;
				@JsonProperty(KEY_DURATION) private boolean duration;
				@JsonProperty(KEY_RESP_LATENCY) private boolean respLatency;
				@JsonProperty(KEY_DATA_LATENCY) private boolean dataLatency;
				@JsonProperty(KEY_TRANSFER_SIZE) private boolean transferSize;

				public TraceConfig() {
				}

				public TraceConfig(final TraceConfig other) {
					this.storageDriver = other.getStorageDriver();
					this.storageNode = other.getStorageNode();
					this.itemPath = other.getItemPath();
					this.itemInfo = other.getItemInfo();
					this.ioTypeCode = other.getIoTypeCode();
					this.statusCode = other.getStatusCode();
					this.reqTimeStart = other.getReqTimeStart();
					this.duration = other.getDuration();
					this.respLatency = other.getRespLatency();
					this.dataLatency = other.getDataLatency();
					this.transferSize = other.getTransferSize();
				}
				
				public final boolean getStorageDriver() {
					return storageDriver;
				}
				public final void setStorageDriver(final boolean storageDriver) {
					this.storageDriver = storageDriver;
				}
				public final boolean getStorageNode() {
					return storageNode;
				}
				public final void setStorageNode(final boolean storageNode) {
					this.storageNode = storageNode;
				}
				public final boolean getItemPath() {
					return itemPath;
				}
				public final void setItemPath(final boolean itemPath) {
					this.itemPath = itemPath;
				}
				public final boolean getItemInfo() {
					return itemInfo;
				}
				public final void setItemInfo(final boolean itemInfo) {
					this.itemInfo = itemInfo;
				}
				public final boolean getIoTypeCode() {
					return ioTypeCode;
				}
				public final void setIoTypeCode(final boolean ioTypeCode) {
					this.ioTypeCode = ioTypeCode;
				}
				public final boolean getStatusCode() {
					return statusCode;
				}
				public final void setStatusCode(final boolean statusCode) {
					this.statusCode = statusCode;
				}
				public final boolean getReqTimeStart() {
					return reqTimeStart;
				}
				public final void setReqTimeStart(final boolean reqTimeStart) {
					this.reqTimeStart = reqTimeStart;
				}
				public final boolean getDuration() {
					return duration;
				}
				public final void setDuration(final boolean duration) {
					this.duration = duration;
				}
				public final boolean getRespLatency() {
					return respLatency;
				}
				public final void setRespLatency(final boolean respLatency) {
					this.respLatency = respLatency;
				}
				public final boolean getDataLatency() {
					return dataLatency;
				}
				public final void setDataLatency(final boolean dataLatency) {
					this.dataLatency = dataLatency;
				}
				public final boolean getTransferSize() {
					return transferSize;
				}
				public final void setTransferSize(final boolean transferSize) {
					this.transferSize = transferSize;
				}
			}
		}
		
		public static final class QueueConfig
		implements Serializable {

			public static final String KEY_SIZE = "size";

			public final void setSize(final int size) {
				this.size = size;
			}
			
			@JsonProperty(KEY_SIZE)
			private int size;
			
			public QueueConfig() {
			}

			public QueueConfig(final QueueConfig other) {
				this.size = other.getSize();
			}
			
			public final int getSize() {
				return size;
			}
		}
	}

	public static final class ScenarioConfig
	implements Serializable {

		public static final String KEY_FILE = "file";
		
		public final void setFile(final String file) {
			this.file = file;
		}
		
		@JsonProperty(KEY_FILE) private String file;

		public ScenarioConfig() {
		}

		public ScenarioConfig(final ScenarioConfig other) {
			this.file = other.getFile();
		}

		public final String getFile() {
			return file;
		}
	}

	public static final class StorageConfig
	implements Serializable {

		public static final String KEY_AUTH = "auth";
		public static final String KEY_NET = "net";
		public static final String KEY_DRIVER = "driver";
		public static final String KEY_TYPE = "type";
		public static final String KEY_MOCK = "mock";
		
		public final void setAuthConfig(final AuthConfig authConfig) {
			this.authConfig = authConfig;
		}
		
		public final void setNetConfig(final NetConfig netConfig) {
			this.netConfig = netConfig;
		}
		
		public final void setDriverConfig(final DriverConfig driverConfig) {
			this.driverConfig = driverConfig;
		}
		
		public final void setType(final String type) {
			this.type = type;
		}
		
		public final void setMockConfig(final MockConfig mockConfig) {
			this.mockConfig = mockConfig;
		}

		@JsonProperty(KEY_AUTH) private AuthConfig authConfig;
		@JsonProperty(KEY_NET) private NetConfig netConfig;
		@JsonProperty(KEY_DRIVER) private DriverConfig driverConfig;
		@JsonProperty(KEY_TYPE) private String type;
		@JsonProperty(KEY_MOCK) private MockConfig mockConfig;


		public StorageConfig() {
		}

		public StorageConfig(final StorageConfig other) {
			this.authConfig = new AuthConfig(other.getAuthConfig());
			this.netConfig = new NetConfig(other.getNetConfig());
			this.driverConfig = new DriverConfig(other.getDriverConfig());
			this.type = other.getType();
			this.mockConfig = new MockConfig(other.getMockConfig());
		}

		public AuthConfig getAuthConfig() {
			return authConfig;
		}

		public NetConfig getNetConfig() {
			return netConfig;
		}

		public DriverConfig getDriverConfig() {
			return driverConfig;
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

			public AuthConfig(final AuthConfig other) {
				this.id = other.getId();
				this.secret = other.getSecret();
				this.token = other.getToken();
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
		
		public static final class NetConfig
		implements Serializable {
			
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
				
				public HttpConfig(final HttpConfig other) {
					this.api = other.getApi();
					this.fsAccess = other.getFsAccess();
					this.namespace = other.getNamespace();
					this.versioning = other.getVersioning();
					this.headers = new HashMap<>(other.getHeaders());
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
				public static final String KEY_PORT = "port";
				
				public final void setAddrs(final List<String> addrs) {
					this.addrs = addrs;
				}
				
				public final void setPort(final int port) {
					this.port = port;
				}
				
				@JsonProperty(KEY_ADDRS) private List<String> addrs;
				@JsonProperty(KEY_PORT) private int port;
				
				public NodeConfig() {
				}
				
				public NodeConfig(final NodeConfig other) {
					this.addrs = new ArrayList<>(other.getAddrs());
					this.port = other.getPort();
				}
				
				public List<String> getAddrs() {
					return addrs;
				}
				
				public int getPort() {
					return port;
				}
			}
			
			public static final String KEY_TIMEOUT_MILLISEC = "timeoutMilliSec";
			public static final String KEY_REUSE_ADDR = "reuseAddr";
			public static final String KEY_KEEP_ALIVE = "keepAlive";
			public static final String KEY_TCP_NO_DELAY = "tcpNoDelay";
			public static final String KEY_LINGER = "linger";
			public static final String KEY_BIND_BACKLOG_SIZE = "bindBacklogSize";
			public static final String KEY_INTEREST_OP_QUEUED = "interestOpQueued";
			public static final String KEY_RCVBUF = "rcvBuf";
			public static final String KEY_SNDBUF = "sndBuf";
			public static final String KEY_SSL = "ssl";
			public static final String KEY_HTTP = "http";
			public static final String KEY_NODE = "node";
			
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
			
			public final SizeInBytes getRcvBuf() {
				return rcvBuf;
			}
			
			public final SizeInBytes getSndBuf() {
				return sndBuf;
			}
			
			public boolean getSsl() {
				return ssl;
			}
			
			public HttpConfig getHttpConfig() {
				return httpConfig;
			}
			
			public NodeConfig getNodeConfig() {
				return nodeConfig;
			}
			
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
			
			public final void setRcvBuf(final SizeInBytes rcvBuf) {
				this.rcvBuf = rcvBuf;
			}
			
			public final void setSndBuf(final SizeInBytes sndBuf) {
				this.sndBuf = sndBuf;
			}
			
			public final void setSsl(final boolean ssl) {
				this.ssl = ssl;
			}
			
			public final void setHttpConfig(final HttpConfig httpConfig) {
				this.httpConfig = httpConfig;
			}
			
			public final void setNodeConfig(final NodeConfig nodeConfig) {
				this.nodeConfig = nodeConfig;
			}
			
			@JsonProperty(KEY_TIMEOUT_MILLISEC) private int timeoutMilliSec;
			
			@JsonProperty(KEY_REUSE_ADDR) private boolean reuseAddr;
			
			@JsonProperty(KEY_KEEP_ALIVE) private boolean keepAlive;
			
			@JsonProperty(KEY_TCP_NO_DELAY) private boolean tcpNoDelay;
			
			@JsonProperty(KEY_LINGER) private int linger;
			
			@JsonProperty(KEY_BIND_BACKLOG_SIZE) private int bindBackLogSize;
			
			@JsonProperty(KEY_INTEREST_OP_QUEUED) private boolean interestOpQueued;
			
			@JsonProperty(KEY_RCVBUF)
			@JsonDeserialize(using = SizeInBytesDeserializer.class)
			@JsonSerialize(using = SizeInBytesSerializer.class)
			private SizeInBytes rcvBuf;
			
			@JsonProperty(KEY_SNDBUF)
			@JsonDeserialize(using = SizeInBytesDeserializer.class)
			@JsonSerialize(using = SizeInBytesSerializer.class)
			private SizeInBytes sndBuf;
			
			@JsonProperty(KEY_SSL) private boolean ssl;
			@JsonProperty(KEY_HTTP) private HttpConfig httpConfig;
			@JsonProperty(KEY_NODE) private NodeConfig nodeConfig;
			
			public NetConfig() {
			}
			
			public NetConfig(final NetConfig other) {
				this.timeoutMilliSec = other.getTimeoutMilliSec();
				this.reuseAddr = other.getReuseAddr();
				this.keepAlive = other.getKeepAlive();
				this.tcpNoDelay = other.getTcpNoDelay();
				this.linger = other.getLinger();
				this.bindBackLogSize = other.getBindBackLogSize();
				this.interestOpQueued = other.getInterestOpQueued();
				this.rcvBuf = new SizeInBytes(other.getRcvBuf());
				this.sndBuf = new SizeInBytes(other.getSndBuf());
				this.ssl = other.getSsl();
				this.httpConfig = new HttpConfig(other.getHttpConfig());
				this.nodeConfig = new NodeConfig(other.getNodeConfig());
			}
		}
		
		public static final class DriverConfig
		implements Serializable {
			
			public static final String KEY_REMOTE = "remote";
			public static final String KEY_ADDRS = "addrs";
			public static final String KEY_PORT = "port";

			public final void setAddrs(final List<String> addrs) {
				this.addrs = addrs;
			}

			public final void setRemote(final boolean remote) {
				this.remote = remote;
			}

			public final void setPort(final int port) {
				this.port = port;
			}

			@JsonProperty(KEY_ADDRS) private List<String> addrs;
			@JsonProperty(KEY_REMOTE) private boolean remote;
			@JsonProperty(KEY_PORT) private int port;

			public DriverConfig() {
			}

			public DriverConfig(final DriverConfig other) {
				this.remote = other.getRemote();
				this.addrs = new ArrayList<>(other.getAddrs());
				this.port = other.getPort();
			}

			public List<String> getAddrs() {
				return addrs;
			}

			public boolean getRemote() {
				return remote;
			}

			public int getPort() {
				return port;
			}
		}

		public static final class MockConfig
		implements Serializable {

			public static final String KEY_CAPACITY = "capacity";
			public static final String KEY_CONTAINER = "container";
			public static final String KEY_FAIL = "fail";
			public static final String KEY_NODE = "node";

			public final void setCapacity(final int capacity) {
				this.capacity = capacity;
			}
			
			public final void setContainerConfig(final ContainerConfig containerConfig) {
				this.containerConfig = containerConfig;
			}

			public final void setFailConfig(final FailConfig failConfig) {
				this.failConfig = failConfig;
			}

			public final void setNode(final boolean node) {
				this.node = node;
			}

			@JsonProperty(KEY_CAPACITY) private int capacity;
			@JsonProperty(KEY_CONTAINER) private ContainerConfig containerConfig;
			@JsonProperty(KEY_FAIL) private FailConfig failConfig;
			@JsonProperty(KEY_NODE) private boolean node;

			public MockConfig() {
			}

			public MockConfig(final MockConfig other) {
				this.capacity = other.getCapacity();
				this.containerConfig = new ContainerConfig(other.getContainerConfig());
				this.failConfig = new FailConfig(other.getFailConfig());
				this.node = other.getNode();
			}

			public int getCapacity() {
				return capacity;
			}

			public ContainerConfig getContainerConfig() {
				return containerConfig;
			}

			public FailConfig getFailConfig() {
				return failConfig;
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

				public ContainerConfig(final ContainerConfig other) {
					this.capacity = other.getCapacity();
					this.countLimit = other.getCountLimit();
				}

				public int getCapacity() {
					return capacity;
				}

				public int getCountLimit() {
					return countLimit;
				}
			}

			public static final class FailConfig
			implements Serializable {

				public static final String KEY_CONNECTIONS = "connections";
				public static final String KEY_RESPONSES = "responses";
				@JsonProperty(KEY_CONNECTIONS) private long connections;
				@JsonProperty(KEY_RESPONSES) private long responses;

				public FailConfig() {
				}

				public FailConfig(final FailConfig other) {
					this.connections = other.getConnections();
					this.responses = other.getResponses();
				}

				public final long getConnections() {
					return connections;
				}

				public final void setConnections(final long connections) {
					this.connections = connections;
				}

				public final long getResponses() {
					return responses;
				}

				public final void setResponses(final long responses) {
					this.responses = responses;
				}
			}

		}

	}
	
	public void apply(final Map<String, Object> tree)
	throws IllegalArgumentException {
		try {
			applyRecursively(this, tree);
		} catch(final IllegalArgumentNameException e) {
			throw new IllegalArgumentNameException(ARG_PREFIX + e.getMessage());
		} catch(final InvocationTargetException | IllegalAccessException e) {
			e.printStackTrace(System.err);
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
			} else {
				applyField(config, key, node);
			}
		}
	}

	private static void applyField(final Object config, final String key, final Object value)
	throws InvocationTargetException, IllegalAccessException {
		final Class configCls = config.getClass();
		try {
			final Method fieldGetter = configCls.getMethod("get" + capitalize(key));
			final Class fieldType = fieldGetter.getReturnType();
			if(value == null) {
				configCls
					.getMethod("set" + capitalize(key), fieldType)
					.invoke(config, value);
			} else {
				final Class valueType = value.getClass();
				if(TypeUtil.typeEquals(fieldType, valueType)) {
					configCls.getMethod("set" + capitalize(key), fieldType).invoke(config, value);
				} else if(value instanceof List && TypeUtil.typeEquals(fieldType, List.class)) {
					configCls.getMethod("set" + capitalize(key), fieldType).invoke(config, value);
				} else if(value instanceof String) { // CLI arguments case
					if(fieldType.equals(List.class)) {
						final List<String> listValue = Arrays.asList(((String) value).split(","));
						configCls
							.getMethod("set" + capitalize(key), List.class)
							.invoke(config, listValue);
					} else if(fieldType.equals(Map.class)) {
						final Map<String, String>
							field = (Map<String, String>) fieldGetter.invoke(config);
						final String keyValuePair[] = ((String) value).split(":", 2);
						if(keyValuePair.length == 1) {
							field.remove(keyValuePair[0]);
						} else if(keyValuePair.length == 2) {
							field.put(keyValuePair[0], keyValuePair[1]);
						}
					} else if(fieldType.equals(Integer.TYPE) || fieldType.equals(Integer.class)) {
						final int intValue = Integer.parseInt((String) value);
						configCls
							.getMethod("set" + capitalize(key), Integer.TYPE)
							.invoke(config, intValue);
					} else if(fieldType.equals(Long.TYPE) || fieldType.equals(Long.class)) {
						try {
							final long longValue = Long.parseLong((String) value);
							configCls
								.getMethod("set" + capitalize(key), Long.TYPE)
								.invoke(config, longValue);
						} catch(final NumberFormatException e) {
							final long timeValue = TimeUtil.getTimeInSeconds((String) value);
							configCls
								.getMethod("set" + capitalize(key), Long.TYPE)
								.invoke(config, timeValue);
						}
					} else if(fieldType.equals(Float.TYPE) || fieldType.equals(Float.class)) {
						final float floatValue = Float.parseFloat((String) value);
						configCls
							.getMethod("set" + capitalize(key), Float.TYPE)
							.invoke(config, floatValue);
					} else if(fieldType.equals(Double.TYPE) || fieldType.equals(Double.class)) {
						final double doubleValue = Double.parseDouble((String) value);
						configCls
							.getMethod("set" + capitalize(key), Double.TYPE)
							.invoke(config, doubleValue);
					} else if(fieldType.equals(Boolean.TYPE) || fieldType.equals(Boolean.class)) {
						final boolean boolValue = Boolean.parseBoolean((String) value);
						configCls
							.getMethod("set" + capitalize(key), Boolean.TYPE)
							.invoke(config, boolValue);
					} else if(fieldType.equals(SizeInBytes.class)) {
						final SizeInBytes sizeValue = new SizeInBytes((String) value);
						configCls
							.getMethod("set" + capitalize(key), SizeInBytes.class)
							.invoke(config, sizeValue);
					} else {
						throw new IllegalStateException(
							"Field type is \"" + fieldType.getName() + "\" for key: " + key
						);
					}
				} else {
					if(Integer.TYPE.equals(valueType) || Integer.class.equals(valueType)) {
						final int intValue = (int) value;
						if(SizeInBytes.class.equals(fieldType)) {
							configCls
								.getMethod("set" + capitalize(key), SizeInBytes.class)
								.invoke(config, new SizeInBytes(intValue));
						} else if(Long.class.equals(fieldType) || Long.TYPE.equals(fieldType)) {
							configCls
								.getMethod("set" + capitalize(key), Long.TYPE)
								.invoke(config, intValue);
						} else if(Double.class.equals(fieldType) || Double.TYPE.equals(fieldType)) {
							configCls
								.getMethod("set" + capitalize(key), Double.TYPE)
								.invoke(config, intValue);
						} else {
							throw new IllegalStateException(
								"Field type is \"" + fieldType.getName() +
								"\" but value type is \"" + valueType.getName() + "\""
							);
						}
					} else {
						throw new IllegalStateException(
							"Field type is \"" + fieldType.getName() +
							"\" but value type is \"" + valueType.getName() + "\""
						);
					}
				}
			}
		} catch(final NoSuchMethodException e) {
			throw new IllegalArgumentNameException(key);
		}
	}
}
