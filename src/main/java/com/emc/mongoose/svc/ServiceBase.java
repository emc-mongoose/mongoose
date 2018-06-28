package com.emc.mongoose.svc;

import com.emc.mongoose.logging.Loggers;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

public abstract class ServiceBase
extends AsyncRunnableBase
implements Service {

	protected final int port;

	protected ServiceBase(final int port) {
		this.port = port;
	}

	@Override
	public final int registryPort() {
		return port;
	}

	@Override
	protected void doStart() {
		ServiceUtil.create(this, port);
		try {
			Loggers.MSG.info("Service \"{}\" started @ port #{}", name(), port);
		} catch(final RemoteException ignored) {
		}
	}

	@Override
	protected void doShutdown() {
	}

	@Override
	protected void doStop() {
		try {
			ServiceUtil.close(this);
			Loggers.MSG.info("Service \"{}\" stopped listening the port #{}", name(), port);
		} catch(final RemoteException | MalformedURLException e) {
			try {
				throw new RemoteException("Failed to stop the service " + name(), e);
			} catch(final RemoteException ignored) {
			}
		}
	}
}
