package com.emc.mongoose.config;

import java.util.Map;

/**
 Created on 11.07.16.
 */
public class GeneratorConfig {
	public static final String KEY_ITEM = "item";

	private final ItemConfig itemConfig;

	public GeneratorConfig(final ItemConfig itemConfig) {
		this.itemConfig = itemConfig;
	}

	public ItemConfig getItemConfig() {
		return itemConfig;
	}

	public static class ItemConfig {

		public static final String KEY_TYPE = "type";
		public static final String KEY_DATA = "data";
		public static final String KEY_DESTINATION = "dst";
		public static final String KEY_SOURCE = "src";
		public static final String KEY_NAMING = "naming";

		private final String type;
		private final DataConfig dataConfig;
		private final DstConfig dstConfig;
		private final SrcConfig srcConfig;
		private final NamingConfig namingConfig;

		public ItemConfig(
			final String type, final DataConfig dataConfig, final DstConfig dstConfig,
			final SrcConfig srcConfig, final NamingConfig namingConfig
		) {
			this.type = type;
			this.dataConfig = dataConfig;
			this.dstConfig = dstConfig;
			this.srcConfig = srcConfig;
			this.namingConfig = namingConfig;
		}

		public String getType() {
			return type;
		}

		public DataConfig getDataConfig() {
			return dataConfig;
		}

		public DstConfig getDstConfig() {
			return dstConfig;
		}

		public SrcConfig getSrcConfig() {
			return srcConfig;
		}

		public NamingConfig getNamingConfig() {
			return namingConfig;
		}

		public static class DataConfig {

			public static final String KEY_RANGES = "ranges";
			public static final String KEY_SIZE = "size";
			public static final String KEY_VERIFY = "verify";

			private ContentConfig contentConfig;
			private int ranges;
			private String size;
			private boolean verify;

			public DataConfig(final int ranges, final String size, final boolean verify) {
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

				private String file;
				private String seed;
				private String ringSize;

				public ContentConfig(final String file, final String seed, final String ringSize) {
					this.file = file;
					this.seed = seed;
					this.ringSize = ringSize;
				}
			}
		}

		public static class SrcConfig {

			public static final String KEY_ID = "id";
			public static final String KEY_SECRET = "secret";
			public static final String KEY_TOKEN = "token";

			private String id;
			private String secret;
			private String token;

			public SrcConfig(final String id, final String secret, final String token) {
				this.id = id;
				this.secret = secret;
				this.token = token;
			}

			public SrcConfig() {}

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
		public static class DstConfig {

			public static final String KEY_ID = "id";
			public static final String KEY_SECRET = "secret";
			public static final String KEY_TOKEN = "token";

			private String id;
			private String secret;
			private String token;

			public DstConfig(final String id, final String secret, final String token) {
				this.id = id;
				this.secret = secret;
				this.token = token;
			}

			public DstConfig() {}

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

		public static class NamingConfig {

			public static final String KEY_API = "api";
			public static final String KEY_FS_ACCESS = "fsAccess";
			public static final String KEY_HEADERS = "headers";
			public static final String KEY_HEADER_CONNECTION = "Connection";
			public static final String KEY_HEADER_USER_AGENT= "User-Agent";
			public static final String KEY_NAMESPACE = "namespace";
			public static final String KEY_VERSIONING = "versioning";

			private final String api;
			private final boolean fsAccess;
			private String namespace;
			private final boolean versioning;
			private Map<String, String> headers;

			public NamingConfig(
				final String api, final boolean fsAccess, final String namespace,
				final boolean versioning, final Map<String, String> headers
			) {
				this.api = api;
				this.fsAccess = fsAccess;
				this.namespace = namespace;
				this.versioning = versioning;
				this.headers = headers;
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
}
