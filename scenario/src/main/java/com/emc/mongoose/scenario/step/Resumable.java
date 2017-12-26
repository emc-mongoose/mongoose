package com.emc.mongoose.scenario.step;

import java.rmi.RemoteException;

public interface Resumable {

	void resume()
	throws IllegalStateException, RemoteException;
}
