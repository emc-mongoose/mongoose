package com.emc.mongoose.base.control.run;

public interface Run extends Runnable {

	/**
	 * @return the count of the milliseconds since 1970-01-01 and the start
	 * @throws IllegalStateException if not started yet
	 */
	long timestamp() throws IllegalStateException;

	/** @return user comment for this run */
	String comment();
}
