package com.emc.mongoose.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 Created on 11.07.16.
 */
public class DriverConfig {

	public static final String KEY_LOAD = "load";
	public static final String KEY_STORAGE = "storage";

	private final LoadConfig loadConfig;
	private final StorageConfig storageConfig;

	public DriverConfig(final LoadConfig loadConfig, final StorageConfig storageConfig) {
		this.loadConfig = loadConfig;
		this.storageConfig = storageConfig;
	}

	public LoadConfig getLoadConfig() {
		return loadConfig;
	}

	public StorageConfig getStorageConfig() {
		return storageConfig;
	}

	public static class LoadConfig {
		
		public static final String KEY_CONCURRENCY = "concurrency";

		private final int concurrency;

		public LoadConfig(final int concurrency) {
			this.concurrency = concurrency;
		}

		public int getConcurrency() {
			return concurrency;
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
}
