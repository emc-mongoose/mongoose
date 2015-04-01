package com.emc.mongoose.storage.mock.impl.cinderella;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
//
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.reactor.IOSession;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Created by olga on 04.02.15.
 */
public final class FaultingConnectionFactory
extends DefaultNHttpServerConnectionFactory {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final AtomicInteger counter = new AtomicInteger(0);
	private final ExecutorService
		connectionPool = Executors.newFixedThreadPool(100000, new NamingWorkerFactory("connKiller"));
	private final static int FAULT_SLEEP_MILLI_SEC = RunTimeConfig.getContext().getStorageMockFaultSleepMilliSec();
	private final static int FAULT_PERIOD = RunTimeConfig.getContext().getStorageMockFaultPeriod();
	//
	public FaultingConnectionFactory(final ConnectionConfig config) {
		super(config);
	}
	//
	@Override
	public final DefaultNHttpServerConnection createConnection(final IOSession session) {
		final DefaultNHttpServerConnection connection = super.createConnection(session);
		if (FAULT_PERIOD > 0 && (counter.incrementAndGet() % FAULT_PERIOD) == 0 ){
			LOG.trace(
				LogUtil.MSG, "The connection {} is submitted to be possibly broken", connection
			);
			connectionPool.submit(new FailConnectionTask(connection));
		}
		return connection;
	}
	///////////////////////////////////////////////////////////////////////////////
	private final static class FailConnectionTask
	implements Runnable {
		//
		private final NHttpServerConnection connection;
		//
		public FailConnectionTask(final NHttpServerConnection connection) {
			this.connection = connection;
		}
		//
		@Override
		public final void run() {
			try {
				Thread.sleep(FAULT_SLEEP_MILLI_SEC);
				if(connection.isOpen()) {
					connection.close();
					LOG.trace(LogUtil.MSG, "The connection {} is closed", connection);
				} else {
					LOG.trace(LogUtil.MSG, "The connection {} is already closed", connection);
				}
			} catch (final IOException e) {
				LogUtil.failure(LOG, Level.ERROR, e, "Failed to fail the connection");
			} catch (final InterruptedException e) {
				LogUtil.failure(LOG, Level.DEBUG, e, "Interrupted");
			}
		}
	}
}
