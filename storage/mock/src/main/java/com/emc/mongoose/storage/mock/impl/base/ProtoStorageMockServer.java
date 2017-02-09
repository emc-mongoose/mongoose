package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.model.DaemonBase;
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
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.storage.mock.impl.http.Nagaina.SVC_NAME;
import static java.rmi.registry.Registry.REGISTRY_PORT;

/**
 * Created by Dmitry Kossovich on 02.02.17.
 */
public class ProtoStorageMockServer<T extends MutableDataItemMock>
        extends DaemonBase
        implements StorageMockServer<T> {

    private static final Logger LOG = LogManager.getLogger();

    private final StorageMock<T> storage;
    private final JmDNS jmDns;
    private ServiceInfo serviceInfo;

    public ProtoStorageMockServer(final StorageMock<T> storage, final JmDNS jmDns)
            throws RemoteException {
        this.storage = storage;
        this.jmDns = jmDns;
    }

    @Override
    protected final void doStart()
            throws IllegalStateException {
        try {
            LOG.info(Markers.MSG, "Register the service");

            // TODO move the registry obtaining section to some kind of init
            try {
                LocateRegistry.createRegistry(REGISTRY_PORT);
            } catch(final RemoteException e) {
                try {
                    LocateRegistry.getRegistry(REGISTRY_PORT);
                } catch(final RemoteException ee) {
                    LogUtil.exception(
                            LOG, Level.ERROR, ee, "Failed to obtain RMI registry"
                    );
                }
            }

            // export
            UnicastRemoteObject.exportObject(this, 0);
            Naming.rebind(SVC_NAME, this);

            serviceInfo = ServiceInfo.create(
                    MDns.Type.HTTP.toString(), SVC_NAME, MDns.DEFAULT_PORT, "storage mock"
            );
            jmDns.registerService(serviceInfo);
            LOG.info("Storage mock was registered as service");
        } catch(final IOException e) {
            LogUtil.exception(
                    LOG, Level.ERROR, e, "Failed to register as service"
            );
        }
        try {
            storage.start();
        } catch(final RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void doShutdown()
            throws IllegalStateException {
        try {
            storage.shutdown();
        } catch(final RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void doInterrupt()
            throws IllegalStateException {
        try {
            UnicastRemoteObject.unexportObject(this, true);
            storage.interrupt();
        } catch(final RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean await(final long timeout, final TimeUnit timeUnit)
            throws InterruptedException, RemoteException {
        return storage.await(timeout, timeUnit);
    }

    @Override
    protected void doClose()
            throws IOException {
        try {
            Naming.unbind(SVC_NAME);
        } catch(final NotBoundException ignored) {
        }
        jmDns.unregisterService(serviceInfo);
    }

    @Override
    public T getObjectRemotely(
            final String containerName, final String id, final long offset, final long size
    ) throws ContainerMockException {
        return storage.getObject(containerName, id, offset, size);
    }
}
