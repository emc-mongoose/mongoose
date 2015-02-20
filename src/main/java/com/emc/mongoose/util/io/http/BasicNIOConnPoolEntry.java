package com.emc.mongoose.util.io.http;
//
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
//
import org.apache.http.HttpHost;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.pool.PoolEntry;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
/**
 Created by kurila on 20.02.15.
 */
public final class BasicNIOConnPoolEntry
extends PoolEntry<HttpHost, NHttpClientConnection> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile int socketTimeout;
	//
	public BasicNIOConnPoolEntry(
		final String id, final HttpHost route, final NHttpClientConnection conn
	) {
		super(id, route, conn);
		socketTimeout = conn.getSocketTimeout();
	}
	//
	@Override
	public final void close() {
		try {
			getConnection().close();
			TraceLogger.trace(
				LOG, Level.TRACE, Markers.MSG,
				String.format("Connection \"%s\" closed by closing the pool entry", getConnection())
			);
		} catch (final IOException e) {
			TraceLogger.failure(LOG, Level.DEBUG, e, "Failed to close the connection");
		}
	}
	//
	@Override
	public final boolean isClosed() {
		return !getConnection().isOpen();
	}
	//
	public final int getSocketTimeout() {
		return socketTimeout;
	}
	//
	public final void setSocketTimeout(final int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
}
