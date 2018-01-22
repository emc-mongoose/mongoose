package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.concurrent.AsyncRunnable;
import com.emc.mongoose.scenario.ScenarioParseException;

import java.rmi.RemoteException;

public interface Step
extends AsyncRunnable {

	/**
	 Configure the step. The actual behavior depends on the particular step type
	 @param config
	 @return the copied step instance
	 @throws ScenarioParseException
	 */
	Step config(final Object config)
	throws ScenarioParseException, RemoteException;

	/**
	 @return the step id
	 */
	String id()
	throws RemoteException;
}
