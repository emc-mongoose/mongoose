package com.emc.mongoose.model.api.load;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.model.api.io.Output;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;

import java.rmi.RemoteException;
/**
 Created on 11.07.16.
 */
public interface LoadMonitor<I extends Item, O extends IoTask<I>>
extends Daemon, Output<O> {
	
	String getName()
	throws RemoteException;
	
	void setItemOutput(final Output<I> itemOutput)
	throws RemoteException;
}
