package com.emc.mongoose.run.scenario.step;

import java.util.List;

/**
 Created by andrey on 05.06.17.
 */
public interface CompositeStep
extends Step {

	List<Step> getChildSteps();
}
