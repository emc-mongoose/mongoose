package com.emc.mongoose.base.load.impl.tasks;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
Created by kurila on 23.10.14.
Register shutdown hook which should perform correct server-side shutdown even if user hits ^C
*/
public final class LoadCloseHook
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static Map<LoadExecutor, Thread> HOOKS_MAP = new ConcurrentHashMap<>();
	//
	private final LoadExecutor loadExecutor;
	private final String loadName;
	//
	private LoadCloseHook(final LoadExecutor loadExecutor) {
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
		this.loadExecutor = loadExecutor;
	}
	//
	public static void add(final LoadExecutor loadExecutor) {
		//
		final LoadCloseHook hookTask = new LoadCloseHook(loadExecutor);
		try {
			final Thread hookThread = new Thread(
				hookTask, String.format("shutDownHook<%s>", hookTask.loadName)
			);
			Runtime.getRuntime().addShutdownHook(hookThread);
			HOOKS_MAP.put(loadExecutor, hookThread);
			LOG.debug(
				Markers.MSG, "Registered shutdown hook \"{}\"", hookTask.loadName
			);
		} catch(final SecurityException | IllegalArgumentException | IllegalStateException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to add the shutdown hook");
		}
	}
	//
	public static void del(final LoadExecutor loadExecutor) {
		if(LoadCloseHook.class.isInstance(Thread.currentThread())) {
			LOG.debug(Markers.MSG, "Won't remove the shutdown hook which is in progress");
		} else if(HOOKS_MAP.containsKey(loadExecutor)) {
			try {
				Runtime.getRuntime().removeShutdownHook(HOOKS_MAP.get(loadExecutor));
				LOG.debug(Markers.MSG, "Shutdown hook for \"{}\" removed", loadExecutor);
			} catch(final SecurityException | IllegalArgumentException | IllegalStateException e) {
				ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to remove the shutdown hook");
			}
		} else {
			LOG.trace(Markers.ERR, "No shutdown hook registered for \"{}\"", loadExecutor);
		}
	}
	//
	@Override
	public final void run() {
		LOG.info(Markers.MSG, "Closing the load executor \"{}\"...", loadName);
		try {
			loadExecutor.close();
			LOG.info(Markers.MSG, "The load executor \"{}\"closed successfully", loadName);
		} catch(final Exception e) {
			ExceptionHandler.trace(
				LOG, Level.WARN, e,
				String.format("Failed to close the load executor \"%s\"", loadName)
			);
		}
	}
}
