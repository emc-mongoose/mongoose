package com.emc.mongoose.scenario.step;

import com.emc.mongoose.api.model.svc.Service;

import java.rmi.RemoteException;

/**
 Remote file access service. All files created are deleted when JVM exits.
 */
public interface FileManagerService
extends Service {

	String SVC_NAME = "file/manager";

	/**
	 @param path the path of the file, may be null (new temporary file path is used in this case)
	 @return the name of the new file service
	 @throws RemoteException
	 */
	String createFileService(final String path)
	throws RemoteException;

	String createLogFileService(final String loggerName, final String testStepId)
	throws RemoteException;
}
