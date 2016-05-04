package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.conf.AppConfig;
/**
 Created by kurila on 02.02.16.
 */
public interface JobContainer
extends Runnable {

	AppConfig getConfig();

	boolean append(final JobContainer subJob);
}
