package com.emc.mongoose.load.step;

import com.emc.mongoose.svc.Service;

import com.github.akurilov.confuse.Config;

import java.rmi.RemoteException;
import java.util.List;

public interface LoadStepManagerService
extends Service {

	String SVC_NAME = "load/step/manager";

	String getStepService(final String stepType, final Config config, final List<Config> ctxConfigs)
	throws RemoteException;
}
