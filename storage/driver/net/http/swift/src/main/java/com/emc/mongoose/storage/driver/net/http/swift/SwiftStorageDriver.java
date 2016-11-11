package com.emc.mongoose.storage.driver.net.http.swift;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.storage.driver.net.http.base.BasicClientHandler;
import com.emc.mongoose.storage.driver.net.http.base.HttpStorageDriverBase;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftConstants.KEY_X_AUTH_TOKEN;
import static com.emc.mongoose.storage.driver.net.http.swift.SwiftConstants.KEY_X_COPY_FROM;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;

/**
 Created by andrey on 07.10.16.
 */
public class SwiftStorageDriver<I extends Item, O extends IoTask<I>>
extends HttpStorageDriverBase<I, O> {

	private static final Logger LOG = LogManager.getLogger();

	public SwiftStorageDriver(
		final String jobName, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final boolean verifyFlag, final SocketConfig socketConfig
	) throws IllegalStateException {
		super(jobName, loadConfig, storageConfig, verifyFlag, socketConfig);
	}
	
	@Override
	public final void channelCreated(final Channel channel)
	throws Exception {
		super.channelCreated(channel);
		final ChannelPipeline pipeline = channel.pipeline();
		pipeline.addLast(new BasicClientHandler<>(this, verifyFlag));
	}
	
	// TODO dst path calculation method: /v1/<namespace>/<container>/...
	
	@Override
	protected final void applyMetaDataHeaders(final HttpHeaders httpHeaders) {
	}

	@Override
	protected final void applyAuthHeaders(
		final HttpMethod httpMethod, final String dstUriPath, final HttpHeaders httpHeaders
	) {
		if(authToken != null && !authToken.isEmpty()) {
			httpHeaders.set(KEY_X_AUTH_TOKEN, authToken);
		}
	}

	@Override
	public final void applyCopyHeaders(final HttpHeaders httpHeaders, final String srcPath)
	throws URISyntaxException {
		httpHeaders.set(KEY_X_COPY_FROM, srcPath);
	}
	
	@Override
	public final String toString() {
		return String.format(super.toString(), "swift");
	}
}
