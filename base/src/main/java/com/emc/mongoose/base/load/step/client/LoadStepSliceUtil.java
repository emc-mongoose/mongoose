package com.emc.mongoose.base.load.step.client;

import static com.emc.mongoose.base.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.base.Constants.KEY_STEP_ID;
import static com.emc.mongoose.base.Exceptions.throwUncheckedIfInterrupted;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;
import static org.apache.logging.log4j.CloseableThreadContext.put;

import com.emc.mongoose.base.load.step.LoadStep;
import com.emc.mongoose.base.load.step.LoadStepManagerService;
import com.emc.mongoose.base.load.step.service.LoadStepService;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.svc.ServiceUtil;
import com.github.akurilov.confuse.Config;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;

public interface LoadStepSliceUtil {

	static LoadStepService resolveRemote(
					final Config configSlice,
					final List<Config> ctxConfigs,
					final String stepTypeName,
					final String nodeAddrWithPort) {

		final LoadStepManagerService stepMgrSvc;
		try {
			stepMgrSvc = ServiceUtil.resolve(nodeAddrWithPort, LoadStepManagerService.SVC_NAME);
		} catch (final Exception e) {
			LogUtil.exception(
							Level.ERROR,
							e,
							"Failed to resolve the service \"{}\" @ {}",
							LoadStepManagerService.SVC_NAME,
							nodeAddrWithPort);
			return null;
		}

		final String stepSvcName;
		try {
			stepSvcName = stepMgrSvc.getStepService(stepTypeName, configSlice, ctxConfigs);
		} catch (final Exception e) {
			throwUncheckedIfInterrupted(e);
			LogUtil.exception(
							Level.ERROR, e, "Failed to start the new scenario step service @ {}", nodeAddrWithPort);
			return null;
		}

		final LoadStepService stepSvc;
		try {
			stepSvc = ServiceUtil.resolve(nodeAddrWithPort, stepSvcName);
		} catch (final Exception e) {
			LogUtil.exception(
							Level.ERROR,
							e,
							"Failed to resolve the service \"{}\" @ {}",
							LoadStepManagerService.SVC_NAME,
							nodeAddrWithPort);
			return null;
		}

		try {
			Loggers.MSG.info("{}: load step service is resolved @ {}", stepSvc.name(), nodeAddrWithPort);
		} catch (final RemoteException ignored) {}

		return stepSvc;
	}

	static boolean await(final LoadStep stepSlice, final long timeout, final TimeUnit timeUnit) {
		try (final var logCtx = put(KEY_STEP_ID, stepSlice.id())
						.put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())) {
			long commFailCount = 0;
			while (true) {
				try {
					if (stepSlice.await(timeout, timeUnit)) {
						return true;
					}
				} catch (final RemoteException e) {
					LogUtil.exception(
									Level.DEBUG,
									e,
									"Failed to invoke the step slice \"{}\" await method {} times",
									stepSlice,
									commFailCount);
					commFailCount++;
					Thread.sleep(commFailCount);
				}
			}
		} catch (final InterruptedException e) {
			throwUnchecked(e);
		} catch (final RemoteException ignored) {
			return false;
		}
		return false;
	}
}
