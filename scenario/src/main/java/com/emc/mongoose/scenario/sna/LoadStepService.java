package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;

public interface LoadStepService
extends Service, LoadStep {

	String SVC_NAME_PREFIX = "scenario/step/";
}
