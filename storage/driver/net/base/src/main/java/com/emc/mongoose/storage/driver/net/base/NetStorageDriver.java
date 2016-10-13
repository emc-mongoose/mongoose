package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.storage.StorageDriver;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.io.IOException;

/**
 Created by kurila on 30.09.16.
 */
public interface NetStorageDriver<I extends Item, O extends IoTask<I>>
extends StorageDriver<I, O> {
	
	AttributeKey<IoTask> ATTR_KEY_IOTASK = AttributeKey.valueOf("ioTask");

	void complete(final Channel channel, final O ioTask)
	throws IOException;
}
