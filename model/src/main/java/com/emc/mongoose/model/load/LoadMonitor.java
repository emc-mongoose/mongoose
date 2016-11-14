package com.emc.mongoose.model.load;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.model.io.task.IoTask;

import java.rmi.RemoteException;

/**
 Created on 11.07.16.
 */
public interface LoadMonitor<R extends IoTask.IoResult>
extends Daemon, Output<R> {
	
	String getName()
	throws RemoteException;
	
	void setItemInfoOutput(final Output<String> itemInfoOutput)
	throws RemoteException;
}
