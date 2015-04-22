package com.emc.mongoose.core.impl.load.tasks;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Consumer;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 21.04.15.
 */
public final class SubmitTask
implements Runnable, Reusable<SubmitTask> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static InstancePool<SubmitTask>
		SUBMIT_TASK_POOL = new InstancePool<>(SubmitTask.class);
	//
	private Consumer<DataItem> consumer = null;
	private DataItem dataItem = null;
	//
	public static SubmitTask getInstance(
		final Consumer<? extends DataItem> consumer, final DataItem dataItem
	) {
		return SUBMIT_TASK_POOL.take(consumer, dataItem);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final SubmitTask reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		if(args != null) {
			if(args.length > 0) {
				consumer = (Consumer<DataItem>) args[0];
			}
			if(args.length > 1) {
				dataItem = (DataItem) args[1];
			}
		}
		return this;
	}
	//
	@Override
	public final void release() {
		SUBMIT_TASK_POOL.release(this);
	}
	//
	@Override
	public final int compareTo(final SubmitTask o) {
		return o == null ? -1 : hashCode() - o.hashCode();
	}
	//
	@SuppressWarnings("FieldCanBeLocal")
	private int rejectCount;
	private final int
		retryCountMax = RunTimeConfig.getContext().getRunRetryCountMax(),
		retryDelayMilliSec = RunTimeConfig.getContext().getRunRetryDelayMilliSec();
	//
	@Override
	public final void run() {
		rejectCount = 0;
		try {
			do {
				try {
					consumer.submit(dataItem);
					break;
				} catch(final RejectedExecutionException | RemoteException e) {
					rejectCount ++;
					Thread.sleep(rejectCount * retryDelayMilliSec);
				}
			} while(rejectCount < retryCountMax);
		} catch(final InterruptedException e) {
			LogUtil.failure(
				LOG, Level.INFO, e, String.format(
					"Failed to submit the data item \"%s\" to consumer \"%s\"",
					dataItem, consumer
				)
			);
		} finally {
			release();
		}
	}
}
