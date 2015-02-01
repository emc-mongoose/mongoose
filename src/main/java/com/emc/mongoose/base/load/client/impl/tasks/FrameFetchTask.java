package com.emc.mongoose.base.load.client.impl.tasks;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.server.LoadSvc;
import com.emc.mongoose.util.pool.InstancePool;
import com.emc.mongoose.util.pool.Reusable;
//
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 Created by kurila on 17.12.14.
 */
public final class FrameFetchTask<T extends List<? extends DataItem>>
implements Callable<T>, Reusable {
	//
	private volatile LoadSvc<?> loadSvc = null;
	//
	@Override @SuppressWarnings("unchecked")
	public final T call()
		throws Exception {
		return (T) loadSvc.takeFrame();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<FrameFetchTask>
		POOL = new InstancePool<>(FrameFetchTask.class);
	//
	public static FrameFetchTask getInstance(final LoadSvc<?> loadSvc) {
		return POOL.take(loadSvc);
	}
	//
	private final AtomicBoolean isClosed = new AtomicBoolean(true);
	//
	@Override
	public final FrameFetchTask<T> reuse(final Object... args)
	throws IllegalArgumentException {
		if(isClosed.compareAndSet(true, false)) {
			if(args==null) {
				throw new IllegalArgumentException("No arguments for reusing the instance");
			}
			if(args.length > 0) {
				loadSvc = LoadSvc.class.cast(args[0]);
			}
		} else {
			throw new IllegalStateException("Not yet released instance reuse attempt");
		}
		return this;
	}
	//
	@Override
	public final void close() {
		if(isClosed.compareAndSet(false, true)) {
			POOL.release(this);
		}
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final int compareTo(Reusable another) {
		return another == null ? 1 : hashCode() - another.hashCode();
	}
}
