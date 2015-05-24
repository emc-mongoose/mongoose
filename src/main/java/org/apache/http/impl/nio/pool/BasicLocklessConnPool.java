package org.apache.http.impl.nio.pool;
//
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.http.HttpHost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
//
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.pool.AdvancedConnPool;
import org.apache.http.nio.pool.LocklessConnPoolBase;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.pool.RouteSpecificPoolBase;
import org.apache.http.nio.pool.SocketAddressResolver;
import org.apache.http.nio.reactor.ConnectingIOReactor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by andrey on 23.05.15.
 The connection pool which:
 * determines and periodically updates the fastest route,
 * asynchronously releases the connections back into the pool
 */
public final class BasicLocklessConnPool
extends LocklessConnPoolBase<HttpHost, NHttpClientConnection, BasicNIOPoolEntry>
implements AdvancedConnPool {
	//
	private static final AtomicLong COUNTER = new AtomicLong();
	//
	private final int connectTimeout;
	//
	private final static class BasicAddressResolver
	implements SocketAddressResolver<HttpHost> {
		//
		@Override
		public final SocketAddress resolveLocalAddress(final HttpHost host) {
			return null;
		}
		//
		@Override
		public final SocketAddress resolveRemoteAddress(final HttpHost host) {
			final String hostname = host.getHostName();
			int port = host.getPort();
			if (port == -1) {
				if (host.getSchemeName().equalsIgnoreCase("http")) {
					port = 80;
				} else if (host.getSchemeName().equalsIgnoreCase("https")) {
					port = 443;
				}
			}
			return new InetSocketAddress(hostname, port);
		}

	}
	/**
	 * @since 4.3
	 */
	public BasicLocklessConnPool(
		final ConnectingIOReactor ioreactor,
		final NIOConnFactory<HttpHost, NHttpClientConnection> connFactory,
		final int connectTimeout) {
		super(ioreactor, connFactory, new BasicAddressResolver(), 2, 20);
		this.connectTimeout = connectTimeout;
	}

	/**
	 * @since 4.3
	 */
	public BasicLocklessConnPool(
		final ConnectingIOReactor ioreactor,
		final int connectTimeout,
		final ConnectionConfig config) {
		this(ioreactor, new BasicNIOConnFactory(config), connectTimeout);
	}

	/**
	 * @since 4.3
	 */
	public BasicLocklessConnPool(
		final ConnectingIOReactor ioreactor,
		final ConnectionConfig config) {
		this(ioreactor, new BasicNIOConnFactory(config), 0);
	}

	/**
	 * @since 4.3
	 */
	public BasicLocklessConnPool(final ConnectingIOReactor ioreactor) {
		this(ioreactor, new BasicNIOConnFactory(ConnectionConfig.DEFAULT), 0);
	}

	/**
	 * @deprecated (4.3) use {@link SocketAddressResolver}
	 */
	@Deprecated
	@Override
	protected SocketAddress resolveRemoteAddress(final HttpHost host) {
		return new InetSocketAddress(host.getHostName(), host.getPort());
	}

	/**
	 * @deprecated (4.3) use {@link SocketAddressResolver}
	 */
	@Deprecated
	@Override
	protected SocketAddress resolveLocalAddress(final HttpHost host) {
		return null;
	}

	@Override
	protected BasicNIOPoolEntry createEntry(final HttpHost host, final NHttpClientConnection conn) {
		final BasicNIOPoolEntry entry = new BasicNIOPoolEntry(
			Long.toString(COUNTER.getAndIncrement()), host, conn);
		entry.setSocketTimeout(conn.getSocketTimeout());
		return entry;
	}

	@Override
	public
	Future<BasicNIOPoolEntry> lease(
		final HttpHost route,
		final Object state,
		final FutureCallback<BasicNIOPoolEntry> callback) {
		return super.lease(route, state,
			this.connectTimeout, TimeUnit.MILLISECONDS, callback);
	}

	@Override
	public Future<BasicNIOPoolEntry> lease(
		final HttpHost route,
		final Object state) {
		return super.lease(route, state,
			this.connectTimeout, TimeUnit.MILLISECONDS, null);
	}

	@Override
	protected void onLease(final BasicNIOPoolEntry entry) {
		final NHttpClientConnection conn = entry.getConnection();
		conn.setSocketTimeout(entry.getSocketTimeout());
	}

	@Override
	protected void onRelease(final BasicNIOPoolEntry entry) {
		final NHttpClientConnection conn = entry.getConnection();
		entry.setSocketTimeout(conn.getSocketTimeout());
		conn.setSocketTimeout(0);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Balancer implementation
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static Logger LOG = LogManager.getLogger();
	private final static int BALANCER_REST_TIME_RATIO = 200;
	//
	private volatile HttpHost mostFreeRoute = null;
	/**
	 @return the current fastest route or null
	 */
	@Override
	public final HttpHost getMostFreeRoute() {
		return mostFreeRoute;
	}
	//
	private final Thread nodeConnBalancer = new Thread("nodeConnBalancer") {
		{
			setDaemon(true);
			start();
		}
		//
		@Override
		public final void run() {
			RouteSpecificPoolBase<HttpHost, NHttpClientConnection, BasicNIOPoolEntry> nextRoutePool;
			int maxFreeConnCount, nextFreeConnCount;
			long t;
			try {
				while(!isInterrupted()) {
					t = System.nanoTime();
					maxFreeConnCount = 0;
					for(final HttpHost nextRoute : routeToPool.keySet()) {
						nextFreeConnCount = routeToPool.get(nextRoute).getAvailableCount();
						if(nextFreeConnCount > maxFreeConnCount) {
							maxFreeConnCount = nextFreeConnCount;
							mostFreeRoute = nextRoute;
						}
					}
					// have a rest
					TimeUnit.NANOSECONDS.sleep((System.nanoTime() - t) * BALANCER_REST_TIME_RATIO);
				}
			} catch(final InterruptedException e) {
				LOG.debug(LogUtil.MSG, "Interrupted");
			} catch(final Exception e) {
				LogUtil.exception(LOG, Level.WARN, e, "Node balancing failure");
			}
		}
	};
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void shutdown(final long waitMilliSec)
		throws IOException {
		nodeConnBalancer.interrupt();
		super.shutdown(waitMilliSec);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
}
