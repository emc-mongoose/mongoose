package com.emc.mongoose.scenario.step;

import java.rmi.RemoteException;

public interface Stoppable {

	void stop()
	throws IllegalStateException, RemoteException;
}
