package com.emc.mongoose.util.io.http;
//
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
//
import org.apache.http.HttpHost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.ConnSupport;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.impl.nio.pool.BasicNIOConnFactory;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.reactor.IOSession;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by kurila on 20.02.15.
 */
public final class BasicNIOClientConnection
extends DefaultNHttpClientConnection
implements NHttpClientConnection {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private BasicNIOClientConnection(final IOSession ioSession, final ConnectionConfig connConfig) {
		super(
			ioSession, connConfig.getBufferSize(), connConfig.getFragmentSizeHint(), null,
			ConnSupport.createDecoder(connConfig), ConnSupport.createEncoder(connConfig),
			connConfig.getMessageConstraints(), null, null, null, null
		);
	}
	/*
	@Override
	public final boolean isOpen() {
		return getStatus() == ACTIVE;
	}*/
	//
	@Override
	public final void close()
	throws IOException {
		TraceLogger.trace(
			LOG, Level.TRACE, Markers.MSG,
			String.format("Connection \"%s\" close", toString())
		);
		super.close();
	}
	//
	public final static class Factory
	extends BasicNIOConnFactory {
		//
		public final AtomicInteger countConn = new AtomicInteger(0);
		//
		private ConnectionConfig connConfig;
		//
		public Factory(final ConnectionConfig connConfig) {
			super(connConfig);
			this.connConfig = connConfig;
		}
		//
		@Override
		public final NHttpClientConnection create(
			final HttpHost route, final IOSession ioSession
		) throws IOException {
			final NHttpClientConnection conn = new BasicNIOClientConnection(ioSession, connConfig);
			final int connNum = countConn.incrementAndGet();
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Created connection #{}, target: {}", connNum, route);
			}
			return conn;
		}
	}
	//
}
