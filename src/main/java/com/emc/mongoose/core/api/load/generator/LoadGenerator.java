package com.emc.mongoose.core.api.load.generator;
//
import com.emc.mongoose.common.concurrent.LifeCycle;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.metrics.IoStats;
import org.apache.logging.log4j.Marker;
//
import java.io.Closeable;
import java.rmi.RemoteException;
import java.util.List;
/**
 Created by kurila on 09.05.14.
 A producer feeding the generated items to its consumer.
 May be linked with particular consumer, started and interrupted.
 */
public interface LoadGenerator<T extends Item, A extends IoTask<T>>
extends LifeCycle, Closeable {

	// immutable properties

	String getName()
	throws RemoteException;

	LoadType getLoadType()
	throws RemoteException;

	long getCountLimit()
	throws RemoteException;

	int getWeight()
	throws RemoteException;

	boolean isCircular()
	throws RemoteException;

	boolean isShuffle()
	throws RemoteException;

	Input<T> getInput()
	throws RemoteException;

	LoadExecutor<T> getExecutor()
	throws RemoteException;

	IoStats.Snapshot getStatsSnapshot()
	throws RemoteException;

	// mutable properties

	LoadState<T> getLoadState()
	throws RemoteException;

	void setOutput(final Output<T> itemDst)
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
