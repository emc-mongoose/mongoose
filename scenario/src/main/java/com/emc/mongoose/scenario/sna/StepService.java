package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;

public interface StepService
extends Service, Step {

	String SVC_NAME_PREFIX = "scenario/step/";
}
