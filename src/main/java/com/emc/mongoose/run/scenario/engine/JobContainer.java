package com.emc.mongoose.run.scenario.engine;
/**
 Created by kurila on 02.02.16.
 */
public interface JobContainer
extends Runnable {
	boolean append(final JobContainer subJob);
}
