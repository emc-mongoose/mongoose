package com.emc.mongoose.scenario.step;

import com.emc.mongoose.scenario.ScenarioParseException;

/**
 A scenario step or the scenario steps which can include other scenario steps
 */
public interface CompositeStep
extends Step {

	/**
	 @param child append child step to the list of the child/nested scenario steps
	 @return <b>new</b> composite step instance with applied child steps
	 */
	CompositeStep step(final Step child)
	throws ScenarioParseException;
}
