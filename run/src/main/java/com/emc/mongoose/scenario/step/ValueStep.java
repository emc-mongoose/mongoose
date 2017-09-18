package com.emc.mongoose.scenario.step;

/**
 A scenario step accepting some value.
 */
public interface ValueStep
extends Step {

	/**
	 @param value
	 @return <b>new</b> ValueStepBuilder instance with applied value
	 */
	ValueStep value(final String value);
}
