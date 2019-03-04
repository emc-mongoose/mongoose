package com.emc.mongoose.base.load.step.service;

import com.emc.mongoose.base.load.step.LoadStep;
import com.emc.mongoose.base.svc.Service;

public interface LoadStepService extends Service, LoadStep {

	String SVC_NAME_PREFIX = "load/step/";
}
