package com.emc.mongoose.web.mock;
//
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
//
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
//
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
	public FaultingConnectionFactory(final ConnectionConfig config, final RunTimeConfig runTimeConfig){
		super(config);
		this.faultSleepMsec =  runTimeConfig.getInt("storage.mock.fault.sleep.msec");
		faultPeriod = runTimeConfig.getInt("storage.mock.fault.period");
	}
	//
	@Override
	public final DefaultNHttpServerConnection createConnection(final IOSession session) {
		final DefaultNHttpServerConnection connection = super.createConnection(session);
		if (faultPeriod > 0 && (counter.incrementAndGet() % faultPeriod) == 0 ){
			LOG.trace(Markers.MSG, "The connection {} is submitted to be possibly broken", connection);
			connectionPool.submit(new ConnectionFaultWorker(connection, faultSleepMsec));
		}
		return connection;
	}
	///////////////////////////////////////////////////////////////////////////////
	//ConnectionFaultWorker
	///////////////////////////////////////////////////////////////////////////////
	private final static class ConnectionFaultWorker
	implements Runnable{
		//
		private final DefaultNHttpServerConnection connection;
		private final int faultSleepMsec;
		//
		public ConnectionFaultWorker(final DefaultNHttpServerConnection connection, final int faultSleepMsec){
			this.connection = connection;
			this.faultSleepMsec = faultSleepMsec;
		}
		//
		@Override
		public final void run() {
			try {
				Thread.sleep(faultSleepMsec);
				if (connection.isOpen()){
					connection.close();
					LOG.trace(Markers.MSG, "The connection {} is closed", connection);
				} else {
					LOG.trace(Markers.MSG, "The connection {} is already closed", connection);
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
