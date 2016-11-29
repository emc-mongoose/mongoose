package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 Created by kurila on 30.09.16.
 */
public interface NetStorageDriver<I extends Item, O extends IoTask<I, R>, R extends IoResult>
extends StorageDriver<I, O, R> {
	
	AttributeKey<IoTask> ATTR_KEY_IOTASK = AttributeKey.valueOf("ioTask");

	void complete(final Channel channel, final O ioTask);
}
