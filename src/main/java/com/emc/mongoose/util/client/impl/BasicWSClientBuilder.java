package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.util.client.api.StorageClient;
//
import com.emc.mongoose.util.shared.WSLoadBuilderFactory;
/**
 Created by kurila on 19.06.15.
 */
public class BasicWSClientBuilder<T extends WSObject, U extends StorageClient<T>>
extends StorageClientBuilderBase<T, U> {
	@Override @SuppressWarnings("unchecked")
	public U build() {
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		return (U) new BasicStorageClient<>(
			rtConfig, WSLoadBuilderFactory.getInstance(rtConfig)
		);
	}
}
