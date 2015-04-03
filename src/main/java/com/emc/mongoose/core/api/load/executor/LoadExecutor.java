package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.model.Producer;
//
import org.apache.logging.log4j.Marker;
//
import java.rmi.RemoteException;
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
extends Producer<T>, Consumer<T> {
	//
	AtomicInteger LAST_INSTANCE_NUM = new AtomicInteger(0);
	//
	int
		NANOSEC_SCALEDOWN = 1000, MIB = 0x100000, COUNT_THREADS_MIN = 2,
		BUFF_SIZE_LO = (int) RunTimeConfig.getContext().getDataBufferSize(),
		BUFF_SIZE_HI = (int) RunTimeConfig.getContext().getDataRingSize();
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
	String
		MSG_FMT_METRICS = "count=(%d/%d/%s); latency[us]=(%d/%d/%d/%d); " +
			"TP[/s]=(%.3f/%.3f/%.3f/%.3f); BW[MB/s]=(%.3f/%.3f/%.3f/%.3f)",
		MSG_FMT_SUM_METRICS = "\"%s\" summary: count=(%d/%s); latency[us]=(%d/%d/%d/%d); " +
			"TP[/s]=(%.3f/%.3f/%.3f/%.3f); BW[MB/s]=(%.3f/%.3f/%.3f/%.3f)";
	//
	String getName()
	throws RemoteException;
	//
	Producer<T> getProducer()
	throws RemoteException;
	//
	Future<IOTask.Status> submit(final IOTask<T> request)
	throws RemoteException, RejectedExecutionException;
	//
	void handleResult(final IOTask<T> task, IOTask.Status status)
	throws RemoteException;
	//
	void join()
	throws RemoteException, InterruptedException;
	//
	void join(final long timeOutMilliSec)
	throws RemoteException, InterruptedException;
	//
	void logMetrics(Marker marker)
	throws RemoteException;
}
