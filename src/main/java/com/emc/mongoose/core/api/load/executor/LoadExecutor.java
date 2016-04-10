package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.IoTask;

import java.io.Closeable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Future;
/**
 Created by andrey on 08.04.16.
 */
public interface LoadExecutor<T extends Item, A extends IoTask<T>>
extends Closeable {

	Future<A> submit(final A ioTask)
	throws RemoteException;

	int submit(final List<A> requests, final int from, final int to)
	throws RemoteException;
}
