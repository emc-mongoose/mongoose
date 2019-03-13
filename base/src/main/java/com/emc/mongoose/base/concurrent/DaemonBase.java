package com.emc.mongoose.base.concurrent;

import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.emc.mongoose.base.Exceptions.throwUncheckedIfInterrupted;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

/**
* Modifies the async runnable to make sure that all instances are closed even if a user hits ^C
*/
public abstract class DaemonBase extends AsyncRunnableBase implements Daemon {

	private static final Logger LOG = Logger.getLogger(DaemonBase.class.getSimpleName());
	private static final Queue<WeakReference<Daemon>> REGISTRY = new ConcurrentLinkedQueue<>();

	private final WeakReference<Daemon> daemonRef;

	protected DaemonBase() {
		daemonRef = new WeakReference<>(this);
		REGISTRY.add(daemonRef);
	}

	@Override
	public final void close() throws IOException {
		super.close();
		REGISTRY.remove(daemonRef);
	}

	public static void closeAll() {
		InterruptedException ex = null;
		synchronized (REGISTRY) {
			for (final var daemonRef : REGISTRY) {
				final var daemon = daemonRef.get();
				try {
					if (daemon != null && !daemon.isClosed()) {
						daemon.close();
					}
				} catch (final Throwable cause) {
					if(cause instanceof InterruptedException) {
						ex = (InterruptedException) cause;
					}
					LOG.log(Level.WARNING, "Failed to close the daemon instance", cause);
				}
			}
			REGISTRY.clear();
		}
		if (ex != null) {
			throwUnchecked(ex);
		}
	}
}
