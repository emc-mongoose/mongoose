package com.emc.mongoose.model.storage;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
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
extends Daemon, Input<O>, Output<O>, Remote {
	
	int BUFF_SIZE_MIN = 0x1_000;
	int BUFF_SIZE_MAX = 0x1_000_000;
	
	List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException;

	@Override
	default int get(final List<O> buff, final int limit)
	throws RemoteException {
		throw new AssertionError("Shouldn't be invoked");
	}
	
	@Override
	default void reset()
	throws RemoteException {
		throw new AssertionError("Shouldn't be invoked");
	}

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

	void adjustIoBuffers(final long avgDataItemSize, final IoType ioType)
	throws RemoteException;
}
