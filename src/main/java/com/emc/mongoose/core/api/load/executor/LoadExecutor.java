package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.common.concurrent.LifeCycle;
//
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.data.model.ItemDst;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.model.LoadState;
import com.emc.mongoose.core.api.load.model.ItemProducer;
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
//
import org.apache.logging.log4j.Marker;
//
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by kurila on 28.04.14.
 A mechanism of data items load execution.
 May be a consumer and producer both also.
 Supports method "join" for waiting the load execution to be done.
 */
public interface LoadExecutor<T extends Item>
extends ItemDst<T>, LifeCycle, ItemProducer<T> {
	//
	int
		DEFAULT_INTERNAL_BATCH_SIZE = 0x80,
		DEFAULT_RESULTS_QUEUE_SIZE = 0x10000;
	//
	AtomicInteger NEXT_INSTANCE_NUM = new AtomicInteger(0);
	//
	Map<String, List<LoadState<? extends Item>>>
		RESTORED_STATES_MAP = new ConcurrentHashMap<>();
	//
	String getName()
	throws RemoteException;
	//
	void setLoadState(final LoadState<T> state)
	throws RemoteException;
	//
	LoadState<T> getLoadState()
	throws RemoteException;
	//
	IOStats.Snapshot getStatsSnapshot()
	throws RemoteException;
	//
	void logMetrics(Marker marker)
	throws RemoteException;
	//
	Future<? extends IOTask<T>> submitReq(final IOTask<T> request)
	throws RemoteException, RejectedExecutionException;
	//
	int submitTasks(final List<? extends IOTask<T>> requests, final int from, final int to)
	throws RemoteException, RejectedExecutionException;
}
