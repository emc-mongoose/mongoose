package com.emc.mongoose.base.load;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Locale;
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
	//
	void join(final long milliSecs)
	throws RemoteException, InterruptedException;
	//
}
