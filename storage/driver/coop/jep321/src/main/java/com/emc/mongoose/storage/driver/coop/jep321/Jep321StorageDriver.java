package com.emc.mongoose.storage.driver.coop.jep321;

public interface Jep321StorageDriver {

	String PROTOCOL_HTTP = "http";
	String PROTOCOL_HTTPS = "https";

	enum HttpMethod {
		DELETE,
		GET,
		HEAD,
		OPTIONS,
		PUT,
		POST,
	}
}
