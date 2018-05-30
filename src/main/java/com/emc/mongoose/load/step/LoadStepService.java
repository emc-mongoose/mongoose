package com.emc.mongoose.load.step;

import com.emc.mongoose.svc.Service;

public interface LoadStepService
extends Service, LoadStep {

	String SVC_NAME_PREFIX = "load/step/";
}
