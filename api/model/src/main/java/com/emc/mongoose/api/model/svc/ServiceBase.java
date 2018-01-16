package com.emc.mongoose.api.model.svc;

import java.io.IOException;
import java.rmi.RemoteException;

public abstract class ServiceBase
implements Service {

	protected final int port;

	protected ServiceBase(final int port) {
		this.port = port;
	}

	protected void start() {
		ServiceUtil.create(this, port);
	}

	@Override
	public final int getRegistryPort()
	throws RemoteException {
		return port;
	}

	@Override
	public void close()
	throws IOException {
		ServiceUtil.close(this);
	}
}
