package com.emc.mongoose.config;

import java.util.List;
import java.util.Map;

/**
 Created on 11.07.16.
 */
public class DriverConfig {

	public static final String KEY_LOAD = "load";
	public static final String KEY_STORAGE = "storage";

	private final Load load;
	private final Storage storage;

	public DriverConfig(final Load load, final Storage storage) {
		this.load = load;
		this.storage = storage;
	}

	public Load load() {
		return load;
	}

	public Storage storage() {
		return storage;
	}

	public static class Load {
		
		public static final String KEY_CONCURRENCY = "concurrency";

		private final int concurrency;

		public Load(final int concurrency) {
			this.concurrency = concurrency;
		}

		public int getConcurrency() {
			return concurrency;
		}
	}

	public static class Storage {
		
		public static final String KEY_ADDRESSES = "addrs";
		public static final String KEY_AUTH = "auth";
		public static final String KEY_HTTP = "http";
		public static final String KEY_PORT = "port";
		public static final String KEY_TYPE = "type";
		
		private final int port;
		private final String type;
		private final Auth auth;
		private final Http http;
		private final List<String> addresses;

		public Storage(
			final int port, final String type, final Auth auth, final Http http,
			final List<String> addresses
		) {
			this.port = port;
			this.type = type;
			this.auth = auth;
			this.http = http;
			this.addresses = addresses;
		}

		public int getPort() {
			return port;
		}

		public String getType() {
			return type;
		}

		public Auth auth() {
			return auth;
		}

		public Http http() {
			return http;
		}

		public List<String> getAddresses() {
			return addresses;
		}

		public static class Auth {
			
			public static final String KEY_ID = "id";
			public static final String KEY_SECRET = "secret";
			public static final String KEY_TOKEN = "token";

			private String id;
			private String secret;
			private String token;

			public Auth(final String id, final String secret, final String token) {
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

		public static class Http {
			
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

			public Http(
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
