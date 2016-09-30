package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.LoadBalancer;
import com.emc.mongoose.storage.driver.base.StorageDriverBase;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;

import java.util.HashMap;
import java.util.Map;

import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;

/**
 Created by kurila on 30.09.16.
 */
public abstract class NetStorageDriverBase<I extends Item, O extends IoTask<I>>
extends StorageDriverBase<I, O>
implements NetStorageDriver<I, O> {
	
	protected final String storageNodeAddrs[];
	protected final int storageNodePort;
	protected final LoadBalancer<String> nodeBalancer;
	protected final EventLoopGroup workerGroup;
	protected final Bootstrap bootstrap;
	protected final Map<String, ChannelPool> connPoolMap = new HashMap<>();
	
	protected NetStorageDriverBase(
		final String runId, final AuthConfig authConfig, final LoadConfig loadConfig,
		final String srcContainer, final boolean verifyFlag
	) {
		super(runId, authConfig, loadConfig, srcContainer, verifyFlag);
		
	}
	
	protected abstract ClientHandlerBase<I, O> getClientHandlerImpl();
}
