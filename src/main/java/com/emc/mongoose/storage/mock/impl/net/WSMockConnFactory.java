package com.emc.mongoose.storage.mock.impl.net;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
//
import org.apache.commons.collections4.queue.CircularFifoQueue;
//
import org.apache.http.config.ConnectionConfig;
//
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.reactor.IOSession;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/**
 * Created by olga on 04.02.15.
 */
public final class WSMockConnFactory
extends DefaultNHttpServerConnectionFactory {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Queue<NHttpServerConnection> connCache;
	private final ScheduledExecutorService connKillExecutor;
	private final int connCacheSize, connKillPeriodSec;
	//
	public WSMockConnFactory(
		final RunTimeConfig runTimeConfig, final ConnectionConfig config
	) {
		super(config);
		connKillExecutor = Executors.newScheduledThreadPool(
			1, new NamingWorkerFactory("connKiller")
		);
		connCacheSize = runTimeConfig.getStorageMockFaultConnCacheSize();
		connCache = new CircularFifoQueue<>(connCacheSize);
		connKillPeriodSec = runTimeConfig.getStorageMockFaultPeriodSec();
		connKillExecutor.scheduleAtFixedRate(
			new FailSomeConnectionsTask(connCache),
			connKillPeriodSec, connKillPeriodSec, TimeUnit.SECONDS
		);
	}
	//
	@Override
	public final DefaultNHttpServerConnection createConnection(final IOSession session) {
		final DefaultNHttpServerConnection connection = super.createConnection(session);
		if(connCacheSize > 0 && connCache.add(connection)) {
			if(LOG.isTraceEnabled(LogUtil.MSG)) {
				LOG.trace(LogUtil.MSG, "Added the connection \"{}\" to the cache", connection);
			}
		}
		return connection;
	}
	///////////////////////////////////////////////////////////////////////////////
	private final static class FailSomeConnectionsTask
	implements Runnable {
		//
		private final Queue<NHttpServerConnection> connCache;
		//
		public FailSomeConnectionsTask(final Queue<NHttpServerConnection> connCache) {
			this.connCache = connCache;
		}
		//
		@Override
		public final void run() {
			for(final NHttpServerConnection conn : connCache) {
				try {
					if(NHttpConnection.ACTIVE == conn.getStatus()) {
						conn.close();
					}
				} catch (final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to close the connection: {}", conn
					);
				}
			}
		}
	}
}
