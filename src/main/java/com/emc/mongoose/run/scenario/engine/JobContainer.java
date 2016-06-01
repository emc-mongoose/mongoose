package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.conf.AppConfig;

import java.io.Closeable;

/**
 Created by kurila on 02.02.16.
 */
public interface JobContainer
extends Closeable, Runnable {

	AppConfig getConfig();

	boolean append(final JobContainer subJob);
}
