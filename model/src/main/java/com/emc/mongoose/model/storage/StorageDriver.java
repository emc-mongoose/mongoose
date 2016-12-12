package com.emc.mongoose.model.storage;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 Created on 11.07.16.
 */
public interface StorageDriver<I extends Item, O extends IoTask<I, R>, R extends IoResult>
extends Daemon, Output<O>, Remote {

	String HOST_ADDR = ServiceUtil.getHostAddr();
	
	boolean createPath(final String path)
	throws RemoteException;

	List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException;

	String getAuthToken()
	throws RemoteException;

	void setAuthToken(final String authToken)
	throws RemoteException;

	List<R> getResults()
	throws IOException;

	int getActiveTaskCount()
	throws RemoteException;

	boolean isIdle()
	throws RemoteException;

	boolean isFullThrottleEntered()
	throws RemoteException;

	boolean isFullThrottleExited()
	throws RemoteException;
}
