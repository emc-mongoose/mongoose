package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.metrics.MetricsContext;
import com.emc.mongoose.api.metrics.MetricsManager;
import com.emc.mongoose.api.metrics.MetricsSnapshot;
import com.emc.mongoose.api.model.concurrent.DaemonBase;
import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;
import com.emc.mongoose.api.model.load.LoadController;
import com.emc.mongoose.api.model.load.LoadGenerator;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class LoadStepBase
extends DaemonBase
implements LoadStep, Runnable {

	protected final Config baseConfig;
	protected final List<Map<String, Object>> stepConfigs;
	protected final List<MetricsContext> metricsContexts = new ArrayList<>();
	protected final List<LoadGenerator> generators = new ArrayList<>();
	protected final List<StorageDriver> drivers = new ArrayList<>();
	protected final List<LoadController> controllers = new ArrayList<>();

	protected boolean distributedFlag = false;
	protected volatile LoadStepClient stepClient = null;

	private volatile Config actualConfig = null;
	private volatile long timeLimitSec = Long.MAX_VALUE;
	private volatile long startTimeSec = -1;
	private String id = null;

	protected LoadStepBase(final Config baseConfig, final List<Map<String, Object>> stepConfigs) {
		this.baseConfig = baseConfig;
		this.stepConfigs = stepConfigs;
	}

	@Override
	public LoadStepBase config(final Map<String, Object> config) {
		final var stepConfigsCopy = new ArrayList<Map<String, Object>>();
		if(stepConfigs != null) {
			stepConfigsCopy.addAll(stepConfigs);
		}
		stepConfigsCopy.add(config);
		return copyInstance(stepConfigsCopy);
	}

	@Override
	public final String id() {
		return id;
	}

	@Override @SuppressWarnings("deprecation")
	public final List<MetricsSnapshot> metricsSnapshots() {
		return metricsContexts
			.stream()
			.map(MetricsContext::lastSnapshot)
			.collect(Collectors.toList());
	}

	protected abstract LoadStepBase copyInstance(final List<Map<String, Object>> stepConfigs);

	protected final void actualConfig(final Config actualConfig) {
		this.actualConfig = actualConfig;
		final StepConfig stepConfig = actualConfig.getTestConfig().getStepConfig();
		this.id = stepConfig.getId();
		this.distributedFlag = stepConfig.getDistributed();
	}


	@Override
	public final void run() {
		try {
			start();
			try {
				await(timeLimitSec, TimeUnit.SECONDS);
			} catch(final IllegalStateException e) {
				LogUtil.exception(Level.WARN, e, "Failed to await \"{}\"", toString());
			} catch(final InterruptedException e) {
				throw new CancellationException();
			}
		} catch(final IllegalStateException e) {
			LogUtil.exception(Level.WARN, e, "Failed to start \"{}\"", toString());
		} catch(final Throwable cause) {
			cause.printStackTrace();
		} finally {
			try {
				close();
			} catch(final Exception e) {
				LogUtil.exception(Level.WARN, e, "Failed to close \"{}\"", toString());
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void doStart()
	throws IllegalStateException {

		init();

		final var stepConfig = actualConfig.getTestConfig().getStepConfig();
		final var stepId = stepConfig.getId();
		try(
			final var logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			if(distributedFlag) {
				doStartRemote(actualConfig);
			} else {
				doStartLocal(actualConfig);
			}

			final var t = stepConfig.getLimitConfig().getTime();
			if(t > 0) {
				timeLimitSec = t;
			}
			startTimeSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		} catch(final Throwable cause) {
			LogUtil.exception(Level.WARN, cause, "{} step failed to start", id);
		}

		metricsContexts.forEach(
			metricsCtx -> {
				metricsCtx.start();
				try {
					MetricsManager.register(id, metricsCtx);
				} catch(final InterruptedException e) {
					throw new CancellationException(e.getMessage());
				}
			}
		);
	}

	/**
	 * Initializes the actual configuration and metrics contexts
	 */
	protected abstract void init();

	private void doStartRemote(final Config actualConfig)
	throws RemoteException {
		stepClient = new BasicLoadStepClient(this, actualConfig);
		stepClient.start();
	}

	protected abstract void doStartLocal(final Config actualConfig);

	protected void doShutdown() {
		if(stepClient == null) {
			shutdownLocal();
		} else {
			try {
				stepClient.shutdown();
			} catch(final RemoteException ignored) {
			}
		}
	}

	private void shutdownLocal() {

		generators.forEach(
			generator -> {
				try(
					final var ctx = CloseableThreadContext
						.put(KEY_TEST_STEP_ID, id)
						.put(KEY_CLASS_NAME, getClass().getSimpleName())
				) {
					generator.stop();
					Loggers.MSG.debug(
						"{}: load generator \"{}\" interrupted", id,
						generator.toString()
					);
				} catch(final RemoteException ignored) {
				}
			}
		);

		drivers.forEach(
			driver -> {
				try(
					final var ctx = CloseableThreadContext
						.put(KEY_TEST_STEP_ID, id)
						.put(KEY_CLASS_NAME, getClass().getSimpleName())
				) {
					driver.shutdown();
					Loggers.MSG.debug(
						"{}: next storage driver {} shutdown", id, driver.toString()
					);
				} catch(final RemoteException ignored) {
				}
			}
		);
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		if(stepClient == null) {
			return awaitLocal(timeout, timeUnit);
		} else {
			try {
				return stepClient.await(timeout, timeUnit);
			} catch(final RemoteException e) {
				LogUtil.exception(Level.WARN, e, "Connectivity failure");
				return false;
			}
		}
	}

	protected boolean awaitLocal(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		final var awaitExecutor = Executors.newFixedThreadPool(
			controllers.size(), new LogContextThreadFactory(id)
		);
		final var latch = new CountDownLatch(controllers.size());
		controllers
			.forEach(
				controller -> {
					awaitExecutor.submit(
						() -> {
							try {
								if(controller.await(timeout, timeUnit)) {
									latch.countDown();
								}
							} catch(final InterruptedException e) {
								throw new CancellationException();
							} catch(final RemoteException ignored) {
							}
						}
					);
				}
			);
		awaitExecutor.shutdown();
		try {
			awaitExecutor.awaitTermination(timeout, timeUnit);
		} finally {
			awaitExecutor.shutdownNow();
		}
		return 0 == latch.getCount();
	}

	@Override
	protected void doStop() {

		if(stepClient != null) {
			try {
				stepClient.stop();
			} catch(final Exception e) {
				LogUtil.exception(Level.WARN, e, "{} step failed to stop", id);
			}
		}

		metricsContexts
			.forEach(
				metricsCtx -> {
					try {
						MetricsManager.unregister(id, metricsCtx);
					} catch(final InterruptedException e) {
						throw new CancellationException(e.getMessage());
					}
				}
			);

		if(stepClient == null) {
			doStopLocal();
		}

		final long t = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - startTimeSec;
		if(t < 0) {
			Loggers.ERR.warn(
				"Stopped earlier than started, won't account the elapsed time"
			);
		} else if(t > timeLimitSec) {
			Loggers.MSG.warn(
				"The elapsed time ({}[s]) is more than the limit ({}[s]), won't resume",
				t, timeLimitSec
			);
			timeLimitSec = 0;
		} else {
			timeLimitSec -= t;
		}
	}

	protected void doStopLocal() {
		drivers.forEach(
			driver -> {
				try {
					driver.stop();
				} catch(final RemoteException ignored) {
				}
				Loggers.MSG.debug("{}: next storage driver {} stopped", id, driver.toString());
			}
		);
		controllers.forEach(
			controller -> {
				try {
					controller.stop();
				} catch(final RemoteException ignored) {
				}
			}
		);
	}

	@Override
	protected void doClose()
	throws IOException {

		metricsContexts
			.forEach(
				metricsCtx -> {
					try {
						metricsCtx.close();
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "Failed to close the metrics context \"{}\"", metricsCtx
						);
					}
				}
			);

		if(stepClient == null) {
			doCloseLocal();
		} else {
			stepClient.close();
		}
	}

	protected void doCloseLocal() {

		drivers.forEach(
			driver -> {
				try {
					driver.close();
				} catch(final IOException e) {
					LogUtil.exception(
						Level.ERROR, e, "Failed to close the storage driver \"{}\"",
						driver.toString()
					);
				}
			}
		);
		drivers.clear();

		generators.forEach(
			generator -> {
				try {
					generator.close();
				} catch(final IOException e) {
					LogUtil.exception(
						Level.ERROR, e, "Failed to close the load generator \"{}\"",
						generator.toString()
					);
				}
			}
		);
		generators.clear();

		controllers.forEach(
			controller -> {
				try {
					controller.close();
				} catch(final IOException e) {
					LogUtil.exception(
						Level.ERROR, e, "Failed to close the load controller \"{}\"",
						controller.toString()
					);
				}
			}
		);
		controllers.clear();
	}
}
