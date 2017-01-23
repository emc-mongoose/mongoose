package com.emc.mongoose.storage.driver.net.base.pool;

import com.emc.mongoose.ui.log.LogUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.pool.ChannelPoolHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 Created by andrey on 23.01.17.
 */
public class BasicConnPool
implements NonBlockingThrottledConnPool {

	private static final Logger LOG = LogManager.getLogger();

	private final Semaphore concurrencyThrottle;
	private final String nodes[];
	private final Map<String, Bootstrap> bootstrapMap;
	private final Map<String, Queue<Channel>> connsMap;

	public BasicConnPool(
		final Semaphore concurrencyThrottle, final String nodes[], final Bootstrap bootstrap,
		final ChannelPoolHandler connPoolHandler, final int defaultPort
	) {
		this.concurrencyThrottle = concurrencyThrottle;
		this.nodes = nodes;
		bootstrapMap = new HashMap<>();
		connsMap = new HashMap<>();
		for(final String node : nodes) {
			final InetSocketAddress nodeAddr;
			if(node.contains(":")) {
				final String addrParts[] = node.split(":");
				nodeAddr = new InetSocketAddress(addrParts[0], Integer.parseInt(addrParts[1]));
			} else {
				nodeAddr = new InetSocketAddress(node, defaultPort);
			}
			bootstrapMap.put(
				node,
				bootstrap
					.clone()
					.remoteAddress(nodeAddr)
					.handler(
						new ChannelInitializer<Channel>() {
							@Override
							protected final void initChannel(final Channel conn)
							throws Exception {
								assert conn.eventLoop().inEventLoop();
								connPoolHandler.channelCreated(conn);
							}
						}
					)
			);
			connsMap.put(node, new ConcurrentLinkedQueue<>());
		}
	}

	protected Channel newConnection(final String addr)
	throws InterruptedException {
		return bootstrapMap.get(addr).connect().sync().channel();
	}

	@Override
	public final Channel lease() {
		Channel conn = null;
		if(concurrencyThrottle.tryAcquire()) {
			Queue<Channel> connQueue;
			for(final String nodeAddr : nodes) {
				connQueue = connsMap.get(nodeAddr);
				conn = connQueue.poll();
				if(conn != null) {
					conn.attr(ATTR_KEY_NODE).set(nodeAddr);
					break;
				}
			}
			if(conn == null) {
				final String nodeAddr = nodes[ThreadLocalRandom.current().nextInt(nodes.length)];
				try {
					conn = newConnection(nodeAddr);
					conn.attr(ATTR_KEY_NODE).set(nodeAddr);
				} catch(final InterruptedException e) {
					concurrencyThrottle.release();
					LogUtil.exception(
						LOG, Level.ERROR, e, "Failed to create a new connection to \"{}\"", nodeAddr
					);
				}
			}
		}
		return conn;
	}

	@Override
	public final void release(final Channel conn) {
		final Queue<Channel> connQueue = connsMap.get(conn.attr(ATTR_KEY_NODE).get());
		connQueue.add(conn);
		concurrencyThrottle.release();
	}

	@Override
	public void close()
	throws IOException {
		Queue<Channel> connQueue;
		Channel nextConn;
		for(final String nodeAddr : nodes) {
			connQueue = connsMap.get(nodeAddr);
			while(null != (nextConn = connQueue.poll())) {
				nextConn.close();
			}
		}
		connsMap.clear();
		bootstrapMap.clear();
	}
}
