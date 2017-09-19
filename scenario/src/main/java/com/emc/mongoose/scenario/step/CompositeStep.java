package com.emc.mongoose.scenario.step;

import com.emc.mongoose.scenario.ScenarioParseException;

import java.util.Map;

/**
 A scenario step or the scenario steps which can include other scenario steps
 */
public interface CompositeStep
extends Step {

	/**
	 @param children a map representing a list of the child/nested scenario steps
	 @return <b>new</b> composite step instance with applied child steps
	 */
	CompositeStep steps(final Map<String, Object> children)
	throws ScenarioParseException;
}
