package com.emc.mongoose.core.impl.load.executor.util;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
import com.emc.mongoose.common.logging.LogUtil;
import org.apache.http.HttpHost;
import org.apache.http.config.ConnectionConfig;
//
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.pool.BasicNIOPoolEntry;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.reactor.ConnectingIOReactor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 19.05.15.
 The connection pool extenstion which makes the connections releasing asynchronous
 */
public class AsyncReleaseWSConnPool
extends BasicNIOConnPool {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public AsyncReleaseWSConnPool(
		final ConnectingIOReactor ioReactor,
		final NIOConnFactory<HttpHost, NHttpClientConnection> connFactory, final int connectTimeOut
	) {
		super(ioReactor, connFactory, connectTimeOut);
	}
	//
	public AsyncReleaseWSConnPool(
		final ConnectingIOReactor ioreactor, final int connectTimeout, final ConnectionConfig config
	) {
		super(ioreactor, connectTimeout, config);
	}
	//
	public AsyncReleaseWSConnPool(
		final ConnectingIOReactor ioreactor, final ConnectionConfig config
	) {
		super(ioreactor, config);
	}
	//
	public AsyncReleaseWSConnPool(final ConnectingIOReactor ioReactor) {
		super(ioReactor);
	}
	//
	private final ExecutorService connReleaseExecSvc = Executors.newSingleThreadExecutor(
		new NamingWorkerFactory("connReleaseWorker")
	);
	//
	public final static class ConnReleaseTask
	implements Reusable<ConnReleaseTask>, Runnable {
		//
		private final static InstancePool<ConnReleaseTask> TASKS_POOL = new InstancePool<>(
			ConnReleaseTask.class
		);
		public static ConnReleaseTask getInstance(
			final BasicNIOConnPool connPool, final BasicNIOPoolEntry poolEntry,
			final boolean isEntryReusable
		) {
			return TASKS_POOL.take(connPool, poolEntry, isEntryReusable);
		}
		//
		private AsyncReleaseWSConnPool connPool = null;
		private BasicNIOPoolEntry poolEntry = null;
		private boolean isEntryReusable = true;
		//
		@Override
		public final Reusable<ConnReleaseTask> reuse(final Object... args)
		throws IllegalArgumentException, IllegalStateException {
			if(args != null) {
				if(args.length > 0) {
					connPool = AsyncReleaseWSConnPool.class.cast(args[0]);
				}
				if(args.length > 1) {
					poolEntry = BasicNIOPoolEntry.class.cast(args[1]);
				}
				if(args.length > 2) {
					isEntryReusable = (boolean) args[2];
				}
			}
			return this;
		}
		//
		@Override
		public void release() {
			TASKS_POOL.release(this);
		}
		//
		@Override
		public void run() {
			connPool.releaseSynchronously(poolEntry, isEntryReusable);
			release();
		}
	}
	//
	@Override
	public final void release(final BasicNIOPoolEntry poolEntry, final boolean isEntryReusable) {
		try {
			connReleaseExecSvc.submit(ConnReleaseTask.getInstance(this, poolEntry, isEntryReusable));
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.WARN, e, "Failed to submit the connection for releasing");
		}
	}
	//
	private void releaseSynchronously(final BasicNIOPoolEntry entry, final boolean isReusable) {
		super.release(entry, isReusable);
	}
	//
	@Override
	public final void shutdown(final long timeOutMilliSec)
	throws IOException {
		connReleaseExecSvc.shutdown();
		long t = System.currentTimeMillis();
		try {
			connReleaseExecSvc.awaitTermination(timeOutMilliSec, TimeUnit.MILLISECONDS);
		} catch(final InterruptedException ignored) {
		} finally {
			connReleaseExecSvc.shutdownNow();
		}
		t = System.currentTimeMillis() - t;
		super.shutdown(timeOutMilliSec > t ? timeOutMilliSec - t : 0);
	}
}
