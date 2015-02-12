package com.emc.mongoose.web.mock;

import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.threading.WorkerFactory;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.nio.reactor.IOSession;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by olga on 04.02.15.
 */
public class CinderellaConnectionFactory
extends DefaultNHttpServerConnectionFactory {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final AtomicInteger counter = new AtomicInteger(0);
	private final ExecutorService connectionPool = Executors.newFixedThreadPool(100000,
		new WorkerFactory("connection-fault"));
	private final int faultSleepMsec;
	private final int faultPeriod;
	//
	public CinderellaConnectionFactory(final ConnectionConfig config, final RunTimeConfig runTimeConfig){
		super(config);
		this.faultSleepMsec =  runTimeConfig.getInt("storage.mock.fault.sleep.msec");
		faultPeriod = runTimeConfig.getInt("storage.mock.fault.period");

	}

	@Override
	public final DefaultNHttpServerConnection createConnection(final IOSession session) {
		DefaultNHttpServerConnection connection = super.createConnection(session);
		if (faultPeriod > 0 && (counter.incrementAndGet() % faultPeriod) == 0 ){
			LOG.info(Markers.MSG, "Connection ready to fault!");
			connectionPool.submit(new ConnectionFaultWorker(connection));
		}
		return connection;
	}
	///////////////////////////////////////////////////////////////////////////////
	//ConnectionFaultWorker
	///////////////////////////////////////////////////////////////////////////////
	class ConnectionFaultWorker
	implements Runnable{
		//
		private final DefaultNHttpServerConnection connection;
		//
		public ConnectionFaultWorker(final DefaultNHttpServerConnection connection){
			this.connection = connection;
		}
		//
		@Override
		public final void run() {
			try {
				Thread.sleep(faultSleepMsec);
				if (connection.isOpen()){
					connection.close();
					LOG.info(Markers.MSG, " Connection close");
				} else {
					LOG.info(Markers.MSG, " Connection already close");
				}
			}catch (final IOException e) {
				TraceLogger.failure(LOG, Level.ERROR, e, "Fault thread");
				e.printStackTrace();
			} catch (final InterruptedException e) {
				TraceLogger.failure(LOG, Level.ERROR, e, "Interrupted");
			}
		}
	}
}
