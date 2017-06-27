package com.emc.mongoose.ui.config;

import com.emc.mongoose.common.reflection.TypeUtil;
import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.api.TimeUtil;
import static com.emc.mongoose.ui.cli.CliArgParser.ARG_PREFIX;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 Created on 11.07.16.
 */
public final class Config
implements Serializable {

	public static final String NAME = "name";
	public static final String DEPRECATED = "deprecated";
	public static final String TARGET = "target";
	public static final String PATH_SEP = "-";

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
	public static final String KEY_OUTPUT = "output";
	public static final String KEY_STORAGE = "storage";
	public static final String KEY_TEST = "test";
	public static final String KEY_ALIASING = "aliasing";
	
	@JsonProperty(KEY_ITEM) private ItemConfig itemConfig;
	@JsonProperty(KEY_LOAD) private LoadConfig loadConfig;
	@JsonProperty(KEY_OUTPUT) private OutputConfig outputConfig;
	@JsonProperty(KEY_STORAGE) private StorageConfig storageConfig;
	@JsonProperty(KEY_TEST) private TestConfig testConfig;
	@JsonProperty(KEY_VERSION) private String version;
	@JsonProperty(KEY_ALIASING) private List<Map<String, Object>> aliasingConfig;

	public Config() {}

	public Config(final Config config) {
		this.version = config.getVersion();
		this.itemConfig = new ItemConfig(config.getItemConfig());
		this.loadConfig = new LoadConfig(config.getLoadConfig());
		this.outputConfig = new OutputConfig(config.getOutputConfig());
		this.storageConfig = new StorageConfig(config.getStorageConfig());
		this.testConfig = new TestConfig(config.getTestConfig());
		final List<Map<String, Object>> ac = config.getAliasingConfig();
		this.aliasingConfig = ac == null ? null : new ArrayList<>(config.getAliasingConfig());
	}

	public final String getVersion() {
		return version;
	}

	public final OutputConfig getOutputConfig() {
		return outputConfig;
	}

	public final StorageConfig getStorageConfig() {
		return storageConfig;
	}

	public final TestConfig getTestConfig() {
		return testConfig;
	}

	public final LoadConfig getLoadConfig() {
		return loadConfig;
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

	public final void setOutputConfig(final OutputConfig outputConfig) {
		this.outputConfig = outputConfig;
	}
	
	public final void setStorageConfig(final StorageConfig storageConfig) {
		this.storageConfig = storageConfig;
	}

	public final void setTestConfig(final TestConfig testConfig) {
		this.testConfig = testConfig;
	}
	
	public final void setLoadConfig(final LoadConfig loadConfig) {
		this.loadConfig = loadConfig;
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
				public static final String KEY_RING = "ring";
				
				public final void setFile(final String file) {
					this.file = file;
				}
				
				public final void setSeed(final String seed) {
					this.seed = seed;
				}
				
				public final void setRingConfig(final RingConfig ringConfig) {
					this.ringConfig = ringConfig;
				}
				
				@JsonProperty(KEY_FILE) private String file;

				@JsonProperty(KEY_SEED) private String seed;

				@JsonProperty(KEY_RING) private RingConfig ringConfig;
				
				public static final class RingConfig
				implements Serializable {
					
					public static final String KEY_CACHE = "cache";
					public static final String KEY_SIZE = "size";
					
					@JsonProperty(KEY_CACHE) private int cache;
					
					@JsonProperty(KEY_SIZE)
					@JsonDeserialize(using = SizeInBytesDeserializer.class)
					@JsonSerialize(using = SizeInBytesSerializer.class)
					private SizeInBytes size;
					
					public final void setCache(final int cache) {
						this.cache = cache;
					}
					
					public final void setSize(final SizeInBytes size) {
						this.size = size;
					}
					
					public RingConfig() {
					}
					
					public RingConfig(final RingConfig other) {
						this.cache = other.getCache();
						this.size = other.getSize();
					}
					
					public final int getCache() {
						return cache;
					}
					
					public final SizeInBytes getSize() {
						return size;
					}
				}

				public ContentConfig() {
				}

				public ContentConfig(final ContentConfig other) {
					this.file = other.getFile();
					this.seed = other.getSeed();
					this.ringConfig = other.getRingConfig();
				}

				public final String getFile() {
					return file;
				}

				public final String getSeed() {
					return seed;
				}

				public final RingConfig getRingConfig() {
					return ringConfig;
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

		public static final String KEY_BATCH = "batch";
		public static final String KEY_CIRCULAR = "circular";
		public static final String KEY_GENERATOR = "generator";
		public static final String KEY_QUEUE = "queue";
		public static final String KEY_TYPE = "type";
		
		public final void setBatchConfig(final BatchConfig batchConfig) {
			this.batchConfig = batchConfig;
		}
		
		public final void setCircular(final boolean circular) {
			this.circular = circular;
		}
		
		public final void setGeneratorConfig(final GeneratorConfig generatorConfig) {
			this.generatorConfig = generatorConfig;
		}

		public final void setQueueConfig(final QueueConfig queueConfig) {
			this.queueConfig = queueConfig;
		}
		
		public final void setType(final String type) {
			this.type = type;
		}
		
		@JsonProperty(KEY_BATCH) private BatchConfig batchConfig;
		@JsonProperty(KEY_CIRCULAR) private boolean circular;
		@JsonProperty(KEY_GENERATOR) private GeneratorConfig generatorConfig;
		@JsonProperty(KEY_QUEUE) private QueueConfig queueConfig;
		@JsonProperty(KEY_TYPE) private String type;
		
		public LoadConfig() {
		}

		public LoadConfig(final LoadConfig other) {
			this.batchConfig = new BatchConfig(other.batchConfig);
			this.circular = other.circular;
			this.generatorConfig = new GeneratorConfig(other.generatorConfig);
			this.queueConfig = new QueueConfig(other.queueConfig);
			this.type = other.type;
		}
		
		public static final class BatchConfig
		implements Serializable {
			
			public static final String KEY_SIZE = "size";
			
			@JsonProperty(KEY_SIZE) private int size;
			
			public BatchConfig() {
			}
			
			public BatchConfig(final BatchConfig other) {
				this.size = other.size;
			}
			
			public final void setSize(final int size) {
				this.size = size;
			}
			
			public final int getSize() {
				return size;
			}
		}
		
		public final BatchConfig getBatchConfig() {
			return batchConfig;
		}
		
		public final String getType() {
			return type;
		}

		public final boolean getCircular() {
			return circular;
		}

		public final GeneratorConfig getGeneratorConfig() {
			return generatorConfig;
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

		public static final class QueueConfig
		implements Serializable {

			public static final String KEY_SIZE = "size";

			public final void setSize(final int size) {
				this.size = size;
			}
			
			@JsonProperty(KEY_SIZE) private int size;
			
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

	public static final class OutputConfig
	implements Serializable {

		public static final String KEY_COLOR = "color";
		public static final String KEY_METRICS = "metrics";
		public static final String KEY_PERSIST = "persist";

		public final void setColor(final boolean colorFlag) {
			this.colorFlag = colorFlag;
		}

		public final void setMetricsConfig(final MetricsConfig metricsConfig) {
			this.metricsConfig = metricsConfig;
		}

		public final void setPersist(final boolean persistFlag) {
			this.persistFlag = persistFlag;
		}

		@JsonProperty(KEY_COLOR) private boolean colorFlag;
		@JsonProperty(KEY_METRICS) private MetricsConfig metricsConfig;
		@JsonProperty(KEY_PERSIST) private boolean persistFlag;

		public OutputConfig() {
		}

		public OutputConfig(final OutputConfig other) {
			this.colorFlag = other.getColor();
			this.metricsConfig = new MetricsConfig(other.getMetricsConfig());
			this.persistFlag = other.getPersist();
		}

		public final boolean getColor() {
			return colorFlag;
		}

		public final MetricsConfig getMetricsConfig() {
			return metricsConfig;
		}

		public final boolean getPersist() {
			return persistFlag;
		}

		public static final class MetricsConfig
		implements Serializable {

			public static final String KEY_AVERAGE = "average";
			public static final String KEY_SUMMARY = "summary";
			public static final String KEY_TRACE = "trace";
			public static final String KEY_SERVICE = "service";
			public static final String KEY_THRESHOLD = "threshold";

			public final void setAverageConfig(final AverageConfig averageConfig) {
				this.averageConfig = averageConfig;
			}

			public final void setSummaryConfig(final SummaryConfig summaryConfig) {
				this.summaryConfig = summaryConfig;
			}

			public final void setTraceConfig(final TraceConfig traceConfig) {
				this.traceConfig = traceConfig;
			}

			public final void setService(final boolean serviceFlag) {
				this.serviceFlag = serviceFlag;
			}

			public final void setThreshold(final double threshold) {
				this.threshold = threshold;
			}

			@JsonProperty(KEY_AVERAGE) private AverageConfig averageConfig;
			@JsonProperty(KEY_SUMMARY) private SummaryConfig summaryConfig;
			@JsonProperty(KEY_TRACE) private TraceConfig traceConfig;
			@JsonProperty(KEY_SERVICE) private boolean serviceFlag;
			@JsonProperty(KEY_THRESHOLD) private double threshold;

			public MetricsConfig() {
			}

			public MetricsConfig(final MetricsConfig other) {
				this.averageConfig = new AverageConfig(other.getAverageConfig());
				this.summaryConfig = new SummaryConfig(other.getSummaryConfig());
				this.traceConfig = new TraceConfig(other.getTraceConfig());
				this.serviceFlag = other.getService();
				this.threshold = other.getThreshold();
			}

			public final AverageConfig getAverageConfig() {
				return averageConfig;
			}

			public final SummaryConfig getSummaryConfig() {
				return summaryConfig;
			}

			public final TraceConfig getTraceConfig() {
				return traceConfig;
			}

			public final boolean getService() {
				return serviceFlag;
			}

			public final double getThreshold() {
				return threshold;
			}

			public static final class AverageConfig
			implements Serializable {

				public static final String KEY_PERIOD = "period";
				public static final String KEY_PERSIST = "persist";
				public static final String KEY_TABLE = "table";

				public final void setPeriod(final long period) {
					this.period = period;
				}

				public final void setPersist(final boolean persistFlag) {
					this.persistFlag = persistFlag;
				}

				public final void setTableConfig(final TableConfig tableConfig) {
					this.tableConfig = tableConfig;
				}

				@JsonDeserialize(using = TimeStrToLongDeserializer.class) @JsonProperty(KEY_PERIOD)
				private long period;
				@JsonProperty(KEY_PERSIST) private boolean persistFlag;
				@JsonProperty(KEY_TABLE) private TableConfig tableConfig;

				public AverageConfig() {
				}

				public AverageConfig(final AverageConfig other) {
					this.period = other.getPeriod();
					this.persistFlag = other.getPersist();
					this.tableConfig = new TableConfig(other.getTableConfig());
				}

				public final long getPeriod() {
					return period;
				}

				public final boolean getPersist() {
					return persistFlag;
				}

				public final TableConfig getTableConfig() {
					return tableConfig;
				}

				public static final class TableConfig
					implements Serializable {

					public static final String KEY_HEADER = "header";

					public final void setHeaderConfig(final HeaderConfig headerConfig) {
						this.headerConfig = headerConfig;
					}

					@JsonProperty(KEY_HEADER) private HeaderConfig headerConfig;

					public TableConfig() {
					}

					public TableConfig(final TableConfig other) {
						this.headerConfig = new HeaderConfig(other.getHeaderConfig());
					}

					public final HeaderConfig getHeaderConfig() {
						return headerConfig;
					}

					public static final class HeaderConfig
						implements Serializable {

						public static final String KEY_PERIOD = "period";

						public final void setPeriod(final int period) {
							this.period = period;
						}

						@JsonProperty(KEY_PERIOD) private int period;

						public HeaderConfig() {
						}

						public HeaderConfig(final HeaderConfig other) {
							this.period = other.getPeriod();
						}

						public final int getPeriod() {
							return period;
						}
					}
				}
			}

			public static final class SummaryConfig
			implements Serializable {

				public static final String KEY_PERSIST = "persist";

				public final void setPersist(final boolean persistFlag) {
					this.persistFlag = persistFlag;
				}

				@JsonProperty(KEY_PERSIST) private boolean persistFlag;

				public SummaryConfig() {
				}

				public SummaryConfig(final SummaryConfig other) {
					this.persistFlag = other.getPersist();
				}

				public final boolean getPersist() {
					return persistFlag;
				}
			}

			public static final class TraceConfig
			implements Serializable {

				public static final String KEY_PERSIST = "persist";

				public final void setPersist(final boolean persistFlag) {
					this.persistFlag = persistFlag;
				}

				@JsonProperty(KEY_PERSIST) private boolean persistFlag;

				public TraceConfig() {
				}

				public TraceConfig(final TraceConfig other) {
					this.persistFlag = other.getPersist();
				}

				public final boolean getPersist() {
					return persistFlag;
				}
			}
		}
	}

	public static final class StorageConfig
	implements Serializable {

		public static final String KEY_AUTH = "auth";
		public static final String KEY_NET = "net";
		public static final String KEY_DRIVER = "driver";
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
		
		public final void setMockConfig(final MockConfig mockConfig) {
			this.mockConfig = mockConfig;
		}

		@JsonProperty(KEY_AUTH) private AuthConfig authConfig;
		@JsonProperty(KEY_NET) private NetConfig netConfig;
		@JsonProperty(KEY_DRIVER) private DriverConfig driverConfig;
		@JsonProperty(KEY_MOCK) private MockConfig mockConfig;

		public StorageConfig() {
		}

		public StorageConfig(final StorageConfig other) {
			this.authConfig = new AuthConfig(other.getAuthConfig());
			this.netConfig = new NetConfig(other.getNetConfig());
			this.driverConfig = new DriverConfig(other.getDriverConfig());
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
		
		public MockConfig getMockConfig() {
			return mockConfig;
		}

		public static final class AuthConfig
		implements Serializable {

			public static final String KEY_FILE = "file";
			public static final String KEY_SECRET = "secret";
			public static final String KEY_TOKEN = "token";
			public static final String KEY_UID = "uid";
			
			public final void setFile(final String file) {
				this.file = file;
			}
			
			public final void setSecret(final String secret) {
				this.secret = secret;
			}
			
			public final void setToken(final String token) {
				this.token = token;
			}
			
			public final void setUid(final String uid) {
				this.uid = uid;
			}
			
			@JsonProperty(KEY_FILE) private String file;
			@JsonProperty(KEY_SECRET) private String secret;
			@JsonProperty(KEY_TOKEN) private String token;
			@JsonProperty(KEY_UID) private String uid;
			
			public AuthConfig() {
			}

			public AuthConfig(final AuthConfig other) {
				this.file = other.getFile();
				this.secret = other.getSecret();
				this.token = other.getToken();
				this.uid = other.getUid();
			}
			
			public final String getFile() {
				return file;
			}

			public final String getSecret() {
				return secret;
			}

			public final String getToken() {
				return token;
			}
			
			public final String getUid() {
				return uid;
			}
		}
		
		public static final class NetConfig
		implements Serializable {
			
			public static final class HttpConfig
			implements Serializable {
				
				public static final String KEY_FS_ACCESS = "fsAccess";
				public static final String KEY_HEADERS = "headers";
				public static final String KEY_HEADER_CONNECTION = "Connection";
				public static final String KEY_HEADER_USER_AGENT = "User-Agent";
				public static final String KEY_NAMESPACE = "namespace";
				public static final String KEY_VERSIONING = "versioning";
				
				public final void setFsAccess(final boolean fsAccess) {
					this.fsAccess = fsAccess;
				}
				
				public final void setNamespace(final String namespace) {
					this.namespace = namespace;
				}
				
				public final void setVersioning(final boolean versioning) {
					this.versioning = versioning;
				}
				
				public final void setHeadersConfig(final Map<String, String> headers) {
					this.headersConfig = headers;
				}
				
				@JsonProperty(KEY_FS_ACCESS) private boolean fsAccess;
				@JsonProperty(KEY_NAMESPACE) private String namespace;
				@JsonProperty(KEY_VERSIONING) private boolean versioning;
				@JsonProperty(KEY_HEADERS) private Map<String, String> headersConfig;
				
				public HttpConfig() {
				}
				
				public HttpConfig(final HttpConfig other) {
					this.fsAccess = other.getFsAccess();
					this.namespace = other.getNamespace();
					this.versioning = other.getVersioning();
					this.headersConfig = new HashMap<>(other.getHeadersConfig());
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
				
				public Map<String, String> getHeadersConfig() {
					return headersConfig;
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
			
			public static final String KEY_TIMEOUT_MILLI_SEC = "timeoutMilliSec";
			public static final String KEY_REUSE_ADDR = "reuseAddr";
			public static final String KEY_KEEP_ALIVE = "keepAlive";
			public static final String KEY_TCP_NO_DELAY = "tcpNoDelay";
			public static final String KEY_LINGER = "linger";
			public static final String KEY_BIND_BACKLOG_SIZE = "bindBacklogSize";
			public static final String KEY_INTEREST_OP_QUEUED = "interestOpQueued";
			public static final String KEY_RCV_BUF = "rcvBuf";
			public static final String KEY_SND_BUF = "sndBuf";
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
			
			public final int getBindBacklogSize() {
				return bindBacklogSize;
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
			
			public final void setBindBacklogSize(final int bindBacklogSize) {
				this.bindBacklogSize = bindBacklogSize;
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
			
			@JsonProperty(KEY_TIMEOUT_MILLI_SEC) private int timeoutMilliSec;
			
			@JsonProperty(KEY_REUSE_ADDR) private boolean reuseAddr;
			
			@JsonProperty(KEY_KEEP_ALIVE) private boolean keepAlive;
			
			@JsonProperty(KEY_TCP_NO_DELAY) private boolean tcpNoDelay;

			@JsonProperty(KEY_LINGER) private int linger;
			
			@JsonProperty(KEY_BIND_BACKLOG_SIZE) private int bindBacklogSize;
			
			@JsonProperty(KEY_INTEREST_OP_QUEUED) private boolean interestOpQueued;
			
			@JsonProperty(KEY_RCV_BUF)
			@JsonDeserialize(using = SizeInBytesDeserializer.class)
			@JsonSerialize(using = SizeInBytesSerializer.class)
			private SizeInBytes rcvBuf;
			
			@JsonProperty(KEY_SND_BUF)
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
				this.bindBacklogSize = other.getBindBacklogSize();
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
			
			public static final class IoConfig
			implements Serializable {
				
				public static final String KEY_WORKERS = "workers";
				
				public final void setWorkers(final int workers) {
					this.workers = workers;
				}
				
				@JsonProperty(KEY_WORKERS) private int workers;
				
				public final int getWorkers() {
					return workers;
				}
				
				public IoConfig() {
				}
				
				public IoConfig(final IoConfig other) {
					this.workers = other.getWorkers();
				}
			}
			
			public static final String KEY_ADDRS = "addrs";
			public static final String KEY_CONCURRENCY = "concurrency";
			public static final String KEY_PORT = "port";
			public static final String KEY_REMOTE = "remote";
			public static final String KEY_TYPE = "type";
			public static final String KEY_IMPL = "impl";
			public static final String KEY_IO = "io";

			public static final String KEY_IMPL_TYPE = "type";
			public static final String KEY_IMPL_FILE = "file";
			public static final String KEY_IMPL_FQCN = "fqcn";

			public final void setAddrs(final List<String> addrs) {
				this.addrs = addrs;
			}

			public final void setConcurrency(final int concurrency) {
				this.concurrency = concurrency;
			}

			public final void setPort(final int port) {
				this.port = port;
			}

			public final void setRemote(final boolean remote) {
				this.remote = remote;
			}

			public final void setType(final String type) {
				this.type = type;
			}

			public final void setImplConfig(final List<Map<String, Object>> implConfig) {
				this.implConfig = implConfig;
			}
			
			public final void setIoConfig(final IoConfig ioConfig) {
				this.ioConfig = ioConfig;
			}

			@JsonProperty(KEY_ADDRS) private List<String> addrs;
			@JsonProperty(KEY_CONCURRENCY) private int concurrency;
			@JsonProperty(KEY_PORT) private int port;
			@JsonProperty(KEY_REMOTE) private boolean remote;
			@JsonProperty(KEY_TYPE) private String type;
			@JsonProperty(KEY_IMPL) private List<Map<String, Object>> implConfig;
			@JsonProperty(KEY_IO) private IoConfig ioConfig;

			public DriverConfig() {
			}

			public DriverConfig(final DriverConfig other) {
				this.addrs = new ArrayList<>(other.getAddrs());
				this.concurrency = other.getConcurrency();
				this.port = other.getPort();
				this.remote = other.getRemote();
				this.type = other.getType();
				this.implConfig = other == null ? null : new ArrayList<>(other.getImplConfig());
				this.ioConfig = other.getIoConfig();
			}

			public List<String> getAddrs() {
				return addrs;
			}

			public final int getConcurrency() {
				return concurrency;
			}

			public int getPort() {
				return port;
			}

			public boolean getRemote() {
				return remote;
			}

			public String getType() {
				return type;
			}

			public List<Map<String, Object>> getImplConfig() {
				return implConfig;
			}
			
			public IoConfig getIoConfig() {
				return ioConfig;
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

	public static final class TestConfig
	implements Serializable {

		public static final String KEY_ID = "id";
		public static final String KEY_SCENARIO = "scenario";
		public static final String KEY_STEP = "step";

		@JsonProperty(KEY_ID)
		private String id;
		@JsonProperty(KEY_SCENARIO)
		private ScenarioConfig scenarioConfig;
		@JsonProperty(KEY_STEP)
		private StepConfig stepConfig;

		public final String getId() {
			return id;
		}

		public final ScenarioConfig getScenarioConfig() {
			return this.scenarioConfig;
		}

		public final StepConfig getStepConfig() {
			return this.stepConfig;
		}

		public final void setId(final String id) {
			this.id = id;
		}

		public final void setScenarioConfig(final ScenarioConfig scenarioConfig) {
			this.scenarioConfig = scenarioConfig;
		}

		public final void setStepConfig(final StepConfig stepConfig) {
			this.stepConfig = stepConfig;
		}

		public TestConfig() {
		}

		public TestConfig(final TestConfig other) {
			this.id = other.getId();
			this.scenarioConfig = new ScenarioConfig(other.getScenarioConfig());
			this.stepConfig = new StepConfig(other.getStepConfig());
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

		public static final class StepConfig
		implements Serializable {

			public static final String KEY_LIMIT = "limit";
			public static final String KEY_ID = "id";

			@JsonProperty(KEY_LIMIT)
			private LimitConfig limitConfig;

			@JsonProperty(KEY_ID)
			private String id;

			public StepConfig() {
			}

			public StepConfig(final StepConfig other) {
				this.limitConfig = new LimitConfig(other.getLimitConfig());
				this.id = other.getId();}

			public final LimitConfig getLimitConfig() {
				return limitConfig;
			}

			public final String getId() {
				return id;
			}

			public final void setLimitConfig(final LimitConfig limitConfig) {
				this.limitConfig = limitConfig;
			}

			public final void setId(final String id) {
				this.id = id;
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
		}
	}

	public void apply(final Map<String, Object> tree)
	throws IllegalArgumentException {
		applyAliasing(tree, getAliasingConfig());
		try {
			applyRecursively(this, tree);
		} catch(final IllegalArgumentNameException e) {
			throw new IllegalArgumentNameException(ARG_PREFIX + e.getMessage());
		} catch(final InvocationTargetException | IllegalAccessException e) {
			e.printStackTrace(System.err);
		}
	}

	private static void applyAliasing(
		final Map<String, Object> tree, final List<Map<String, Object>> rawAliases
	) {
		String aliasName, aliasTarget, aliasNamePath[], aliasNamePart;
		Map<String, Object> subTree;
		Object t;

		for(final Map<String, Object> nextAliasNode : rawAliases) {

			aliasName = (String) nextAliasNode.get(NAME);
			aliasTarget = (String) nextAliasNode.get(TARGET);
			if(aliasName.equals(aliasTarget)) {
				throw new IllegalAliasNameException(aliasName);
			}
			aliasNamePath = aliasName.split(PATH_SEP);
			subTree = tree;

			for(int i = 0; i < aliasNamePath.length; i ++) {

				aliasNamePart = aliasNamePath[i];
				t = subTree.get(aliasNamePart);

				if(t != null) {
					if(t instanceof Map) {
						subTree = (Map<String, Object>) t;
					} else if(i == aliasNamePath.length - 1) {
						if(aliasTarget == null) {
							System.err.println(
								"ERROR: configuration value @ \"" + aliasName + "\" is deprecated"
							);
						} else if(nextAliasNode.containsKey(DEPRECATED)) {
							if((boolean) nextAliasNode.get(DEPRECATED)) {
								System.err.println(
									"WARNING: configuration value @ \"" + aliasName +
										"\" is deprecated, please use \"" + aliasTarget +
										"\" instead"
								);
							}
						}
						setNewPath(tree, aliasTarget, t);
						subTree.remove(aliasNamePart);
					} else {
						throw new IllegalAliasNameException(aliasName);
					}
				} else {
					break;
				}
			}
		}

		cleanEmptyPaths(tree);
	}

	private static void setNewPath(
		final Map<String, Object> tree, final String rawPath, final Object value
	) {
		final String newPath[] = rawPath.split(PATH_SEP);
		Map<String, Object> subTree = tree;
		Object t;
		String newPathPart;

		for(int i = 0; i < newPath.length; i ++) {
			newPathPart = newPath[i];
			t = subTree.get(newPathPart);

			if(t != null) {
				if(t instanceof Map) {
					subTree = (Map<String, Object>) t;
					if(i == newPath.length - 1) {
						subTree.put(newPathPart, value);
					}
				} else {
					throw new IllegalAliasTargetException(rawPath);
				}
			} else {
				if(i == newPath.length - 1) {
					subTree.put(newPathPart, value);
				} else {
					t = new HashMap<String, Object>();
					subTree.put(newPathPart, t);
					subTree = (Map<String, Object>) t;
				}
			}
		}
	}

	private static void cleanEmptyPaths(final Map<String, Object> tree) {

		boolean emptyBranchFound = true; // assume
		Object t;
		Iterator<Map.Entry<String, Object>> i;
		Map.Entry<String, Object> nextEntry;

		while(emptyBranchFound && !tree.isEmpty()) {
			i = tree.entrySet().iterator();
			while(i.hasNext()) {
				nextEntry = i.next();
				emptyBranchFound = false;
				t = nextEntry.getValue();
				if(t instanceof Map) {
					if(((Map) t).isEmpty()) {
						i.remove();
						emptyBranchFound = true;
					} else {
						cleanEmptyPaths((Map<String, Object>) t);
					}
				}
			}
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
						throw new IllegalArgumentNameException(key + PATH_SEP + e.getMessage());
					}
				} catch(final NoSuchMethodException e) {
					throw new IllegalArgumentNameException(key);
				}
			} else if(config instanceof Map) {
				((Map<String, Object>) config).put(key, node);
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

	/**
	 @return The JSON pretty-printed representation of this configuration.
	 */
	@Override
	public final String toString() {
		final ObjectMapper mapper = new ObjectMapper()
			.configure(SerializationFeature.INDENT_OUTPUT, true);
		final DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter(
			"\t", DefaultIndenter.SYS_LF
		);
		final DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
		printer.withObjectIndenter(indenter);
		printer.withArrayIndenter(indenter);
		try {
			return mapper.writer(printer).writeValueAsString(this);
		} catch(final JsonProcessingException e) {
			throw new AssertionError(e);
		}
	}
}
