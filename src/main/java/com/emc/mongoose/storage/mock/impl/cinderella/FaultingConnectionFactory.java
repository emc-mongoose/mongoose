package com.emc.mongoose.storage.mock.impl.cinderella;
//
import com.emc.mongoose.core.impl.util.RunTimeConfig;
import com.emc.mongoose.core.api.util.log.Markers;
import com.emc.mongoose.core.impl.util.log.TraceLogger;
import com.emc.mongoose.core.impl.util.WorkerFactory;
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
		connectionPool = Executors.newFixedThreadPool(100000, new WorkerFactory("connKiller"));
	private final int faultSleepMsec;
	private final int faultPeriod;
	//
	public FaultingConnectionFactory(
		final ConnectionConfig config, final RunTimeConfig runTimeConfig
	) {
		super(config);
		this.faultSleepMsec =  runTimeConfig.getStorageMockFaultSleepMilliSec();
		faultPeriod = runTimeConfig.getStorageMockFaultPeriod();
	}
	//
	@Override
	public final DefaultNHttpServerConnection createConnection(final IOSession session) {
		final DefaultNHttpServerConnection connection = super.createConnection(session);
		if (faultPeriod > 0 && (counter.incrementAndGet() % faultPeriod) == 0 ){
			LOG.trace(
				Markers.MSG, "The connection {} is submitted to be possibly broken", connection
			);
			connectionPool.submit(new FailConnectionTask(connection, faultSleepMsec));
		}
		return connection;
	}
	///////////////////////////////////////////////////////////////////////////////
	private final static class FailConnectionTask
	implements Runnable {
		//
		private final NHttpServerConnection connection;
		private final int faultSleepMsec;
		//
		public FailConnectionTask(
			final NHttpServerConnection connection, final int faultSleepMsec
		) {
			this.connection = connection;
			this.faultSleepMsec = faultSleepMsec;
		}
		//
		@Override
		public final void run() {
			try {
				Thread.sleep(faultSleepMsec);
				if(connection.isOpen()) {
					connection.close();
					LOG.trace(Markers.MSG, "The connection {} is closed", connection);
				} else {
					LOG.trace(Markers.MSG, "The connection {} is already closed", connection);
				}
			} catch (final IOException e) {
				TraceLogger.failure(LOG, Level.ERROR, e, "Failed to fail the connection");
			} catch (final InterruptedException e) {
				TraceLogger.failure(LOG, Level.DEBUG, e, "Interrupted");
			}
		}
	}
}
