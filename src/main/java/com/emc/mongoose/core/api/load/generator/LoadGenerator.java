package com.emc.mongoose.core.api.load.generator;
//
import com.emc.mongoose.common.concurrent.LifeCycle;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.metrics.IOStats;
import org.apache.logging.log4j.Marker;
//
import java.rmi.RemoteException;
import java.util.List;
/**
 Created by kurila on 09.05.14.
 A producer feeding the generated items to its consumer.
 May be linked with particular consumer, started and interrupted.
 */
public interface LoadGenerator<T extends Item, A extends IoTask<T>>
extends LifeCycle {

	// immutable properties

	String getName()
	throws RemoteException;

	long getCountLimit()
	throws RemoteException;

	int getWeight()
	throws RemoteException;

	boolean isCircular()
	throws RemoteException;

	Input<T> getInput()
	throws RemoteException;

	LoadExecutor<T> getExecutor()
	throws RemoteException;

	IOStats.Snapshot getStatsSnapshot()
	throws RemoteException;

	// mutable properties

	LoadState<T> getState()
	throws RemoteException;

	void setState(final LoadState<T> state)
	throws RemoteException;

	void setOutput(final Output<T> itemDst)
	throws RemoteException;

	void setSkipCount(final long itemsCount)
	throws RemoteException;

	void setLastItem(final T item)
	throws RemoteException;

	// other methods

	void ioTaskCompleted(final A ioTask)
	throws RemoteException;

	int ioTaskCompletedBatch(final List<A> ioTasks, final int from, final int to)
	throws RemoteException;

	void logMetrics(Marker marker)
	throws RemoteException;

	void reset()
	throws RemoteException;
}
