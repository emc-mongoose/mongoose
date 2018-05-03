package com.emc.mongoose.scenario.step;

import com.emc.mongoose.model.svc.Service;
import com.emc.mongoose.config.Config;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface LoadStepManagerService
extends Service {

	String SVC_NAME = "scenario/step/manager";

	String getStepService(
		final String stepType, final Config config, final List<Map<String, Object>> stepConfigs
	) throws RemoteException;
}
