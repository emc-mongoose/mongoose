package com.emc.mongoose.model.api.load;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.model.api.io.Output;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 Created on 11.07.16.
 */
public interface StorageDriver<I extends Item, O extends IoTask<I>>
extends Daemon, Output<O>, Registry<Monitor<I, O>>, Remote {

	boolean isFullThrottleEntered()
	throws RemoteException;

	boolean isFullThrottleExited()
	throws RemoteException;
}
