package com.emc.mongoose.model.storage;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 Created on 11.07.16.
 */
public interface StorageDriver<I extends Item, O extends IoTask<I>>
extends Daemon, Output<O>, Remote {
	
	String HOST_ADDR = ServiceUtil.getHostAddr();
	int BUFF_SIZE_MIN = 0x1_000;
	int BUFF_SIZE_MAX = 0x1_000_000;
	
	List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException;

	List<O> getResults()
	throws IOException;

	int getConcurrencyLevel()
	throws RemoteException;

	int getActiveTaskCount()
	throws RemoteException;
	
	long getScheduledTaskCount()
	throws RemoteException;
	
	long getCompletedTaskCount()
	throws RemoteException;

	boolean isIdle()
	throws RemoteException;

	void adjustIoBuffers(final SizeInBytes avgDataItemSize, final IoType ioType)
	throws RemoteException;
}
