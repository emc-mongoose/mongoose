package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.AsyncConsumer;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.load.model.LoadState;
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
public interface LoadExecutor<T extends DataItem>
extends Producer<T>, AsyncConsumer<T> {
	//
	Map<String, AtomicInteger> INSTANCE_NUMBERS = new ConcurrentHashMap<>();
	Map<String, List<LoadState>> RESTORED_STATES_MAP = new ConcurrentHashMap<>();
	//AtomicInteger NEXT_INSTANCE_NUM = new AtomicInteger(0);
	//
	int NANOSEC_SCALEDOWN = 1000, MIB = 0x100000;
	//
	String
		METRIC_NAME_SUCC = "succ",
		METRIC_NAME_FAIL = "fail",
		METRIC_NAME_SUBM = "subm",
		METRIC_NAME_REJ = "rej",
		METRIC_NAME_REQ = "req",
		METRIC_NAME_TP = "TP",
		METRIC_NAME_BW = "BW",
		METRIC_NAME_DUR = "dur",
		METRIC_NAME_LAT = "lat",
		NAME_SEP = "@";
	//
	String MSG_FMT_METRICS = "count=(%d/%s); dur[us]=(%d/%d/%d/%d); lat[us]=(%d/%d/%d/%d); " +
		"TP[s^-1]=(%.3f/%.3f); BW[MB*s^-1]=(%.3f/%.3f)";
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
	Future<IOTask.Status> submit(final IOTask<T> request)
	throws RemoteException, RejectedExecutionException;
	//
	void handleResult(final IOTask<T> task)
	throws RemoteException;
	//
	void setLoadState(final LoadState<T> state)
	throws RemoteException;
	//
	LoadState<T> getLoadState()
	throws RemoteException;
	//
	void logMetrics(Marker marker)
	throws RemoteException;
}
