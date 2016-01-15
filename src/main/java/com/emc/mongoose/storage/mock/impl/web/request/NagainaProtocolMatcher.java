package com.emc.mongoose.storage.mock.impl.web.request;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
import io.netty.handler.codec.http.HttpRequest;

import static io.netty.handler.codec.http.HttpHeaders.Names.AUTHORIZATION;

public final class NagainaProtocolMatcher {

	private final static String
			SWIFT_AUTH = "auth",
			S3_AUTH_PREFIX = RunTimeConfig.getContext().getApiS3AuthPrefix() + " ",
			ATMOS_URI_BASE_PATH = "/rest";

	private final String apiBasePathSwift;

	public NagainaProtocolMatcher(RunTimeConfig runTimeConfig) {
		apiBasePathSwift = runTimeConfig.getString(WSRequestConfigImpl.KEY_CONF_SVC_BASEPATH);
	}

	public boolean matchesS3(HttpRequest request) {
		String auth = request.headers().get(AUTHORIZATION);
		return auth != null && auth.startsWith(S3_AUTH_PREFIX);
	}

	public boolean matchesSwift(HttpRequest request) {
		String uri = request.getUri();
		return uri.startsWith(SWIFT_AUTH, 1) || uri.startsWith(apiBasePathSwift, 1);
	}

	public boolean matchesAtmos(HttpRequest request) {
		return request.getUri().startsWith(ATMOS_URI_BASE_PATH);
	}

}
