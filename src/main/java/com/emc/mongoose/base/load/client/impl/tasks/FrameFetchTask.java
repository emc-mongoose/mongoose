package com.emc.mongoose.base.load.client.impl.tasks;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.server.LoadSvc;
//
import java.util.List;
import java.util.concurrent.Callable;
/**
 Created by kurila on 17.12.14.
 */
public final class FrameFetchTask<T extends List<? extends DataItem>>
implements Callable<T> {
	//
	private final LoadSvc<?> loadSvc;
	//
	public FrameFetchTask(final LoadSvc<?> loadSvc) {
		this.loadSvc = loadSvc;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final T call()
		throws Exception {
		return (T) loadSvc.takeFrame();
	}
}
