package com.emc.mongoose.scenario.step.svc;

import com.emc.mongoose.api.model.svc.Service;
import com.emc.mongoose.scenario.step.LoadStep;

public interface LoadStepService
extends Service, LoadStep {

	String SVC_NAME_PREFIX = "scenario/step/";
}
