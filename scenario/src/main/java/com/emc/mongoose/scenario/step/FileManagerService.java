package com.emc.mongoose.scenario.step;

import com.emc.mongoose.api.model.svc.Service;

import java.rmi.RemoteException;

public interface FileManagerService
extends Service {

	String SVC_NAME = "file/manager";

	String createFileService(final String path)
	throws RemoteException;

	String createLogFileService(final String loggerName, final String testStepId)
	throws RemoteException;
}
