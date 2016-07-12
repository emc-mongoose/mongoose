package com.emc.mongoose.common.config;

import com.emc.mongoose.common.config.decoder.DecodeException;
import com.emc.mongoose.common.config.reader.ConfigReader;
import com.emc.mongoose.common.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 Created on 11.07.16.
 */
public class CommonConfig {

	private final static Logger LOG = LogManager.getLogger();
	private static final CommonDecoder DECODER = new CommonDecoder();
	private static CommonConfig CONFIG;
	static {
		try {
			CONFIG = DECODER.decode(ConfigReader.readJson("defaults.json"));
		} catch(final DecodeException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to load the configuration");
		}
	}

	public static CommonConfig getConfig() {
		return CONFIG;
	}

	public static final String KEY_NAME = "name";
	public static final String KEY_NETWORK = "network";
	public static final String KEY_STORAGE = "storage";
	public static final String KEY_ITEM = "item";

	private final String name;
	private final NetworkConfig networkConfig;
	private final StorageConfig storageConfig;
	private final ItemConfig itemConfig;


	public CommonConfig(final String name, final NetworkConfig networkConfig,
		final StorageConfig storageConfig, final ItemConfig itemConfig) {
		this.name = name;
		this.networkConfig = networkConfig;
		this.storageConfig = storageConfig;
		this.itemConfig = itemConfig;
	}

	public String getName() {
		return name;
	}

	public NetworkConfig getNetworkConfig() {
		return networkConfig;
	}

	public StorageConfig getStorageConfig() {
		return storageConfig;
	}

	public ItemConfig getItemConfig() {
		return itemConfig;
	}


	public static class NetworkConfig {

		public static final String KEY_SOCKET = "socket";
		private final SocketConfig socketConfig;

		public NetworkConfig(final SocketConfig socketConfig) {
			this.socketConfig = socketConfig;
		}

		public SocketConfig getSocketConfig() {
			return socketConfig;
		}

		public static class SocketConfig {
			public static final String KEY_TIMEOUT_IN_MILLISECONDS = "timeoutMilliSec";
			public static final String KEY_REUSABLE_ADDRESS = "reuseAddr";
			public static final String KEY_KEEP_ALIVE = "keepAlive";
			public static final String KEY_TCP_NO_DELAY = "tcpNoDelay";
			public static final String KEY_LINGER = "linger";
			public static final String KEY_BIND_BACK_LOG_SIZE = "bindBacklogSize";
			public static final String KEY_INTEREST_OP_QUEUED = "interestOpQueued";
			public static final String KEY_SELECT_INTERVAL = "selectInterval";
			private final int timeoutMilliSec;
			private final boolean reuseAddr;
			private final boolean keepAlive;
			private final boolean tcpNoDelay;
			private final int linger;
			private final int bindBackLogSize;
			private final boolean interestOpQueued;
			private final int selectInterval;

			public SocketConfig(
				final int timeoutMilliSec, final boolean reuseAddr, final boolean keepAlive,
				final boolean tcpNoDelay, final int linger, final int bindBackLogSize,
				final boolean interestOpQueued, final int selectInterval
			) {
				this.timeoutMilliSec = timeoutMilliSec;
				this.reuseAddr = reuseAddr;
				this.keepAlive = keepAlive;
				this.tcpNoDelay = tcpNoDelay;
				this.linger = linger;
				this.bindBackLogSize = bindBackLogSize;
				this.interestOpQueued = interestOpQueued;
				this.selectInterval = selectInterval;
			}

			public int getTimeoutInMilliseconds() {
				return timeoutMilliSec;
			}

			public boolean getReusableAddress() {
				return reuseAddr;
			}

			public boolean getKeepAlive() {
				return keepAlive;
			}

			public boolean getTcpNoDelay() {
				return tcpNoDelay;
			}

			public int getLinger() {
				return linger;
			}

			public int getBindBackLogSize() {
				return bindBackLogSize;
			}

			public boolean getInterestOpQueued() {
				return interestOpQueued;
			}

			public int getSelectInterval() {
				return selectInterval;
			}
		}
	}

	public static class StorageConfig {

		public static final String KEY_ADDRESSES = "addrs";
		public static final String KEY_AUTH = "auth";
		public static final String KEY_HTTP = "http";
		public static final String KEY_PORT = "port";
		public static final String KEY_TYPE = "type";

		private final int port;
		private final String type;
		private final AuthConfig authConfig;
		private final HttpConfig httpConfig;
		private final List<String> addrs;

		public StorageConfig(
			final int port, final String type, final AuthConfig authConfig,
			final HttpConfig httpConfig, final List<String> addrs
		) {
			this.port = port;
			this.type = type;
			this.authConfig = authConfig;
			this.httpConfig = httpConfig;
			this.addrs = Collections.unmodifiableList(addrs);
		}

		public int getPort() {
			return port;
		}

		public String getType() {
			return type;
		}

		public AuthConfig getAuthConfig() {
			return authConfig;
		}

		public HttpConfig getHttpConfig() {
			return httpConfig;
		}

		public List<String> getAddresses() {
			return addrs;
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
			public static final String KEY_HEADER_USER_AGENT= "User-Agent";
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
	}

	public static class ItemConfig {

		public static final String KEY_TYPE = "type";
		public static final String KEY_DATA = "data";
		public static final String KEY_DESTINATION = "dst";
		public static final String KEY_SOURCE = "src";
		public static final String KEY_NAMING = "naming";
		private final String type;
		private final DataConfig dataConfig;
		private final DestinationConfig dst;
		private final SourceConfig src;
		private final NamingConfig namingConfig;

		public ItemConfig(
			final String type, final DataConfig dataConfig, final DestinationConfig dstConfig,
			final SourceConfig srcConfig, final NamingConfig namingConfig
		) {
			this.type = type;
			this.dataConfig = dataConfig;
			this.dst = dstConfig;
			this.src = srcConfig;
			this.namingConfig = namingConfig;
		}

		public String getType() {
			return type;
		}

		public DataConfig getDataConfig() {
			return dataConfig;
		}

		public DestinationConfig getDestinationConfig() {
			return dst;
		}

		public SourceConfig getSourceConfig() {
			return src;
		}

		public NamingConfig getNamingConfig() {
			return namingConfig;
		}

		public static class DataConfig {

			public static final String KEY_CONTENT = "content";
			public static final String KEY_RANGES = "ranges";
			public static final String KEY_SIZE = "size";
			public static final String KEY_VERIFY = "verify";
			private final ContentConfig contentConfig;
			private final int ranges;
			private final String size;
			private final boolean verify;

			public DataConfig(
				final ContentConfig contentConfig, final int ranges, final String size, final boolean verify
			) {
				this.contentConfig = contentConfig;
				this.ranges = ranges;
				this.size = size;
				this.verify = verify;
			}

			public ContentConfig getContentConfig() {
				return contentConfig;
			}

			public int getRanges() {
				return ranges;
			}

			public String getSize() {
				return size;
			}

			public boolean getVerify() {
				return verify;
			}

			public static class ContentConfig {

				public static final String KEY_FILE = "file";
				public static final String KEY_SEED = "seed";
				public static final String KEY_RING_SIZE = "ringSize";
				private final String file;
				private final String seed;
				private final String ringSize;

				public ContentConfig(final String file, final String seed, final String ringSize) {
					this.file = file;
					this.seed = seed;
					this.ringSize = ringSize;
				}

				public String getFile() {
					return file;
				}

				public String getSeed() {
					return seed;
				}

				public String getRingSize() {
					return ringSize;
				}
			}
		}

		public static class DestinationConfig {

			public static final String KEY_CONTAINER = "container";
			public static final String KEY_FILE = "file";
			private final String container;
			private final String file;

			public DestinationConfig(final String container, final String file) {
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

		public static class SourceConfig {

			public static final String KEY_CONTAINER = "container";
			public static final String KEY_FILE = "file";
			public static final String KEY_BATCH_SIZE = "batchSize";
			private final String container;
			private final String file;
			private final int batchSize;

			public SourceConfig(final String file, final String container, final int batchSize) {
				this.file = file;
				this.container = container;
				this.batchSize = batchSize;
			}

			public String getContainer() {
				return container;
			}

			public String getFile() {
				return file;
			}

			public int getBatchSize() {
				return batchSize;
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

			public String getType() {
				return type;
			}

			public String getPrefix() {
				return prefix;
			}

			public int getRadix() {
				return radix;
			}

			public int getOffset() {
				return offset;
			}

			public int getLength() {
				return length;
			}
		}
	}

}
