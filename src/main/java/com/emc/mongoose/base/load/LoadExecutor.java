package com.emc.mongoose.base.load;
//
import com.emc.mongoose.base.data.DataItem;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 28.04.14.
 A mechanism of data items load execution.
 May be a consumer and producer both also.
 Supports method "join" for waiting the load execution to be done.
 */
public interface LoadExecutor<T extends DataItem>
extends Producer<T>, Consumer<T> {
	//
	static int BILLION = 1000000000, MIB = 0x100000;
	//
	static String
		METRIC_NAME_SUCC = "succ",
		METRIC_NAME_FAIL = "fail",
		METRIC_NAME_SUBM = "subm",
		METRIC_NAME_REJ = "rej",
		METRIC_NAME_REQ = "req",
		METRIC_NAME_TP = "TP",
		METRIC_NAME_BW = "BW",
		METRIC_NAME_DUR = "dur",
		NAME_SEP = "@";
	//
	static String
		MSG_FMT_METRICS = "count=(%d/%d/%d); duration[s]=(%.6f/%.6f/%.6f/%.6f); " +
			"TP[/s]=(%.3f/%.3f/%.3f/%.3f); BW[MB/s]=(%.3f/%.3f/%.3f/%.3f)",
		MSG_FMT_SUM_METRICS =
			"%s: count=(%d/%d); duration[s]=(%.6f/%.6f/%.6f/%.6f); " +
			"TP[/s]=(%.3f/%.3f/%.3f/%.3f); BW[MB/s]=(%.3f/%.3f/%.3f/%.3f)";
	//
	String getName()
	throws RemoteException;
	//
	Producer<T> getProducer()
	throws RemoteException;
	//
	void join(final long milliSecs)
	throws RemoteException, InterruptedException;
	//
}
