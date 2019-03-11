package com.emc.mongoose.base.logging;

import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;

public final class ShutdownCallbackRegistryImpl implements ShutdownCallbackRegistry {

	@Override
	public final Cancellable addShutdownCallback(final Runnable callback) {
		return new CancellableImpl(callback);
	}

	private static final class CancellableImpl implements Cancellable {

		private final Runnable callback;

		public CancellableImpl(final Runnable callback) {
			this.callback = callback;
		}

		@Override
		public final void cancel() {}

		@Override
		public final void run() {
			if (callback != null) {
				System.out.println("Shutdown callback + \"" + callback + "\" run...");
				callback.run();
			}
		}
	}
}
