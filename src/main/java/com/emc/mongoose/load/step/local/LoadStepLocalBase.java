package com.emc.mongoose.load.step.local;

import com.emc.mongoose.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.load.step.local.context.LoadStepContext;
import com.emc.mongoose.load.step.LoadStepBase;
import com.emc.mongoose.load.step.local.context.LoadStepContextImpl;
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

import static org.apache.logging.log4j.CloseableThreadContext.put;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;

import com.github.akurilov.fiber4j.ExclusiveFiberBase;

import org.apache.logging.log4j.Level;

import java.io.Closeable;
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

	protected final List<LoadStepContextImpl> stepContexts = new ArrayList<>();

	protected LoadStepLocalBase(
		final Config baseConfig, final List<Extension> extensions, final List<Config> contextConfigs
	) {
		super(baseConfig, extensions, contextConfigs);
	}

	@Override
	protected void doStartWrapped() {
		stepContexts.forEach(LoadStepContextImpl::start);
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
			id(), ioType, () -> stepContexts.stream().mapToInt(LoadStepContext::activeTasksCount).sum(),
			concurrency, (int) (concurrency * metricsConfig.doubleVal("threshold")), itemDataSize, metricsAvgPeriod,
			outputColorFlag
		);
		metricsContexts.add(metricsCtx);
	}

	@Override
	protected final void doShutdown() {
		stepContexts.forEach(LoadStepContextImpl::shutdown);
		super.doShutdown();
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		final CountDownLatch awaitCountDown = new CountDownLatch(stepContexts.size());
		final List<Closeable> awaitStepCtxTasks = stepContexts
			.stream()
			.map(stepCtx -> new AwaitTask(stepCtx, awaitCountDown))
			.peek(AsyncRunnableBase::start)
			.collect(Collectors.toList());
		try {
			return awaitCountDown.await(timeout, timeUnit);
		} finally {
			awaitStepCtxTasks
				.forEach(
					awaitStepCtxTask -> {
						try {
							awaitStepCtxTask.close();
						} catch(final IOException ignored) {
						}
					}
				);
		}
	}

	private static final class AwaitTask
	extends ExclusiveFiberBase {

		private final LoadStepContext loadStepCtx;
		private final CountDownLatch sharedCountDown;

		private AwaitTask(final LoadStepContext loadStepCtx, final CountDownLatch sharedCountDown) {
			super(ServiceTaskExecutor.INSTANCE);
			this.loadStepCtx = loadStepCtx;
			this.sharedCountDown = sharedCountDown;
		}

		@Override
		protected final void invokeTimedExclusively(final long startTimeNanos) {
			try {
				if(loadStepCtx.await(TIMEOUT_NANOS / 10, TimeUnit.NANOSECONDS)) {
					sharedCountDown.countDown();
					stop();
				}
			} catch(final InterruptedException e) {
				throw new CancellationException();
			} catch(final RemoteException ignored) {
			}
		}
	}

	@Override
	protected final void doStop() {
		stepContexts.forEach(LoadStepContextImpl::stop);
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
