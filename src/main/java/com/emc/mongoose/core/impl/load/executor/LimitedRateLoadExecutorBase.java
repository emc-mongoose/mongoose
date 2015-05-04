package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 04.05.15.
 The extension of load executor which is able to sustain the rate (throughput, item/sec) not higher
 than the specified limit.
 */
public abstract class LimitedRateLoadExecutorBase<T extends DataItem>
extends LoadExecutorBase<T> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	private final float rateLimit;
	private final int tgtMicroDur;
	//
	protected LimitedRateLoadExecutorBase(
		final RunTimeConfig runTimeConfig, final RequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias,
		final float rateLimit
	) throws ClassCastException {
		super(
			runTimeConfig, reqConfig, addrs, connCountPerNode, listFile, maxCount,
			sizeMin, sizeMax, sizeBias
		);
		//
		if(rateLimit < 0) {
			throw new IllegalArgumentException("Frequency rate limit shouldn't be a negative value");
		}
		this.rateLimit = rateLimit;
		if(rateLimit > 0) {
			tgtMicroDur = (int) (1e6 * addrs.length * connCountPerNode / rateLimit);
		} else {
			tgtMicroDur = 0;
		}
	}
	/**
	 Adds the optional delay calculated from last successfull I/O task duration and the target
	 duration
	 */
	@Override
	public void submit(final T dataItem)
	throws InterruptedException, RemoteException, RejectedExecutionException {
		if(rateLimit > 0 && throughPut.getMeanRate() > rateLimit) {
			TimeUnit.MICROSECONDS.sleep(tgtMicroDur);
		}
		super.submit(dataItem);
	}
}
