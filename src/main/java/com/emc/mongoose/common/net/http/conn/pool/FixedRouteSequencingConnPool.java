package com.emc.mongoose.common.net.http.conn.pool;
//
import com.emc.mongoose.common.concurrent.Sequencer;
import com.emc.mongoose.common.log.LogUtil;
//
import org.apache.http.HttpHost;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
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
//
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
/**
 Created by kurila on 15.10.15.
 */
public final class FixedRouteSequencingConnPool
extends BasicNIOConnPool
implements HttpConnPool<HttpHost, BasicNIOPoolEntry> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Sequencer connPoolSequencer;
	private final HttpHost route;
	//
	public FixedRouteSequencingConnPool(
		final ConnectingIOReactor ioReactor, final HttpHost route,
		final NIOConnFactory<HttpHost, NHttpClientConnection> connFactory,
		final int connectTimeout, final int batchSize
	) {
		super(ioReactor, connFactory, connectTimeout);
		this.route = route;
		connPoolSequencer = new Sequencer(
			"connPoolSequencer<" + route.toHostString() + ">", true, batchSize
		);
		connPoolSequencer.start();
	}
	//
	public void shutdown(final long waitMs)
	throws IOException {
		try {
			super.shutdown(waitMs);
		} finally {
			connPoolSequencer.interrupt();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Sequenced connection leasing ////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final class ConnLeaseTask
	extends BasicFuture<BasicNIOPoolEntry>
	implements RunnableFuture<BasicNIOPoolEntry> {
		//
		private final Object state;
		private final FutureCallback<BasicNIOPoolEntry> callback;
		//
		public ConnLeaseTask(final Object state, final FutureCallback<BasicNIOPoolEntry> callback) {
			super(null);
			this.state = state;
			this.callback = callback;
		}
		//
		@Override
		public void run() {
			try {
				FixedRouteSequencingConnPool.super.lease(route, state, callback);
			} catch(final Exception e) {
				if(!FixedRouteSequencingConnPool.super.isShutdown()) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to lease the connection");
				}
			}
		}
	}
	//
	@Override
	public final Future<BasicNIOPoolEntry> lease(
		final HttpHost route, final Object state, final FutureCallback<BasicNIOPoolEntry> callback
	) {
		try {
			return connPoolSequencer.submit(new ConnLeaseTask(state, callback));
		} catch(final InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Sequenced connection releasing //////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final class ConnReleaseTask
	extends BasicFuture<BasicNIOPoolEntry>
	implements RunnableFuture<BasicNIOPoolEntry> {
		//
		private final BasicNIOPoolEntry entry;
		private final boolean reusable;
		//
		public ConnReleaseTask(final BasicNIOPoolEntry entry, final boolean reusable) {
			super(null);
			this.entry = entry;
			this.reusable = reusable;
		}
		//
		@Override
		public void run() {
			try {
				FixedRouteSequencingConnPool.super.release(entry, reusable);
			} catch(final Exception e) {
				if(!FixedRouteSequencingConnPool.super.isShutdown()) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to release the connection");
				}
			}
		}
	}
	//
	@Override
	public final void release(final BasicNIOPoolEntry entry, final boolean reusable) {
		try {
			connPoolSequencer.submit(new ConnReleaseTask(entry, reusable));
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to enqueue connection release task");
		}
	}
}
