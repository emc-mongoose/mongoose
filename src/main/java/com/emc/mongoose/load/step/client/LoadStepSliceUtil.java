package com.emc.mongoose.load.step.client;

import com.emc.mongoose.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.load.step.LoadStep;
import com.emc.mongoose.load.step.LoadStepManagerService;
import com.emc.mongoose.load.step.service.LoadStepService;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.svc.ServiceUtil;

import com.github.akurilov.confuse.Config;

import com.github.akurilov.fiber4j.ExclusiveFiberBase;

import org.apache.logging.log4j.Level;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public interface LoadStepSliceUtil {

	static LoadStepService resolveRemote(
		final Config configSlice, final List<Config> ctxConfigs, final String stepTypeName,
		final String nodeAddrWithPort
	) {

		final LoadStepManagerService stepMgrSvc;
		try {
			stepMgrSvc = ServiceUtil.resolve(nodeAddrWithPort, LoadStepManagerService.SVC_NAME);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.ERROR, e, "Failed to resolve the service \"{}\" @ {}", LoadStepManagerService.SVC_NAME,
				nodeAddrWithPort
			);
			return null;
		}

		final String stepSvcName;
		try {
			stepSvcName = stepMgrSvc.getStepService(stepTypeName, configSlice, ctxConfigs);
		} catch(final Exception e) {
			LogUtil.exception(Level.ERROR, e, "Failed to start the new scenario step service @ {}", nodeAddrWithPort);
			return null;
		}

		final LoadStepService stepSvc;
		try {
			stepSvc = ServiceUtil.resolve(nodeAddrWithPort, stepSvcName);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.ERROR, e, "Failed to resolve the service \"{}\" @ {}", LoadStepManagerService.SVC_NAME,
				nodeAddrWithPort
			);
			return null;
		}

		try {
			Loggers.MSG.info("{}: load step service is resolved @ {}", stepSvc.name(), nodeAddrWithPort);
		} catch(final RemoteException ignored) {
		}

		return stepSvc;
	}

	final class AwaitTask
	extends ExclusiveFiberBase {

		private final CountDownLatch sharedCountDown;
		private final LoadStep stepSlice;
		private int commFailCount = 0;

		public AwaitTask(final CountDownLatch sharedCountDown, final LoadStep stepSlice) {
			super(ServiceTaskExecutor.INSTANCE);
			this.sharedCountDown = sharedCountDown;
			this.stepSlice = stepSlice;
		}

		@Override
		protected final void invokeTimedExclusively(final long startTimeNanos) {
			try {
				try {
					if(stepSlice.await(1, TimeUnit.NANOSECONDS)) {
						sharedCountDown.countDown();
						stop();
					}
				} catch(final RemoteException e) {
					LogUtil.exception(
						Level.DEBUG, e, "Failed to invoke the step slice \"{}\" await method {} times", stepSlice,
						commFailCount
					);
					commFailCount ++;
					Thread.sleep(commFailCount);
				}
			} catch(final InterruptedException e) {
				stop();
				throw new CancellationException();
			}
		}
	}
}
