package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.scenario.ScenarioParseException;

public interface Step
extends AsyncRunnable {

	Step config(final Object config)
	throws ScenarioParseException;
}
