package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;

import java.rmi.RemoteException;

public interface ScenarioStepManagerService
extends Service {

	String SVC_NAME = "scenario/step/manager";
}
