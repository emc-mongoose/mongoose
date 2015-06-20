package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.util.scenario.shared.WSLoadBuilderFactory;
/**
 Created by kurila on 19.06.15.
 */
public class BasicWSClientBuilder<T extends WSObject, U extends BasicStorageClient<T>>
extends BasicStorageClientBuilder<T, U> {
	@Override @SuppressWarnings("unchecked")
	public U build() {
		return (U) new BasicStorageClient<>(
			rtConfig, WSLoadBuilderFactory.getInstance(rtConfig)
		);
	}
}
