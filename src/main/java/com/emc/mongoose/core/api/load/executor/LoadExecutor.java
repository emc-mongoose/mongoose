package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.AsyncConsumer;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.load.model.LoadState;
//
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
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
public interface LoadExecutor<T extends DataItem>
extends Producer<T>, AsyncConsumer<T> {
	//
	Map<String, AtomicInteger>
		INSTANCE_NUMBERS = new ConcurrentHashMap<>();
	Map<String, List<LoadState<? extends DataItem>>>
		RESTORED_STATES_MAP = new ConcurrentHashMap<>();
	//
	String getName()
	throws RemoteException;
	//
	Producer<T> getProducer()
	throws RemoteException;
	//
	RequestConfig<T> getRequestConfig()
	throws RemoteException;
	//
	Future<IOTask.Status> submitReq(final IOTask<T> request)
	throws RemoteException, RejectedExecutionException;
	//
	List<Future<IOTask.Status>> submitBatchReq(final List<? extends IOTask<T>> requests)
	throws RemoteException, RejectedExecutionException;
	//
	void handleResult(final IOTask<T> task)
	throws RemoteException;
	//
	void handleBatchResult(final List<IOTask<T>> tasks)
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
}
