package com.emc.mongoose.storage.driver.net.base.pool;

import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
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
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by andrey on 23.01.17.
 The simple multi-endpoint connection pool which is throttled externally by providing the semaphore.
 The provided semaphore limits the count of the simultaneously used connections.
 Based on netty.
 */
public class BasicMultiNodeConnPool
implements NonBlockingConnPool {

	private static final Logger LOG = LogManager.getLogger();

	private final Semaphore concurrencyThrottle;
	private final String nodes[];
	private final int n;
	private final Map<String, Bootstrap> bootstrapMap;
	private final Map<String, Queue<Channel>> connsMap;
	private final AtomicLong connCount = new AtomicLong(0);

	public BasicMultiNodeConnPool(
		final Semaphore concurrencyThrottle, final String nodes[], final Bootstrap bootstrap,
		final ChannelPoolHandler connPoolHandler, final int defaultPort
	) {
		this.concurrencyThrottle = concurrencyThrottle;
		this.nodes = nodes;
		this.n = nodes.length;
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

	protected Channel connect()
	throws InterruptedException {
		final String addr = nodes[(int) (connCount.getAndIncrement() % nodes.length)];
		LOG.debug(Markers.MSG, "New connection to \"{}\"", addr);
		final Channel conn = bootstrapMap.get(addr).connect().sync().channel();
		conn.attr(ATTR_KEY_NODE).set(addr);
		return conn;
	}
	
	protected Channel poll() {
		final int i = ThreadLocalRandom.current().nextInt(n);
		Queue<Channel> connQueue;
		Channel conn;
		for(int j = i; j < i + n; j ++) {
			connQueue = connsMap.get(nodes[j % n]);
			conn = connQueue.poll();
			if(conn != null) {
				return conn;
			}
		}
		return null;
	}

	@Override
	public final Channel lease() {
		Channel conn = null;
		if(concurrencyThrottle.tryAcquire()) {
			if((conn = poll()) == null) {
				try {
					conn = connect();
				} catch(final InterruptedException e) {
					concurrencyThrottle.release();
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to create a new connection");
				}
			}
			assert conn != null;
		}
		return conn;
	}
	
	@Override
	public final int lease(final List<Channel> conns, final int maxCount) {
		int availableCount = concurrencyThrottle.drainPermits();
		if(availableCount == 0) {
			return availableCount;
		}
		if(availableCount > maxCount) {
			concurrencyThrottle.release(availableCount - maxCount);
			availableCount = maxCount;
		}
		
		Channel conn;
		for(int i = 0; i < availableCount; i ++) {
			if((conn = poll()) == null) {
				try {
					conn = connect();
				} catch(final InterruptedException e) {
					concurrencyThrottle.release();
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to create a new connection");
				}
			}
			assert conn != null;
			conns.add(conn);
		}
		return availableCount;
	}

	@Override
	public final void release(final Channel conn) {
		if(conn.isActive()) {
			final Queue<Channel> connQueue = connsMap.get(conn.attr(ATTR_KEY_NODE).get());
			connQueue.add(conn);
		}
		concurrencyThrottle.release();
	}
	
	@Override
	public final void release(final List<Channel> conns) {
		Queue<Channel> connQueue;
		for(final Channel conn : conns) {
			if(conn.isActive()) {
				connQueue = connsMap.get(conn.attr(ATTR_KEY_NODE).get());
				connQueue.add(conn);
			}
			concurrencyThrottle.release();
		}
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
