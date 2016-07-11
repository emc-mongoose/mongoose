package com.emc.mongoose.config;

import java.util.List;
import java.util.Map;

/**
 Created on 11.07.16.
 */
public class GeneratorConfig {
	public static final String KEY_ITEM = "item";

	private final Item item;

	public GeneratorConfig(final Item item) {
		this.item = item;
	}

	public Item item() {
		return item;
	}

	public static class Item {

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

		public Item(
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

			public static final String KEY_RANGES = "ranges";
			public static final String KEY_SIZE = "size";
			public static final String KEY_VERIFY = "verify";

			private Content content;
			private int ranges;
			private String size;
			private boolean verify;

			public Data(final int ranges, final String size, final boolean verify) {
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
			}
		}

		public static class Source {

			public static final String KEY_ID = "id";
			public static final String KEY_SECRET = "secret";
			public static final String KEY_TOKEN = "token";

			private String id;
			private String secret;
			private String token;

			public Source(final String id, final String secret, final String token) {
				this.id = id;
				this.secret = secret;
				this.token = token;
			}

			public Source() {}

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
		public static class Destination {

			public static final String KEY_ID = "id";
			public static final String KEY_SECRET = "secret";
			public static final String KEY_TOKEN = "token";

			private String id;
			private String secret;
			private String token;

			public Destination(final String id, final String secret, final String token) {
				this.id = id;
				this.secret = secret;
				this.token = token;
			}

			public Destination() {}

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

		public static class Naming {

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

			public Naming(
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
