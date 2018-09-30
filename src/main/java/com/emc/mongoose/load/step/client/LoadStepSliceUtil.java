package com.emc.mongoose.load.step.client;

import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.load.step.LoadStep;
import com.emc.mongoose.load.step.LoadStepManagerService;
import com.emc.mongoose.load.step.service.LoadStepService;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.svc.ServiceUtil;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;

import com.github.akurilov.confuse.Config;

import static org.apache.logging.log4j.CloseableThreadContext.put;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
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
		final List<Config> ctxConfigsCopy = new ArrayList<>();
		ctxConfigsCopy.addAll(ctxConfigs);
		try {
			stepSvcName = stepMgrSvc.getStepService(stepTypeName, configSlice, ctxConfigsCopy);
		} catch(final Exception e) {
			LogUtil.exception(Level.ERROR, e, "Failed to start the new scenario step service @ {}", nodeAddrWithPort);
			e.printStackTrace();
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

	static boolean await(final LoadStep stepSlice, final long timeout, final TimeUnit timeUnit)
	throws InterruptRunException {
		try(
			final Instance logCtx = put(KEY_STEP_ID, stepSlice.id())
				.put(KEY_CLASS_NAME, LoadStepClientBase.class.getSimpleName())
		) {
			long commFailCount = 0;
			while(true) {
				try {
					if(stepSlice.await(timeout, timeUnit)) {
						return true;
					}
				} catch(final RemoteException e) {
					LogUtil.exception(
						Level.DEBUG, e, "Failed to invoke the step slice \"{}\" await method {} times",
						stepSlice, commFailCount
					);
					commFailCount ++;
					Thread.sleep(commFailCount);
				}
			}
		} catch(final InterruptedException e) {
			throw new InterruptRunException(e);
		} catch(final RemoteException ignored) {
			return false;
		}
	}
}
