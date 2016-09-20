package com.emc.mongoose.model.api.load;

import com.emc.mongoose.common.concurrent.InterruptibleDaemon;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 Created on 11.07.16.
 */
public interface Driver<I extends Item, O extends IoTask<I>>
extends InterruptibleDaemon, IoTaskCallback<I, O>, Registry<Monitor<I, O>>, Remote {

	boolean isFullThrottleEntered()
	throws RemoteException;

	boolean isFullThrottleExited()
	throws RemoteException;

	void submit(final O task)
	throws InterruptedException, RemoteException;

	int submit(final List<O> tasks, final int from, final int to)
	throws InterruptedException, RemoteException;
}
