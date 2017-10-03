package com.emc.mongoose.tests.perf.mock;

import com.emc.mongoose.storage.driver.net.base.pool.BasicMultiNodeConnPool;
import com.emc.mongoose.storage.driver.net.base.pool.NonBlockingConnPool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.pool.ChannelPoolHandler;

import java.util.concurrent.Semaphore;

/**
 Created by andrey on 12.05.17.
 */
public final class BasicMultiNodeConnPoolMock
extends BasicMultiNodeConnPool
implements NonBlockingConnPool {

	public BasicMultiNodeConnPoolMock(
		final int concurrencyLevel, final Semaphore concurrencyThrottle, final String[] nodes,
		final Bootstrap bootstrap, final ChannelPoolHandler connPoolHandler, final int defaultPort,
		final int connFailSeqLenLimit
	) {
		super(
			concurrencyLevel, concurrencyThrottle, nodes, bootstrap, connPoolHandler, defaultPort,
			connFailSeqLenLimit
		);
	}

	protected final Channel connect(final String addr) {
		return new EmbeddedChannel();
	}
}
