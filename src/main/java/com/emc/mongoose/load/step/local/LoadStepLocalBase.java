package com.emc.mongoose.load.step.local;

import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.load.controller.LoadController;
import com.emc.mongoose.load.generator.LoadGenerator;
import com.emc.mongoose.load.step.LoadStepBase;
import com.emc.mongoose.logging.LogContextThreadFactory;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.metrics.MetricsContext;
import com.emc.mongoose.metrics.MetricsContextImpl;
import com.emc.mongoose.storage.driver.StorageDriver;

import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;

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

import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;

public abstract class LoadStepLocalBase
extends LoadStepBase {

	protected final List<LoadGenerator> generators = new ArrayList<>();
	protected final List<StorageDriver> drivers = new ArrayList<>();
	protected final List<LoadController> controllers = new ArrayList<>();

	protected LoadStepLocalBase(
		final Config baseConfig, final List<Extension> extensions, final List<Map<String, Object>> overrides
	) {
		super(baseConfig, extensions, overrides);
	}

	@Override
	protected void doStartWrapped() {
		controllers.forEach(
			controller -> {
				try {
					controller.start();
				} catch(final RemoteException ignored) {
				} catch(final IllegalStateException e) {
					LogUtil.exception(
						Level.WARN, e, "{}: failed to start the load controller \"{}\"", id(),
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
						Level.WARN, e, "{}: failed to start the storage driver \"{}\"", id(),
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
						Level.WARN, e, "{}: failed to start the load generator \"{}\"", id(),
						generator
					);
				}
			}
		);
	}

	@Override
	protected final void initMetrics(
		final int originIndex, final IoType ioType, final int concurrency, final Config metricsConfig,
		final SizeInBytes itemDataSize, final boolean outputColorFlag
	) {
		final int metricsAvgPeriod;
		final Object metricsAvgPeriodRaw = metricsConfig.val("average-period");
		if(metricsAvgPeriodRaw instanceof String) {
			metricsAvgPeriod = (int) TimeUtil.getTimeInSeconds((String) metricsAvgPeriodRaw);
		} else {
			metricsAvgPeriod = TypeUtil.typeConvert(metricsAvgPeriodRaw, int.class);
		}
		final MetricsContext metricsCtx = new MetricsContextImpl(
			id(), ioType, () -> drivers.stream().mapToInt(StorageDriver::getActiveTaskCount).sum(),
			concurrency, (int) (concurrency * metricsConfig.doubleVal("threshold")), itemDataSize, metricsAvgPeriod,
			outputColorFlag, metricsConfig.boolVal("average-persist"), metricsConfig.boolVal("summary-persist"),
			metricsConfig.boolVal("summary-perfDbResultsFile")
		);
		metricsContexts.add(metricsCtx);
	}

	protected final void doShutdown() {

		generators.forEach(
			generator -> {
				try(
					final CloseableThreadContext.Instance ctx = CloseableThreadContext
						.put(KEY_STEP_ID, id())
						.put(KEY_CLASS_NAME, getClass().getSimpleName())
				) {
					generator.shutdown();
					Loggers.MSG.debug(
						"{}: load generator \"{}\" interrupted", id(), generator.toString()
					);
				} catch(final RemoteException ignored) {
				}
			}
		);

		drivers.forEach(
			driver -> {
				try(
					final CloseableThreadContext.Instance ctx = CloseableThreadContext
						.put(KEY_STEP_ID, id())
						.put(KEY_CLASS_NAME, getClass().getSimpleName())
				) {
					driver.shutdown();
					Loggers.MSG.debug(
						"{}: next storage driver {} shutdown", id(), driver.toString()
					);
				} catch(final RemoteException ignored) {
				}
			}
		);
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		final ExecutorService awaitExecutor = Executors.newFixedThreadPool(
			controllers.size(), new LogContextThreadFactory(id())
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
	protected final void doStop() {
		drivers.forEach(
			driver -> {
				try {
					driver.stop();
				} catch(final RemoteException ignored) {
				}
				Loggers.MSG.debug("{}: next storage driver {} stopped", id(), driver.toString());
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
		super.doStop();
	}

	protected final void doClose()
	throws IOException {

		super.doClose();

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
