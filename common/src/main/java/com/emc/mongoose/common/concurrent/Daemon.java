package com.emc.mongoose.common.concurrent;

import com.emc.mongoose.common.exception.UserShootHisFootException;

/**
 Created by kurila on 23.09.15.
 */
public interface Daemon
extends Launchable {

	void shutdown()
	throws UserShootHisFootException;

	boolean isShutdown();

	void interrupt()
	throws UserShootHisFootException;

	boolean isInterrupted();

}
