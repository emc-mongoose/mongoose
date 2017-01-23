package com.emc.mongoose.storage.driver.net.base.pool;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.io.Closeable;

/**
 Created by andrey on 23.01.17.
 */
public interface NonBlockingThrottledConnPool
extends Closeable {

	AttributeKey<String> ATTR_KEY_NODE = AttributeKey.valueOf("node");

	Channel lease();

	void release(final Channel conn);

}
