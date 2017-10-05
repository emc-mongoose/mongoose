package com.emc.mongoose.tests.unit;

import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.storage.driver.net.http.swift.SwiftStorageDriver;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.auth.AuthConfig;
import com.emc.mongoose.ui.config.storage.net.http.HttpConfig;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import org.junit.After;

import java.util.ArrayDeque;
import java.util.Queue;

public class SwiftStorageDriverTest
extends SwiftStorageDriver {

	private static final String UID = "user1";
	private static final String SECRET = "u5QtPuQx+W5nrrQQEg7nArBqSgC8qLiDt2RhQthb";

	private static Config getConfig() {
		try {
			final Config config = Config.loadDefaults();
			final StorageConfig storageConfig = config.getStorageConfig();
			final HttpConfig httpConfig = storageConfig.getNetConfig().getHttpConfig();
			httpConfig.setNamespace("ns1");
			httpConfig.setFsAccess(true);
			httpConfig.setVersioning(true);
			final AuthConfig authConfig = storageConfig.getAuthConfig();
			authConfig.setUid(UID);
			authConfig.setSecret(SECRET);
			return config;
		} catch(final Throwable cause) {
			throw new RuntimeException(cause);
		}
	}

	private final Queue<FullHttpRequest> httpRequestsLog = new ArrayDeque<>();

	public SwiftStorageDriverTest()
	throws Exception {
		this(getConfig());
	}

	private SwiftStorageDriverTest(final Config config)
	throws Exception {
		super(
			config.getTestConfig().getStepConfig().getId(),
			DataInput.getInstance(
				config.getItemConfig().getDataConfig().getInputConfig().getFile(),
				config.getItemConfig().getDataConfig().getInputConfig().getSeed(),
				config.getItemConfig().getDataConfig().getInputConfig().getLayerConfig().getSize(),
				config.getItemConfig().getDataConfig().getInputConfig().getLayerConfig().getCache()
			),
			config.getLoadConfig(), config.getStorageConfig(),
			config.getItemConfig().getDataConfig().getVerify()
		);
	}

	@Override
	protected FullHttpResponse executeHttpRequest(final FullHttpRequest httpRequest) {
		httpRequestsLog.add(httpRequest);
		return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
	}

	@After
	public void tearDown() {
		httpRequestsLog.clear();
	}
}
