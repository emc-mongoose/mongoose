package com.emc.mongoose.base.concurrent;

import com.emc.mongoose.base.exception.InterruptRunException;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modifies the async runnable to make sure that all instances are closed even if an user hits ^C
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

	public static void closeAll() throws InterruptRunException {
		InterruptRunException ex = null;
		synchronized (REGISTRY) {
			for (final WeakReference<Daemon> daemonRef : REGISTRY) {
				final Daemon daemon = daemonRef.get();
				try {
					if (daemon != null && !daemon.isClosed()) {
						daemon.close();
					}
				} catch (final InterruptRunException e) {
					ex = e;
				} catch (final Throwable cause) {
					LOG.log(Level.WARNING, "Failed to close the daemon instance", cause);
				}
			}
			REGISTRY.clear();
		}
		if (ex != null) {
			throw ex;
		}
	}
}
