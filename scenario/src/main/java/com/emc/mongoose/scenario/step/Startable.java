package com.emc.mongoose.scenario.step;

import java.rmi.RemoteException;

public interface Startable {

	void start()
	throws IllegalStateException, RemoteException;
}
