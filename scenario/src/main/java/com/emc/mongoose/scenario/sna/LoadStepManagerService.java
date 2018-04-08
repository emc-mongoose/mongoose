package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;
import com.emc.mongoose.ui.config.Config;

import java.rmi.RemoteException;

public interface LoadStepManagerService
extends Service {

	String SVC_NAME = "scenario/step/manager";

	String getStepService(final String stepType, final Config config)
	throws RemoteException;
}
