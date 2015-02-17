package com.emc.mongoose.base.load;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.data.DataItem;
//
import java.rmi.RemoteException;
import java.util.concurrent.Future;
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
	int NANOSEC_SCALEDOWN = 1000, MIB = 0x100000, COUNT_THREADS_MIN = 2;
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
	Future<AsyncIOTask.Status> submit(final AsyncIOTask<T> request)
	throws RemoteException;
	//
	void handleResult(final AsyncIOTask<T> task, AsyncIOTask.Status status)
	throws RemoteException;
	//
	void join()
	throws RemoteException, InterruptedException;
	//
	void join(final long timeOutMilliSec)
	throws RemoteException, InterruptedException;
	//
}
