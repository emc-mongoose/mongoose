package com.emc.mongoose.load.step.local;

import com.emc.mongoose.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.load.step.local.context.LoadStepContext;
import com.emc.mongoose.load.step.LoadStepBase;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.metrics.MetricsContext;
import com.emc.mongoose.metrics.MetricsContextImpl;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;

import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;

import com.github.akurilov.confuse.Config;

import com.github.akurilov.fiber4j.ExclusiveFiberBase;

import static org.apache.logging.log4j.CloseableThreadContext.put;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class LoadStepLocalBase
extends LoadStepBase {

	protected final List<LoadStepContext> stepContexts = new ArrayList<>();

	protected LoadStepLocalBase(
		final Config baseConfig, final List<Extension> extensions, final List<Config> contextConfigs
	) {
		super(baseConfig, extensions, contextConfigs);
	}

	@Override
	protected void doStartWrapped() {
		stepContexts.forEach(
			stepCtx -> {
				try {
					stepCtx.start();
				} catch(final RemoteException ignored) {
				} catch(final IllegalStateException e) {
					LogUtil.exception(
						Level.WARN, e, "{}: failed to start the load step context \"{}\"", id(),
						stepCtx
					);
				}
			}
		);
	}

	@Override
	protected final void initMetrics(
		final int originIndex, final OpType opType, final int concurrency, final Config metricsConfig,
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
			id(), opType, () -> stepContexts.stream().mapToInt(LoadStepContext::activeOpCount).sum(),
			concurrency, (int) (concurrency * metricsConfig.doubleVal("threshold")), itemDataSize, metricsAvgPeriod,
			outputColorFlag
		);
		metricsContexts.add(metricsCtx);
	}

	@Override
	protected final void doShutdown() {
		stepContexts.forEach(
			stepCtx -> {
				try(
					final Instance ctx = put(KEY_STEP_ID, id())
						.put(KEY_CLASS_NAME, getClass().getSimpleName())
				) {
					stepCtx.shutdown();
					Loggers.MSG.debug("{}: load step context shutdown", id());
				} catch(final RemoteException ignored) {
				}
			}
		);
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {

		final CountDownLatch awaitCountDown = new CountDownLatch(stepContexts.size());
		final List<AutoCloseable> awaitTasks = stepContexts
			.stream()
			.map(
				stepCtx -> new ExclusiveFiberBase(ServiceTaskExecutor.INSTANCE) {
					@Override
					protected final void invokeTimedExclusively(final long startTimeNanos) {
						try {
							if(stepCtx.await(TIMEOUT_NANOS, TimeUnit.NANOSECONDS)) {
								awaitCountDown.countDown();
							}
						} catch(final InterruptedException e) {
							throw new CancellationException();
						} catch(final Exception e) {
							LogUtil.exception(Level.WARN, e, "Await call failure on the step context \"{}\"", stepCtx);
						}
					}
				}
			)
			.peek(AsyncRunnableBase::start)
			.collect(Collectors.toList());

		try {
			return awaitCountDown.await(timeout, timeUnit);
		} finally {
			awaitTasks
				.forEach(
					awaitTask -> {
						try {
							awaitTask.close();
						} catch(final Exception ignored) {
						}
					}
				);
		}
	}

	@Override
	protected final void doStop() {
		stepContexts.forEach(
			stepCtx -> {
				try {
					stepCtx.stop();
				} catch(final RemoteException ignored) {
				}
			}
		);
		super.doStop();
	}

	protected final void doClose()
	throws IOException {

		super.doClose();

		stepContexts
			.parallelStream()
			.filter(Objects::nonNull)
			.forEach(
				stepCtx -> {
					try {
						stepCtx.close();
					} catch(final IOException e) {
						LogUtil.exception(
							Level.ERROR, e, "Failed to close the load step context \"{}\"",
							stepCtx.toString()
						);
					}
				}
			);
		stepContexts.clear();
	}
}
