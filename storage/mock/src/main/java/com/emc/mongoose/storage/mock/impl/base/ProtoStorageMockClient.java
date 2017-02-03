package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.impl.http.ChannelFactory;
import com.emc.mongoose.storage.mock.impl.proto.BasicStorageMockServerGrpc;
import com.emc.mongoose.storage.mock.impl.proto.StorageMockProto;
import com.emc.mongoose.ui.log.Markers;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import java.rmi.RemoteException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Dmitry Kossovich on 02.02.17.
 */
public class ProtoStorageMockClient <T extends MutableDataItemMock>
        extends BasicStorageMockServerGrpc.BasicStorageMockServerImplBase
        implements StorageMockClient<T> {

    private static final Logger LOG = LogManager.getLogger();
    private static final ChannelFactory channelFactory = new ChannelFactory();
    private final JmDNS jmDns;
    private final ContentSource contentSource;
    private final BasicStorageMockServerGrpc.BasicStorageMockServerBlockingStub blockingStub;

    public ProtoStorageMockClient(final ContentSource contentSrc, final JmDNS jmDns) {
        this.jmDns = jmDns;
        this.contentSource = contentSrc;

        //TODO may be we need async stub - it seems it will increase perfomance
        blockingStub = BasicStorageMockServerGrpc.newBlockingStub(channelFactory.newChannel(jmDns));
    }

    protected final void doStart()
    throws IllegalStateException {

    }

    //TODO think if we need to return proto class here or convert it into the StorageMockSrever class
    private StorageMockProto.ServerMessage makeQuery(StorageMockProto.ClientMessage request) {
        LOG.info(Markers.MSG, "Making query to a server");

        StorageMockProto.ServerMessage response;
        try {
           response = blockingStub.manageRequest(request);
        } catch (StatusRuntimeException e) {
            LOG.info(Markers.ERR, "RPC failed: {0}", e.getStatus());
            return null;
        }

        return response;
    }

    public final StorageMockProto.ServerMessage makeQuery(
            final String containerName, final String id, final long offset, final long size
    ) {
        StorageMockProto.ClientMessage.Builder builder = StorageMockProto.ClientMessage.newBuilder();
        builder.setContainerName(containerName);
        builder.setId(id);
        builder.setOffset(offset);
        builder.setSize(size);

        return makeQuery(builder.build());
    }

    //TODO write correct implementation
    @Override
    public boolean await(final long timeout, final TimeUnit timeUnit)
            throws InterruptedException, RemoteException {
        return false;
    }

    @Override
    public T getObject(String containerName, String id, long offset, long size) throws ExecutionException, InterruptedException {
        StorageMockProto.ServerMessage serverMessage = makeQuery(containerName, id, offset, size);
        //TODO conversion into T <HOW?!>
        return null;
    }
}
