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
	private volatile boolean isActiveFlag = false;

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
			isActiveFlag = true;
			try {
				invoke();
			} finally {
				isActiveFlag = false;
			}
		}
	}

	@Override
	public final boolean isClosed() {
		return isClosedFlag;
	}

	@Override
	public final boolean isActive() {
		return isActiveFlag;
	}

	protected abstract void invoke();

	protected abstract void doClose()
	throws IOException;
}
