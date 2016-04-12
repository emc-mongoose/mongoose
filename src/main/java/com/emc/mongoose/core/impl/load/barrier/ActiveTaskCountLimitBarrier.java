package com.emc.mongoose.core.impl.load.barrier;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.barrier.Barrier;
//
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 04.04.16.
 */
public class ActiveTaskCountLimitBarrier<T extends Item>
implements Barrier<T> {
	//
	private final int activeTaskCountLimit;
	private final AtomicLong counterIn;
	private final AtomicLong counterOut;
	//
	public ActiveTaskCountLimitBarrier(
		final int activeTaskCountLimit, final AtomicLong counterIn, AtomicLong counterOut
	) {
		this.activeTaskCountLimit = activeTaskCountLimit;
		this.counterIn = counterIn;
		this.counterOut = counterOut;

	}
	//
	@Override
	public final boolean getApprovalFor(final T item)
	throws InterruptedException {
		int activeTaskCount;
		do {
			activeTaskCount = (int) (counterIn.get() - counterOut.get());
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
