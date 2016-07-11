package com.emc.mongoose.config;

/**
 Created on 11.07.16.
 */
public class MonitorConfig {

	public static final String KEY_JOB = "job";
	private final Job job;

	public MonitorConfig(final Job job) {
		this.job = job;
	}

	public Job job() {
		return job;
	}

	public static class Job {

		public static final String KEY_TYPE = "type";
		public static final String KEY_DATA = "data";
		public static final String KEY_DESTINATION = "dst";
		public static final String KEY_SOURCE = "src";
		public static final String KEY_NAMING = "naming";
		private final String type;
		private final Data data;
		private final Destination dst;
		private final Source src;
		private final Naming naming;

		public Job(
			final String type, final Data data, final Destination dst, final Source src,
			final Naming naming
		) {
			this.type = type;
			this.data = data;
			this.dst = dst;
			this.src = src;
			this.naming = naming;
		}

		public String getType() {
			return type;
		}

		public Data data() {
			return data;
		}

		public Destination destination() {
			return dst;
		}

		public Source source() {
			return src;
		}

		public Naming naming() {
			return naming;
		}

		public static class Data {

			public static final String KEY_CONTENT = "content";
			public static final String KEY_RANGES = "ranges";
			public static final String KEY_SIZE = "size";
			public static final String KEY_VERIFY = "verify";
			private Content content;
			private int ranges;
			private String size;
			private boolean verify;

			public Data(
				final Content content, final int ranges, final String size, final boolean verify
			) {
				this.content = content;
				this.ranges = ranges;
				this.size = size;
				this.verify = verify;
			}

			public Content content() {
				return content;
			}

			public int getRanges() {
				return ranges;
			}

			public String getSize() {
				return size;
			}

			public boolean isVerify() {
				return verify;
			}

			public static class Content {

				public static final String KEY_FILE = "file";
				public static final String KEY_SEED = "seed";
				public static final String KEY_RING_SIZE = "ringSize";
				private String file;
				private String seed;
				private String ringSize;

				public Content(final String file, final String seed, final String ringSize) {
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

		public static class Destination {

			public static final String KEY_CONTAINER = "container";
			public static final String KEY_FILE = "file";
			private String container;
			private String file;

			public Destination(final String container, final String file) {
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

		public static class Source {

			public static final String KEY_CONTAINER = "container";
			public static final String KEY_FILE = "file";
			public static final String KEY_BATCH_SIZE = "batchSize";
			private String container;
			private String file;
			private int batchSize;

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

		public static class Naming {

			public static final String KEY_TYPE = "type";
			public static final String KEY_PREFIX = "prefix";
			public static final String KEY_RADIX = "radix";
			public static final String KEY_OFFSET = "offset";
			public static final String KEY_LENGTH = "length";
			private final String type;
			private String prefix;
			private final int radix;
			private final int offset;
			private final int length;

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
