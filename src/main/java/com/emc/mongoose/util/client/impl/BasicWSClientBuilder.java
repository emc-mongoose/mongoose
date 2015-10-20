package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.builder.DataLoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.util.client.api.StorageClient;
//
import com.emc.mongoose.util.factory.LoadBuilderFactory;
/**
 Created by kurila on 19.06.15.
 */
public class BasicWSClientBuilder<T extends WSObject, U extends StorageClient<T>>
extends StorageClientBuilderBase<T, U> {
	@Override @SuppressWarnings("unchecked")
	public U build() {
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		return (U) new BasicStorageClient<>(
			rtConfig, (DataLoadBuilder) LoadBuilderFactory.<T, LoadExecutor<T>>getInstance(rtConfig)
		);
	}
}
