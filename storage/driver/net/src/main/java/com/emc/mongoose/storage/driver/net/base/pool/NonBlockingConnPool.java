package com.emc.mongoose.storage.driver.net.base.pool;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.io.Closeable;
import java.util.List;

/**
 Created by andrey on 23.01.17.
 */
public interface NonBlockingConnPool
extends Closeable {

	AttributeKey<String> ATTR_KEY_NODE = AttributeKey.valueOf("node");

	/**
	 Get the connection immediately (don't block) or null. The caller should decide whether to fail,
	 to sleep or to block if no connection is available at the moment.
	 @throws ConnLeaseException if no connections are in the pool and was unable to create new connection
	 */
	Channel lease()
	throws ConnLeaseException;
	
	/**
	 Get multiple connections immediately (don't block).
	 @param conns The output buffer to store the leased connections
	 @param maxCount The count limit
	 @return the actual count of the connections leased successfully, 0 if no connections available
	 @throws ConnLeaseException if no connections are in the pool and was unable to create new connection
	 */
	int lease(final List<Channel> conns, final int maxCount)
	throws ConnLeaseException;

	/** Release the connection back into the pool */
	void release(final Channel conn);
	
	/** Release the connections back into the pool */
	void release(final List<Channel> conns);
}
