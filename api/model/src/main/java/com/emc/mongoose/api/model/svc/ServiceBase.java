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
	public final int getRegistryPort()
	throws RemoteException {
		return port;
	}

	@Override
	protected void doStart() {
		ServiceUtil.create(this, port);
	}

	@Override
	protected void doStop()
	throws RemoteException, MalformedURLException {
		ServiceUtil.close(this);
	}
}
