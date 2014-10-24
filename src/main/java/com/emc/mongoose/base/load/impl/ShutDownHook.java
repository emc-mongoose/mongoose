package com.emc.mongoose.base.load.impl;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.logging.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
/**
Created by kurila on 23.10.14.
Register shutdown hook which should perform correct server-side shutdown even if user hits ^C
*/
public final class ShutDownHook {
	//
	private final static Logger LOG = LogManager.getLogger();
	private ShutDownHook() {}
	//
	public static void add(final LoadExecutor loadExecutor) {
		Runtime.getRuntime().addShutdownHook(
			new Thread() {
				@Override
				public final void run() {
					try {
						LOG.info(
							Markers.MSG, "Closing the load executor \"{}\"...",
							loadExecutor.getName()
						);
					} catch(final IOException e) {
						e.printStackTrace();
					}
					try {
						loadExecutor.close();
						LOG.debug(Markers.MSG, "Finished successfully");
					} catch(final IOException e) {
						e.printStackTrace();
					}
				}
			}
		);
		LOG.trace(Markers.MSG, "Registered shutdown hook");
	}
}
