package com.emc.mongoose.web.load.impl.tasks;
//
import com.emc.mongoose.util.logging.TraceLogger;
//
import org.apache.http.impl.nio.reactor.BaseIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 16.02.15.
 */
public final class IODispatchTask
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final BaseIOReactor dispatcher;
	private final IOEventDispatch ioEventDispatch;
	//
	public IODispatchTask(final BaseIOReactor dispatcher, final IOEventDispatch ioEventDispatch) {
		this.dispatcher = dispatcher;
		this.ioEventDispatch = ioEventDispatch;
	}
	//
	@Override
	public final void run() {
		try {
			dispatcher.execute(ioEventDispatch);
		} catch(final Exception e) {
			TraceLogger.failure(LOG, Level.ERROR, e, "I/O dispatch worker failure");
		}
	}
}
