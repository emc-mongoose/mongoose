package com.emc.mongoose.core.impl.load.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.logging.Markers;
//
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InterruptedIOException;
/**
Created by kurila on 30.01.15.
*/
public final class HttpClientRunTask
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
		LOG.debug(
			Markers.MSG, "Running the web storage client {}", Thread.currentThread().getName()
		);
		try {
			ioReactor.execute(ioEventDispatch);
		} catch(final IOReactorException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e,
				"Possible max open files limit exceeded, please check the environment configuration"
			);
		} catch(final InterruptedIOException e) {
			LOG.debug(Markers.MSG, "{}: interrupted", Thread.currentThread().getName());
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to execute the web storage client"
			);
		} catch(final IllegalStateException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Looks like I/O reactor shutdown");
		}
	}
}
