package com.emc.mongoose.client.impl.load.executor.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.LoadSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
/**
 Created by kurila on 19.12.14.
 */
public final class InterruptSvcTask
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadSvc loadSvc;
	private final String addr;
	//
	public InterruptSvcTask(final LoadSvc loadSvc, final String addr) {
		this.loadSvc = loadSvc;
		this.addr = addr;
	}
	//
	@Override
	public final void run() {
		try {
			loadSvc.interrupt();
			LOG.trace(LogUtil.MSG, "Interrupted remote service @ {}", addr);
		} catch(final IOException e) {
			LogUtil.failure(
				LOG, Level.DEBUG, e,
				String.format(
					"Failed to interrupt remote load service @ %s", addr
				)
			);
		}
	}
}
