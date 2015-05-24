package com.emc.mongoose.core.impl.net;
//
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.net.WSConnPool;
//
import org.apache.http.HttpHost;
import org.apache.http.config.ConnectionConfig;
//
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
//
import org.apache.http.pool.PoolStats;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 23.05.15.
 The connection pool which:
 * determines and periodically updates the fastest route,
 * asynchronously releases the connections back into the pool
 */
public final class BasicWSConnPool
extends BasicNIOConnPool
implements WSConnPool {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static int BALANCER_SLEEP_MILLISEC = 1000;
	//
	private final Map<HttpHost, Integer> busyConnCountPerRoute = new HashMap<>();
	public BasicWSConnPool(
		final ConnectingIOReactor ioReactor,
		final NIOConnFactory<HttpHost, NHttpClientConnection> connFactory, final int connectTimeout
	) {
		super(ioReactor, connFactory, connectTimeout);
	}
	//
	public BasicWSConnPool(
		final ConnectingIOReactor ioReactor, final int connectTimeout, final ConnectionConfig config
	) {
		super(ioReactor, connectTimeout, config);
	}
	//
	public BasicWSConnPool(final ConnectingIOReactor ioReactor, final ConnectionConfig config) {
		super(ioReactor, config);
	}
	//
	public BasicWSConnPool(final ConnectingIOReactor ioReactor) {
		super(ioReactor);
	}
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
			PoolStats nextRouteStats;
			HttpHost currBestRoute;
			int minBusyConnCount, nextBusyConnCount;
			try {
				while(!isInterrupted()) {
					minBusyConnCount = Integer.MAX_VALUE;
					currBestRoute = null;
					// determine the route having the minimum busy connections count
					// this should correspond to the fastest target node
					for(final HttpHost nextRoute : getRoutes()) {
						nextRouteStats = getStats(nextRoute);
						nextBusyConnCount = nextRouteStats.getLeased() + nextRouteStats.getPending();
						if(nextBusyConnCount < minBusyConnCount) {
							minBusyConnCount = nextBusyConnCount;
							currBestRoute = nextRoute;
						}
					}
					// update the field
					mostFreeRoute = currBestRoute;
					if(LOG.isTraceEnabled(LogUtil.MSG)) {
						LOG.trace(LogUtil.MSG, "Fastest route: {}", mostFreeRoute);
					}
					// have a rest
					TimeUnit.MILLISECONDS.sleep(BALANCER_SLEEP_MILLISEC);
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
		busyConnCountPerRoute.clear();
		super.shutdown(waitMilliSec);
	}
}
