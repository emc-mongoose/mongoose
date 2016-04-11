package com.emc.mongoose.core.impl.load.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.generator.LoadState;
//
import com.emc.mongoose.core.impl.load.generator.BasicLoadState;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
Created by kurila on 23.10.14.
Register shutdown hook which should perform correct server-side shutdown even if user hits ^C
*/
public final class LoadCloseHook
implements Runnable {
	//
	private static final Logger LOG = LogManager.getLogger();
	private static final Map<String, Map<LoadExecutor, Thread>>
		HOOKS_MAP = new HashMap<>();
	//
	public static final Map<String, List<LoadState>>
		LOAD_STATES_MAP = new HashMap<>();
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
			final String currRunId = BasicConfig.THREAD_CONTEXT.get().getRunId();
			synchronized(HOOKS_MAP) {
				Map<LoadExecutor, Thread> runLoadHooks = HOOKS_MAP.get(currRunId);
				if(runLoadHooks == null) {
					runLoadHooks = new HashMap<>();
					HOOKS_MAP.put(currRunId, runLoadHooks);
				}
				runLoadHooks.put(loadExecutor, hookThread);
			}
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
		String currRunId;
		try {
			currRunId = loadExecutor.getLoadState().getAppConfig().getRunId();
		} catch (final RemoteException e) {
			currRunId = BasicConfig.THREAD_CONTEXT.get().getRunId();
			LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
		}
		synchronized(HOOKS_MAP) {
			final Map<LoadExecutor, Thread> runHooks = HOOKS_MAP.get(currRunId);
			if(runHooks != null && runHooks.containsKey(loadExecutor)) {
				final Thread loadCloseHookThread = runHooks.get(loadExecutor);
				try {
					Runtime.getRuntime().removeShutdownHook(loadCloseHookThread);
					LOG.debug(Markers.MSG, "Shutdown hook for \"{}\" removed", loadExecutor);
				} catch(final IllegalStateException e) {
					LogUtil.exception(LOG, Level.TRACE, e, "Failed to remove the shutdown hook");
				} catch(final SecurityException | IllegalArgumentException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to remove the shutdown hook");
				} finally {
					runHooks.remove(loadExecutor);
					if(LogUtil.LOAD_HOOKS_COUNT.get() > 1) {
						LogUtil.LOAD_HOOKS_COUNT.decrementAndGet();
					}
					//
					try {
						final LoadState currState = loadExecutor.getLoadState();
						List<LoadState> runLoadStates;
						synchronized(LOAD_STATES_MAP) {
							runLoadStates = LOAD_STATES_MAP.get(currRunId);
							if(runLoadStates == null) {
								runLoadStates = new LinkedList<>();
								LOAD_STATES_MAP.put(currRunId, runLoadStates);
							}
							runLoadStates.add(currState);
						}
						//
						if(runHooks.isEmpty()) {
							final AppConfig appConfig = currState.getAppConfig();
							if(!BasicLoadState.isRunFinished(appConfig, runLoadStates)) {
								if(Constants.RUN_MODE_STANDALONE.equals(appConfig.getRunMode())) {
									final int loadJobCount = runLoadStates.size();
									if(loadJobCount == 1) {
										BasicLoadState.saveRunState(currRunId, runLoadStates);
										synchronized(LOAD_STATES_MAP) {
											LOAD_STATES_MAP.remove(currRunId);
										}
									} else {
										LOG.debug(
											Markers.MSG,
											"This is not single load job run ({}), will not save the states",
											loadJobCount
										);
									}
								} else {
									LOG.debug(
										Markers.MSG,
										"This is not standalone mode (but \"{}\"), will not save the states",
										appConfig.getRunMode().toLowerCase()
									);
								}
							} else {
								LOG.debug(
									Markers.MSG,
									"This run is finished already, will not save its state"
								);
							}
							HOOKS_MAP.remove(currRunId);
							if(HOOKS_MAP.isEmpty()) {
								try {
									if(LogUtil.HOOKS_LOCK.tryLock(10, TimeUnit.SECONDS)) {
										try {
											LogUtil.LOAD_HOOKS_COUNT.decrementAndGet();
											LogUtil.HOOKS_COND.signalAll();
										} finally {
											LogUtil.HOOKS_LOCK.unlock();
										}
									} else {
										LOG.debug(
											Markers.ERR,
											"Failed to acquire the lock for the del method"
										);
									}
								} catch(final InterruptedException e) {
									LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted");
								}
							}
						}
					} catch(final Throwable e) {
						e.printStackTrace(System.out);
						LogUtil.exception(LOG, Level.WARN, e, "Failed to remove the shutdown hook");
					}
				}
			} else {
				LOG.trace(Markers.ERR, "No shutdown hook registered for \"{}\"", loadExecutor);
			}
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
	//
}
