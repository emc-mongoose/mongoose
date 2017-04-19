package com.emc.mongoose.common.concurrent;

import java.io.IOException;
import java.util.List;

/**
 Created by andrey on 19.04.17.
 */
public abstract class SvcTaskBase
implements SvcTask {

	private final List<SvcTask> svcTasks;

	private volatile boolean isClosedFlag = false;

	protected SvcTaskBase(final List<SvcTask> svcTasks) {
		this.svcTasks = svcTasks;
	}

	@Override
	public final void close()
	throws IOException {
		isClosedFlag = true;
		svcTasks.remove(this);
		doClose();
	}

	@Override
	public void run() {
		if(!isClosedFlag) {
			invoke();
		}
	}

	@Override
	public final boolean isClosed() {
		return isClosedFlag;
	}

	protected abstract void invoke();

	protected abstract void doClose()
	throws IOException;
}
