package com.emc.mongoose.generator.config;

/**
 Created on 11.07.16.
 */
public class GeneratorConfig {

	public static final String KEY_ITEM = "item";
	private final ItemConfig itemConfig;

	public GeneratorConfig(final ItemConfig itemConfig) {
		this.itemConfig = itemConfig;
	}

	public ItemConfig item() {
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
