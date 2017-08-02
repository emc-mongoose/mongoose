package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.StorageDriver;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.Map;

/**
 Created by kurila on 30.09.16.
 */
public interface NetStorageDriver<I extends Item, O extends IoTask<I>>
extends StorageDriver<I, O> {

	enum Transport {
		NIO,
		EPOLL,
		KQUEUE
	}

	Map<Transport, String> IO_EXECUTOR_IMPLS = new HashMap<Transport, String>() {
		{
			put(Transport.NIO, "io.netty.channel.nio.NioEventLoopGroup");
			put(Transport.EPOLL, "io.netty.channel.epoll.EpollEventLoopGroup");
			put(Transport.KQUEUE, "io.netty.channel.kqueue.KQueueEventLoopGroup");
		}
	};

	Map<Transport, String> SOCKET_CHANNEL_IMPLS = new HashMap<Transport, String>() {
		{
			put(Transport.NIO, "io.netty.channel.socket.nio.NioSocketChannel");
			put(Transport.EPOLL, "io.netty.channel.epoll.EpollSocketChannel");
			put(Transport.KQUEUE, "io.netty.channel.kqueue.KQueueSocketChannel");
		}
	};
	
	AttributeKey<IoTask> ATTR_KEY_IOTASK = AttributeKey.valueOf("ioTask");

	void complete(final Channel channel, final O ioTask);
}
