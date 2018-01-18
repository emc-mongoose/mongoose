package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;

public interface ScenarioStepManagerService
extends Service {

	String SVC_NAME = "scenario/step/manager";

	String getCommandStepService()
	throws Exception;

	String getLoadStepService()
	throws Exception;
}
