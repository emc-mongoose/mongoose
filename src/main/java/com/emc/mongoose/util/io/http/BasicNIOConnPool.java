package com.emc.mongoose.util.io.http;
//
import com.emc.mongoose.util.logging.Markers;
import org.apache.http.HttpHost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.pool.AbstractNIOConnPool;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.pool.SocketAddressResolver;
import org.apache.http.nio.reactor.ConnectingIOReactor;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 20.02.15.
 */
public final class BasicNIOConnPool
extends AbstractNIOConnPool<HttpHost, NHttpClientConnection, BasicNIOConnPoolEntry> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static AtomicLong COUNTER = new AtomicLong(0);
	//
	private final static class BasicAddrResolver
	implements SocketAddressResolver<HttpHost> {
		@Override
		public SocketAddress resolveLocalAddress(final HttpHost host) {
			return null;
		}
		//
		@Override
		public SocketAddress resolveRemoteAddress(final HttpHost host) {
			final String hostname = host.getHostName();
			int port = host.getPort();
			if(port < 0) {
				if (host.getSchemeName().equalsIgnoreCase("http")) {
					port = 80;
				} else if (host.getSchemeName().equalsIgnoreCase("https")) {
					port = 443;
				}
			}
			return new InetSocketAddress(hostname, port);
		}
	}
	//
	private final int connPoolTimeOut;
	//
	public BasicNIOConnPool(
		final ConnectingIOReactor ioReactor,
		final NIOConnFactory<HttpHost, NHttpClientConnection> connFactory,
		final int connPoolTimeOut, final int poolSize
	) {
		super(ioReactor, connFactory, new BasicAddrResolver(), poolSize, poolSize);
		this.connPoolTimeOut = connPoolTimeOut;
	}
	//
	@Override
	protected final BasicNIOConnPoolEntry createEntry(
		final HttpHost host, final NHttpClientConnection conn
	) {
		return new BasicNIOConnPoolEntry(
			Long.toString(COUNTER.getAndIncrement()), host, conn
		);
	}
	//
	@Override
	public Future<BasicNIOConnPoolEntry> lease(
		final HttpHost route, final Object state,
		final FutureCallback<BasicNIOConnPoolEntry> callback
	) {
		return super.lease(route, state, connPoolTimeOut, TimeUnit.MILLISECONDS, callback);
	}
	//
	@Override
	public final Future<BasicNIOConnPoolEntry> lease(
		final HttpHost route, final Object state
	) {
		return super.lease(
			route, state, connPoolTimeOut, TimeUnit.MILLISECONDS, null
		);
	}
	//
	@Override
	protected final void onLease(final BasicNIOConnPoolEntry entry) {
		final NHttpClientConnection conn = entry.getConnection();
		conn.setSocketTimeout(entry.getSocketTimeout());
	}

	@Override
	protected final void onRelease(final BasicNIOConnPoolEntry entry) {
		final NHttpClientConnection conn = entry.getConnection();
		entry.setSocketTimeout(conn.getSocketTimeout());
		conn.setSocketTimeout(0);
	}

}
