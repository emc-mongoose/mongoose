package com.emc.mongoose.base.load.impl;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
/**
Created by kurila on 23.10.14.
Register shutdown hook which should perform correct server-side shutdown even if user hits ^C
*/
public final class ShutDownHook {
	//
	private final static Logger LOG = LogManager.getLogger();
	private ShutDownHook() {}
	//
	private static void failWithCauseAndForce(final String cause) {
		LOG.warn(
			Markers.ERR,
			"Failed to add the shutdown hoot due to {}, forcing the shutdown", cause
		);
		System.exit(0);
	}
	//
	public static void add(final LoadExecutor loadExecutor) {
		//
		final String loadName;
		String ln = "";
		try {
			ln = loadExecutor.getName();
		} catch(final RemoteException e) {
			ExceptionHandler.trace(
				LOG, Level.WARN, e, "Failed to get the name of the remote load executor"
			);
		} finally {
			loadName = ln;
		}
		//
		try {
			Runtime.getRuntime().addShutdownHook(
				new Thread(String.format("shutDownHook<%s>", loadName)) {
					private final Logger log = LogManager.getLogger();
					@Override
					public final void run() {
						log.info(Markers.MSG, "Closing the load executor \"{}\"...", loadName);
						try {
							loadExecutor.close();
							log.info(
								Markers.MSG, "The load executor \"{}\"closed successfully", loadName
							);
						} catch(final Exception e) {
							ExceptionHandler.trace(
								LOG, Level.WARN, e,
								String.format("Failed to close the load executor \"%s\"", loadName)
							);
						}
					}
				}
			);
			LOG.debug(
				Markers.MSG, "Registered shutdown hook for the load executor \"{}\"", loadName
			);
		} catch(final IllegalArgumentException | IllegalStateException e) {
			failWithCauseAndForce("run-time state");
		} catch(final SecurityException e) {
			failWithCauseAndForce("security policy");
		}
	}
}
