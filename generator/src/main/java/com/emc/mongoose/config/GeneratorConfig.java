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

			public static final String KEY_CONTENT = "content";
			public static final String KEY_RANGES = "ranges";
			public static final String KEY_SIZE = "size";
			public static final String KEY_VERIFY = "verify";

			private final Content content;
			private final int ranges;
			private final String size;
			private final boolean verify;

			private ContentConfig contentConfig;
			private int ranges;
			private String size;
			private boolean verify;

			public DataConfig(
				final ContentConfig contentConfig, final int ranges, final String size,
				final boolean verify
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
				public String getFile() {
					return file;
				}

			public SrcConfig() {}

			public String getId() {
				return id;
			}
				public String getSeed() {
					return seed;
				}

			public String getSecret() {
				return secret;
			}

			public String getToken() {
				return token;
			}
		}
		public static class DstConfig {
				public String getRingSize() {
					return ringSize;
				}
			}
		}

		public static class Destination {

			public static final String KEY_CONTAINER = "container";
			public static final String KEY_FILE = "file";
			private final String container;
			private final String file;

			public Destination(final String container, final String file) {
				this.container = container;
				this.file = file;
			}

			public DstConfig(final String id, final String secret, final String token) {
				this.id = id;
				this.secret = secret;
				this.token = token;
			public String getContainer() {
				return container;
			}

			public DstConfig() {}
			public String getFile() {
				return file;
			}
		}

		public static class Source {

			public static final String KEY_CONTAINER = "container";
			public static final String KEY_FILE = "file";
			public static final String KEY_BATCH_SIZE = "batchSize";
			private final String container;
			private final String file;
			private final int batchSize;

			public Source(final String file, final String container, final int batchSize) {
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
				final String api, final boolean fsAccess, final String namespace,
				final boolean versioning, final Map<String, String> headers
			public Naming(
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
