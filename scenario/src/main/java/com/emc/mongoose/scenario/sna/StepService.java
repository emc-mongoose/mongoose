package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;

public interface StepService
extends Service, Step {

	String SVC_NAME_PREFIX = "scenario/step/";

	static StepService stopStepSvc(final StepService stepSvc) {
		try {
			stepSvc.stop();
		} catch(final Exception e) {
			try {
				LogUtil.exception(
					Level.WARN, e, "Failed to stop the step service \"{}\"",
					stepSvc.getName()
				);
			} catch(final Exception ignored) {
			}
		}
		return stepSvc;
	}

	static StepService closeStepSvc(final StepService stepSvc) {
		try {
			stepSvc.close();
		} catch(final Exception e) {
			try {
				LogUtil.exception(
					Level.WARN, e, "Failed to close the step service \"{}\"",
					stepSvc.getName()
				);
			} catch(final Exception ignored) {
			}
		}
		return stepSvc;
	}
}
