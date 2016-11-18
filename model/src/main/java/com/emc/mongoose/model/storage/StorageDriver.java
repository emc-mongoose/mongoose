package com.emc.mongoose.model.storage;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.result.IoResult;
import com.emc.mongoose.model.item.Item;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 Created on 11.07.16.
 */
public interface StorageDriver<I extends Item, O extends IoTask<I>, R extends IoResult>
extends Daemon, Output<O>, Remote {

	String HOST_ADDR = ServiceUtil.getHostAddr();
	
	boolean configureStorage()
	throws RemoteException;
	
	void setOutput(final Output<R> ioTaskResultOutput)
	throws RemoteException;

	int getActiveTaskCount()
	throws RemoteException;

	boolean isIdle()
	throws RemoteException;

	boolean isFullThrottleEntered()
	throws RemoteException;

	boolean isFullThrottleExited()
	throws RemoteException;
}
