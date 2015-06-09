package com.emc.mongoose.core.impl.load.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
			LogUtil.exception(
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
				hookTask, String.format("loadCloseHook<%s>", hookTask.loadName)
			);
			Runtime.getRuntime().addShutdownHook(hookThread);
			HOOKS_MAP.put(loadExecutor, hookThread);
			LogUtil.LOAD_HOOKS_COUNT.incrementAndGet();
			LOG.debug(
				Markers.MSG, "Registered shutdown hook \"{}\"", hookTask.loadName
			);
		} catch(final SecurityException | IllegalArgumentException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to add the shutdown hook");
		} catch(final IllegalStateException e) { // shutdown is in progress
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to add the shutdown hook");
		}
	}
	//
	public static void del(final LoadExecutor loadExecutor) {
		if (LoadCloseHook.class.isInstance(Thread.currentThread())) {
			LOG.debug(Markers.MSG, "Won't remove the shutdown hook which is in progress");
		} else if (HOOKS_MAP.containsKey(loadExecutor)) {
			try {
				Runtime.getRuntime().removeShutdownHook(HOOKS_MAP.get(loadExecutor));
				LOG.debug(Markers.MSG, "Shutdown hook for \"{}\" removed", loadExecutor);
			} catch (final IllegalStateException e) {
				LogUtil.exception(LOG, Level.TRACE, e, "Failed to remove the shutdown hook");
			} catch (final SecurityException | IllegalArgumentException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to remove the shutdown hook");
			} finally {
				HOOKS_MAP.remove(loadExecutor);
				LogUtil.LOAD_HOOKS_COUNT.decrementAndGet();
				if (HOOKS_MAP.isEmpty()) {
					try {
						if (LogUtil.HOOKS_LOCK.tryLock(10, TimeUnit.SECONDS)) {
							try {
								LogUtil.HOOKS_COND.signalAll();
							} finally {
								LogUtil.HOOKS_LOCK.unlock();
							}
						} else {
							LOG.debug(Markers.ERR, "Failed to acquire the lock for the del method");
						}
					} catch (final InterruptedException e) {
						LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted");
					}
				}
			}
		} else {
			LOG.trace(Markers.ERR, "No shutdown hook registered for \"{}\"", loadExecutor);
		}
	}
	//
	@Override
	public final void run() {
		LOG.debug(Markers.MSG, "Closing the load executor \"{}\"...", loadName);
		try {
			loadExecutor.close();
			LOG.debug(Markers.MSG, "The load executor \"{}\" closed successfully", loadName);
		} catch(final Exception e) {
			LogUtil.exception(
				LOG, Level.DEBUG, e, "Failed to close the load executor \"{}\"", loadName
			);
		}
	}
}
