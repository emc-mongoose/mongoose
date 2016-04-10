package com.emc.mongoose.core.impl.v1.load.barrier;
//
import com.emc.mongoose.core.api.v1.item.base.Item;
import com.emc.mongoose.core.api.v1.load.barrier.Barrier;
//
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 04.04.16.
 */
public class ActiveTaskCountLimitBarrier<T extends Item>
implements Barrier<T> {
	//
	private final int activeTaskCountLimit;
	private final AtomicLong counterSubm;
	private final AtomicLong counterResults;
	//
	private final AtomicBoolean isInterrupted;
	private final AtomicBoolean isShutdown;
	private final boolean isCircular;
	//
	public ActiveTaskCountLimitBarrier(
		final int activeTaskCountLimit, final AtomicLong counterSubm, AtomicLong counterResults,
		final AtomicBoolean isInterrupted, final AtomicBoolean isShutdown, final boolean isCircular
	) {
		this.activeTaskCountLimit = activeTaskCountLimit;
		this.counterSubm = counterSubm;
		this.counterResults = counterResults;
		//
		this.isInterrupted = isInterrupted;
		this.isShutdown = isShutdown;
		this.isCircular = isCircular;
	}
	//
	@Override
	public final boolean getApprovalFor(final T item)
	throws InterruptedException {
		int activeTaskCount;
		do {
			activeTaskCount = (int) (counterSubm.get() - counterResults.get());
			if(isInterrupted.get() || (isShutdown.get() && !isCircular)) {
				throw new InterruptedException("Submit failed, shut down already or interrupted");
			}
			if(activeTaskCount < activeTaskCountLimit) {
				break;
			}
			LockSupport.parkNanos(activeTaskCount);
		} while(true);
		return true;
	}
	//
	@Override
	public final boolean getApprovalsFor(final T item, final int times)
	throws InterruptedException {
		return getApprovalFor(item);
	}
}
