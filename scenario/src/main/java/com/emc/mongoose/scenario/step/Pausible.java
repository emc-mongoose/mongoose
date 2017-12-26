package com.emc.mongoose.scenario.step;

import java.rmi.RemoteException;

public interface Pausible {

	void pause()
	throws IllegalStateException, RemoteException;
}
