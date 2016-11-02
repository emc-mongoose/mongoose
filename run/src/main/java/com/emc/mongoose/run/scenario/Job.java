package com.emc.mongoose.run.scenario;

import com.emc.mongoose.ui.config.Config;

import java.io.Closeable;

/**
 Created by kurila on 02.02.16.
 */
public interface Job
extends Closeable, Runnable {

	String KEY_NODE_CONFIG = "config";
	String KEY_NODE_JOBS = "jobs";
	String KEY_NODE_VALUE = "value";
	String KEY_NODE_TYPE = "type";

	String NODE_TYPE_PARALLEL = "parallel";
	String NODE_TYPE_SEQUENTIAL = "sequential";
	String NODE_TYPE_LOAD = "load";
	String NODE_TYPE_PRECONDITION = "precondition";
	String NODE_TYPE_COMMAND = "command";
	String NODE_TYPE_FOR = "for";

	Config getConfig();
}
