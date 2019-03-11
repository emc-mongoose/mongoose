package com.emc.mongoose.base.svc;

import com.github.akurilov.commons.concurrent.AsyncRunnable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** Created by kurila on 07.05.14. A remote service which has a name for resolution by URI. */
public interface Service extends AsyncRunnable, Remote {

	int registryPort() throws RemoteException;

	String name() throws RemoteException;
}
