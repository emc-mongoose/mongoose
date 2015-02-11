package com.emc.mongoose.base.load.client.impl.tasks;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.server.LoadSvc;
import com.emc.mongoose.util.collections.InstancePool;
import com.emc.mongoose.util.collections.Reusable;
//
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 Created by kurila on 17.12.14.
 */
public final class FrameFetchTask<T extends List<U>, U extends DataItem>
implements Callable<T>, Reusable {
	//
	private volatile LoadSvc<U> loadSvc = null;
	//
	@Override @SuppressWarnings("unchecked")
	public final T call()
	throws Exception {
		final T frame = (T) loadSvc.takeFrame();
		release();
		return frame;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<FrameFetchTask>
		POOL = new InstancePool<>(FrameFetchTask.class);
	//
	public static FrameFetchTask getInstance(final LoadSvc<?> loadSvc)
	throws InterruptedException {
		return POOL.take(loadSvc);
	}
	//
	private final AtomicBoolean isAvailable = new AtomicBoolean(true);
	//
	@Override @SuppressWarnings("unchecked")
	public final FrameFetchTask<T, U> reuse(final Object... args)
	throws IllegalArgumentException {
		if(isAvailable.compareAndSet(true, false)) {
			if(args==null) {
				throw new IllegalArgumentException("No arguments for reusing the instance");
			}
			if(args.length > 0) {
				loadSvc = (LoadSvc<U>) args[0];
			}
		} else {
			throw new IllegalStateException("Not yet released instance reuse attempt");
		}
		return this;
	}
	//
	@Override
	public final void release() {
		if(isAvailable.compareAndSet(false, true)) {
			POOL.release(this);
		}
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final int compareTo(Reusable another) {
		return another == null ? 1 : hashCode() - another.hashCode();
	}
}
