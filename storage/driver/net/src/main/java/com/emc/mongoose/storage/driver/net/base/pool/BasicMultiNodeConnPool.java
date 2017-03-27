package com.emc.mongoose.storage.driver.net.base.pool;

import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.pool.ChannelPoolHandler;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

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
	private final Object2IntMap<String> connsCountMap;

	public BasicMultiNodeConnPool(
		final int concurrencyLevel, final Semaphore concurrencyThrottle,  final String nodes[],
		final Bootstrap bootstrap, final ChannelPoolHandler connPoolHandler, final int defaultPort
	) {
		this.concurrencyThrottle = concurrencyThrottle;
		if(nodes.length == 0) {
			throw new IllegalArgumentException("Empty nodes array argument");
		}
		this.nodes = nodes;
		this.n = nodes.length;
		bootstrapMap = new HashMap<>(n);
		connsMap = new HashMap<>(n);
		connsCountMap = new Object2IntOpenHashMap<>(n);

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
			connsCountMap.put(node, 0);
		}

		// pre-create the connections
		for(int i = 0; i < concurrencyLevel; i ++) {
			final Channel conn = connect();
			final String nodeAddr = conn.attr(ATTR_KEY_NODE).get();
			if(conn.isActive()) {
				final Queue<Channel> connQueue = connsMap.get(nodeAddr);
				if(connQueue != null) {
					connQueue.add(conn);
				}
			} else {
				synchronized(connsCountMap) {
					connsCountMap.put(nodeAddr, connsCountMap.get(nodeAddr) - 1);
				}
				conn.close();
			}
		}
	}

	protected Channel connect() {
		Channel conn = null;
		String selectedNodeAddr = null, nextNodeAddr;
		int minConnsCount = Integer.MAX_VALUE, nextConnsCount = 0;
		synchronized(connsCountMap) {
			for(int i = 0; i < n; i ++) {
				nextNodeAddr = nodes[i];
				nextConnsCount = connsCountMap.get(nextNodeAddr);
				if(nextConnsCount == 0) {
					selectedNodeAddr = nextNodeAddr;
					break;
				} else if(nextConnsCount < minConnsCount) {
					minConnsCount = nextConnsCount;
					selectedNodeAddr = nextNodeAddr;
				}
			}
			LOG.debug(Markers.MSG, "New connection to \"{}\"", selectedNodeAddr);
			try {
				conn = bootstrapMap.get(selectedNodeAddr).connect().sync().channel();
				conn.attr(ATTR_KEY_NODE).set(selectedNodeAddr);
				connsCountMap.put(selectedNodeAddr, nextConnsCount + 1);
			} catch(final Exception e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to create a new connection to {}", selectedNodeAddr
				);
			}
		}
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
			if(null == (conn = poll())) {
				conn = connect();
			}
			if(conn == null) {
				concurrencyThrottle.release();
			}
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
			if(null == (conn = poll())) {
				conn = connect();
			}
			if(conn == null) {
				concurrencyThrottle.release(availableCount - i);
				return i;
			} else {
				conns.add(conn);
			}
		}
		return availableCount;
	}

	@Override
	public final void release(final Channel conn) {
		final String nodeAddr = conn.attr(ATTR_KEY_NODE).get();
		if(conn.isActive()) {
			final Queue<Channel> connQueue = connsMap.get(nodeAddr);
			if(connQueue != null) {
				connQueue.add(conn);
			}
		} else {
			synchronized(connsCountMap) {
				connsCountMap.put(nodeAddr, connsCountMap.get(nodeAddr) - 1);
			}
			conn.close();
		}
		concurrencyThrottle.release();
	}
	
	@Override
	public final void release(final List<Channel> conns) {
		String nodeAddr;
		Queue<Channel> connQueue;
		for(final Channel conn : conns) {
			nodeAddr = conn.attr(ATTR_KEY_NODE).get();
			if(conn.isActive()) {
				connQueue = connsMap.get(nodeAddr);
				connQueue.add(conn);
			} else {
				synchronized(connsCountMap) {
					connsCountMap.put(nodeAddr, connsCountMap.get(nodeAddr) - 1);
				}
				conn.close();
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
		synchronized(connsCountMap) {
			connsCountMap.clear();
		}
		bootstrapMap.clear();
	}
}
