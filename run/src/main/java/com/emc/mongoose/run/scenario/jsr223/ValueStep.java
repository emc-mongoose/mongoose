package com.emc.mongoose.run.scenario.jsr223;

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
