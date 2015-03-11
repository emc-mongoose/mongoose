package com.emc.mongoose.core.impl.load.tasks;
//
import com.emc.mongoose.core.api.persist.Markers;
import com.emc.mongoose.core.impl.persist.TraceLogger;
import com.emc.mongoose.core.api.data.WSObject;
//
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InterruptedIOException;
/**
Created by kurila on 30.01.15.
*/
public final class HttpClientRunTask<T extends WSObject>
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final IOEventDispatch ioEventDispatch;
	private final ConnectingIOReactor ioReactor;
	//
	public HttpClientRunTask(
		final IOEventDispatch ioEventDispatch, final ConnectingIOReactor ioReactor
	) {
		this.ioEventDispatch = ioEventDispatch;
		this.ioReactor = ioReactor;
	}
	//
	@Override
	public final void run() {
		LOG.debug(Markers.MSG, "Running the web storage client");
		try {
			ioReactor.execute(ioEventDispatch);
		} catch(final IOReactorException e) {
			TraceLogger.failure(
				LOG, Level.ERROR, e,
				"Possible max open files limit exceeded, please check the environment configuration"
			);
		} catch(final InterruptedIOException e) {
			LOG.debug(Markers.MSG, "Interrupted");
		} catch(final IOException e) {
			TraceLogger.failure(
				LOG, Level.ERROR, e, "Failed to execute the web storage client"
			);
		} catch(final IllegalStateException e) {
			TraceLogger.failure(LOG, Level.DEBUG, e, "Looks like I/O reactor shutdown");
		} finally {
			try {
				ioReactor.shutdown();
			} catch(final IOException e) {
				TraceLogger.failure(LOG, Level.DEBUG, e, "I/O reactor shutdown failure");
			}
		}
	}
}
