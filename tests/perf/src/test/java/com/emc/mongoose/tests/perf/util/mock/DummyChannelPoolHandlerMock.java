package com.emc.mongoose.tests.perf.util.mock;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;

/**
 Created by andrey on 13.05.17.
 */
public final class DummyChannelPoolHandlerMock
implements ChannelPoolHandler {

	@Override
	public final void channelReleased(final Channel ch)
	throws Exception {
	}

	@Override
	public final void channelAcquired(final Channel ch)
	throws Exception {
	}

	@Override
	public final void channelCreated(final Channel ch)
	throws Exception {
	}
}
