package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.result.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 Created by kurila on 30.09.16.
 */
public interface NetStorageDriver<I extends Item, R extends IoResult, O extends IoTask<I, R>>
extends StorageDriver<I, R, O> {
	
	AttributeKey<IoTask> ATTR_KEY_IOTASK = AttributeKey.valueOf("ioTask");

	void complete(final Channel channel, final O ioTask);
}
