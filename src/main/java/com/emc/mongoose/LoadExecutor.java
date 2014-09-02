package com.emc.mongoose;
//
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.data.UniformData;
import com.emc.mongoose.data.UniformDataSource;
//
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Locale;
/**
 Created by kurila on 28.04.14.
 */
public interface LoadExecutor<T extends UniformData>
extends Producer<T>, Consumer<T> {
	//
	static int
		METRICS_UPDATE_PERIOD_SEC = RunTimeConfig.getInt("run.metrics.period.sec"),
		REQ_QUEUE_FACTOR = RunTimeConfig.getInt("run.request.queue.factor"),
		BILLION = 1000000000, MIB = 0x100000;
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
	static MessageFormat MSG_FMT_METRICS = new MessageFormat(
		"count=({0,number,integer}/{1,number,integer}/{2,number,integer}); " +
		"duration[s]=({3,number,#.###}/{4,number,#.###}/{5,number,#.###}/{6,number,#.###}); " +
		"TP[/s]=({7,number,#.###}/{8,number,#.###}/{9,number,#.###}/{10,number,#.###}); " +
		"BW[Mib/s]=({11,number,#.###}/{12,number,#.###}/{13,number,#.###}/{14,number,#.###})",
		Locale.ROOT
	);
	//
	String getName()
	throws RemoteException;
	//
	Producer<T> getProducer()
	throws RemoteException;
}
