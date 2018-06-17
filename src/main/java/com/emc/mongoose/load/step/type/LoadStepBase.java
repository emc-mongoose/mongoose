package com.emc.mongoose.load.step.type;

import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;
import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.load.generator.LoadGenerator;
import com.emc.mongoose.metrics.AggregatingMetricsContext;
import com.emc.mongoose.metrics.MetricsContextImpl;
import com.emc.mongoose.metrics.MetricsContext;
import com.emc.mongoose.metrics.MetricsManager;
import com.emc.mongoose.metrics.MetricsSnapshot;
import com.emc.mongoose.concurrent.DaemonBase;
import com.emc.mongoose.logging.LogContextThreadFactory;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.storage.driver.StorageDriver;
import com.emc.mongoose.load.step.LoadStep;
import com.emc.mongoose.load.step.client.LoadStepClientImpl;
import com.emc.mongoose.load.step.client.LoadStepClient;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class LoadStepBase
extends DaemonBase
implements LoadStep, Runnable {

	protected final Config baseConfig;
	protected final List<Extension> extensions;
	protected final List<Map<String, Object>> stepConfigs;
	protected final List<MetricsContext> metricsContexts = new ArrayList<>();
	protected final List<LoadGenerator> generators = new ArrayList<>();
	protected final List<StorageDriver> drivers = new ArrayList<>();
	protected final List<LoadController> controllers = new ArrayList<>();

	protected boolean clientFlag = false;
	protected volatile LoadStepClient stepClient = null;

	private volatile Config actualConfig = null;
	private volatile long timeLimitSec = Long.MAX_VALUE;
	private volatile long startTimeSec = -1;
	private String id = null;

	protected LoadStepBase(
		final Config baseConfig, final List<Extension> extensions,
		final List<Map<String, Object>> overrides
	) {
		this.baseConfig = baseConfig;
		this.extensions = extensions;
		this.stepConfigs = overrides;
	}

	@Override
	public LoadStepBase config(final Map<String, Object> config) {
		final List<Map<String, Object>> stepConfigsCopy = new ArrayList<>();
		if(stepConfigs != null) {
			stepConfigsCopy.addAll(stepConfigs);
		}
		final Map<String, Object> stepConfig = deepCopyTree(config);
		stepConfigsCopy.add(stepConfig);
		return copyInstance(stepConfigsCopy);
	}

	private static Map<String, Object> deepCopyTree(final Map<String, Object> srcTree) {
		return srcTree
			.entrySet()
			.stream()
			.collect(
				Collectors.toMap(
					Map.Entry::getKey,
					entry -> {
						final Object value = entry.getValue();
						return value instanceof Map ? deepCopyTree((Map<String, Object>) value) : value;
					}
				)
			);
	}

	@Override
	public final String id() {
		return id;
	}

	@Override //@SuppressWarnings("deprecation")
	public final List<MetricsSnapshot> metricsSnapshots() {
		return metricsContexts
			.stream()
			.map(MetricsContext::lastSnapshot)
			.collect(Collectors.toList());
	}

	protected abstract LoadStepBase copyInstance(final List<Map<String, Object>> stepConfigs);

	protected final void actualConfig(final Config actualConfig) {
		this.actualConfig = actualConfig;
		final Config stepConfig = actualConfig.configVal("load-step");
		this.id = stepConfig.stringVal("id");
		final List<String> nodeAddrs = stepConfig.listVal("node-addrs");
		this.clientFlag = nodeAddrs != null && !nodeAddrs.isEmpty();
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
		} catch(final CancellationException e) {
			throw e;
		} catch(final IllegalStateException e) {
			LogUtil.exception(Level.ERROR, e, "Failed to start \"{}\"", toString());
		} catch(final Throwable cause) {
			LogUtil.exception(Level.ERROR, cause, "Load step execution failure \"{}\"", toString());
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

		final Config stepConfig = actualConfig.configVal("load-step");
		final String stepId = stepConfig.stringVal("id");
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			if(clientFlag) {
				// need to set the once generated step id
				final Config config = new BasicConfig(baseConfig);
				config.val("load-step-id", stepId);
				stepClient = new LoadStepClientImpl(this, config, extensions, stepConfigs);
				stepClient.start();
			} else {
				doStartLocal();
			}

			final long t = TimeUtil.getTimeInSeconds(stepConfig.stringVal("limit-time"));
			if(t > 0) {
				timeLimitSec = t;
			}
			startTimeSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		} catch(final Throwable cause) {
			LogUtil.exception(Level.WARN, cause, "{} step failed to start", id);
		}

		startMetricsAccounting();
	}

	/**
	 * Initializes the actual configuration and metrics contexts
	 * @throws IllegalStateException if initialization fails
	 */
	protected abstract void init()
	throws IllegalStateException;

	protected final void initDistributedMetrics(
		final int originIndex, final IoType ioType, final int concurrency, final int nodeCount,
		final Config metricsConfig, final SizeInBytes itemDataSize, final boolean outputColorFlag
	) {
		metricsContexts.add(
			new AggregatingMetricsContext(
				id, ioType, nodeCount, concurrency * nodeCount,
				(int) (concurrency * nodeCount * metricsConfig.doubleVal("threshold")),
				itemDataSize,
				(int) TimeUtil.getTimeInSeconds(metricsConfig.stringVal("average-period")),
				outputColorFlag, metricsConfig.boolVal("average-persist"),
				metricsConfig.boolVal("summary-persist"),
				metricsConfig.boolVal("summary-perfDbResultsFile"),
				() -> stepClient.remoteMetricsSnapshots(originIndex)
			)
		);
	}

	protected final void initLocalMetrics(
		final IoType ioType, final int concurrency, final Config metricsConfig,
		final SizeInBytes itemDataSize, final boolean outputColorFlag
	) {
		metricsContexts.add(
			new MetricsContextImpl(
				id, ioType,
				() -> drivers.stream().mapToInt(StorageDriver::getActiveTaskCount).sum(),
				concurrency, (int) (concurrency * metricsConfig.doubleVal("threshold")),
				itemDataSize,
				(int) TimeUtil.getTimeInSeconds(metricsConfig.stringVal("average-period")),
				outputColorFlag, metricsConfig.boolVal("average-persist"),
				metricsConfig.boolVal("summary-persist"),
				metricsConfig.boolVal("summary-perfDbResultsFile")
			)
		);
	}

	private void doStartLocal() {
		controllers.forEach(
			controller -> {
				try {
					controller.start();
				} catch(final RemoteException ignored) {
				} catch(final IllegalStateException e) {
					LogUtil.exception(
						Level.WARN, e, "{}: failed to start the load controller \"{}\"", id,
						controller
					);
				}
			}
		);
		drivers.forEach(
			driver -> {
				try {
					driver.start();
				} catch(final RemoteException ignored) {
				} catch(final IllegalStateException e) {
					LogUtil.exception(
						Level.WARN, e, "{}: failed to start the storage driver \"{}\"", id,
						driver
					);
				}
			}
		);
		generators.forEach(
			generator -> {
				try {
					generator.start();
				} catch(final RemoteException ignored) {
				} catch(final IllegalStateException e) {
					LogUtil.exception(
						Level.WARN, e, "{}: failed to start the load generator \"{}\"", id,
						generator
					);
				}
			}
		);
	}

	private void startMetricsAccounting() {
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
					final CloseableThreadContext.Instance ctx = CloseableThreadContext
						.put(KEY_STEP_ID, id)
						.put(KEY_CLASS_NAME, getClass().getSimpleName())
				) {
					generator.shutdown();
					Loggers.MSG.debug(
						"{}: load generator \"{}\" interrupted", id, generator.toString()
					);
				} catch(final RemoteException ignored) {
				}
			}
		);

		drivers.forEach(
			driver -> {
				try(
					final CloseableThreadContext.Instance ctx = CloseableThreadContext
						.put(KEY_STEP_ID, id)
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
		final ExecutorService awaitExecutor = Executors.newFixedThreadPool(
			controllers.size(), new LogContextThreadFactory(id)
		);
		final CountDownLatch latch = new CountDownLatch(controllers.size());
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

		generators
			.parallelStream()
			.filter(Objects::nonNull)
			.forEach(
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

		drivers
			.parallelStream()
			.filter(Objects::nonNull)
			.forEach(
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

		controllers
			.parallelStream()
			.filter(Objects::nonNull)
			.forEach(
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
