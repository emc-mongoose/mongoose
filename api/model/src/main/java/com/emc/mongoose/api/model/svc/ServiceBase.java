package com.emc.mongoose.api.model.svc;

import com.emc.mongoose.api.model.concurrent.AsyncRunnableBase;

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
	public final int registryPort()
	throws RemoteException {
		return port;
	}

	@Override
	protected void doStart() {
		ServiceUtil.create(this, port);
	}

	@Override
	protected void doStop()
	throws RemoteException {
		try {
			ServiceUtil.close(this);
		} catch(final MalformedURLException e) {
			throw new RemoteException("Failed to stop the service " + name(), e);
		}
	}
}
