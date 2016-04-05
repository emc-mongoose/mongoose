package com.emc.mongoose.core.impl.load.barrier;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.barrier.Barrier;
//
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 04.04.16.
 */
public class RateLimitBarrier<T extends Item>
implements Barrier<T> {
	//
	private final long tgtNanoTime;
	//
	public RateLimitBarrier(final float rateLimit) {
		this.tgtNanoTime = rateLimit > 0 && !Float.isInfinite(rateLimit) && !Float.isNaN(rateLimit) ?
			(long) (TimeUnit.SECONDS.toNanos(1) / rateLimit) :
			0;
	}
	//
	@Override
	public final boolean getApprovalFor(final T item)
	throws InterruptedException {
		if(tgtNanoTime > 0) {
			TimeUnit.NANOSECONDS.sleep(tgtNanoTime);
		}
		return true;
	}
	//
	@Override
	public final boolean getApprovalsFor(final T item, final int times)
	throws InterruptedException {
		if(tgtNanoTime > 0 && times > 0) {
			TimeUnit.NANOSECONDS.sleep(tgtNanoTime * times);
		}
		return true;
	}
}
