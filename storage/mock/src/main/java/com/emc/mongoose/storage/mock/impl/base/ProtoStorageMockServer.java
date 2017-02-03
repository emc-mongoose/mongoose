package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.api.StorageMockServer;
import com.emc.mongoose.storage.mock.impl.proto.BasicStorageMockServerGrpc;
import com.emc.mongoose.storage.mock.impl.proto.StorageMockProto;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Dmitry Kossovich on 02.02.17.
 */
public class ProtoStorageMockServer<T extends MutableDataItemMock>
        extends BasicStorageMockServerGrpc.BasicStorageMockServerImplBase
        implements StorageMockServer<T> {

    private static final Logger LOG = LogManager.getLogger();

    public ProtoStorageMockClient(final StorageMock<T> storage, final JmDNS jmDns) {

    }

    @Override
    public void manageRequest(
            StorageMockProto.ClientMessage request,
            StreamObserver<StorageMockProto.ServerMessage> response
    ) {

    }

    //TODO write correct implementation
    @Override
    public boolean await(final long timeout, final TimeUnit timeUnit)
            throws InterruptedException, RemoteException {
        return false;
    }
}
