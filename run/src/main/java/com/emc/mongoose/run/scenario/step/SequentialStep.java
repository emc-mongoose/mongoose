package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.run.scenario.ScenarioParseException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Loggers;

import java.util.Map;

/**
 Created by kurila on 02.02.16.
 */
public class SequentialStep
extends ParentStepBase {
	//
	public SequentialStep(final Config appConfig, final Map<String, Object> subTree)
	throws ScenarioParseException {
		super(appConfig, subTree);
	}
	//
	@Override
	public String toString() {
		return "sequentialStep#" + hashCode();
	}
	
	//
	@Override
	protected void invoke() {
		synchronized(this) {
			Loggers.MSG.info(
				"{}: execute {} child steps sequentially", toString(), subSteps.size()
			);
			for(final Step subStep : subSteps) {
				Loggers.MSG.debug("{}: child step \"{}\" start", toString(), subStep.toString());
				subStep.run();
				Loggers.MSG.debug("{}: child step \"{}\" is done", toString(), subStep.toString());
			}
			Loggers.MSG.info(
				"{}: finished the sequential execution of {} child steps", toString(),
				subSteps.size()
			);
		}
	}
}
