package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageMockServer;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;
import com.emc.mongoose.storage.mock.impl.remote.MDns;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.storage.mock.impl.http.Nagaina.IDENTIFIER;
import static java.rmi.registry.Registry.REGISTRY_PORT;

/**
 Created on 06.09.16.
 */
public class BasicStorageMockServer<T extends MutableDataItemMock>
extends UnicastRemoteObject
implements StorageMockServer<T> {

	private static final Logger LOG = LogManager.getLogger();

	private final StorageMock<T> storage;
	private final JmDNS jmDns;
	private ServiceInfo serviceInfo;

	public BasicStorageMockServer(final StorageMock<T> storage, final JmDNS jmDns)
	throws RemoteException {
		this.storage = storage;
		this.jmDns = jmDns;
	}

	@Override
	public void start()
	throws UserShootHisFootException {
		try {
			LOG.info(Markers.MSG, "Register RMI service");
			Registry registry = null;
			try {
				registry = LocateRegistry.createRegistry(REGISTRY_PORT);
			} catch(final RemoteException e) {
				try {
					registry = LocateRegistry.getRegistry(REGISTRY_PORT);
				} catch(final RemoteException ie) {
					LogUtil.exception(
						LOG, Level.ERROR, ie, "Failed to obtain RMI registry"
					);
				}
			}
			if (registry != null) {
				registry.rebind(IDENTIFIER, this);
			}
			serviceInfo = ServiceInfo.create(
				MDns.Type.HTTP.toString(), IDENTIFIER,
				MDns.DEFAULT_PORT, "storage mock"
			);
			jmDns.registerService(serviceInfo);
			LOG.info("Nagaina registered as service");
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to register as service"
			);
		}
		storage.start();
	}

	@Override
	public boolean isStarted() {
		return storage.isStarted();
	}

	@Override
	public boolean await()
	throws InterruptedException {
		return storage.await();
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return storage.await(timeout, timeUnit);
	}

	@Override
	public T getObjectRemotely(
		final String containerName, final String id, final long offset, final long size
	) throws ContainerMockException {
		return storage.getObject(containerName, id, offset, size);
	}

	@Override
	public void close()
	throws IOException {
		jmDns.unregisterService(serviceInfo);
		storage.close();
		// Server is the wrapper for StorageMock, but not jmDns.
		// That's why jmDns object should be closed outside this object (Since it may be used
		// by different objects.
	}
}
