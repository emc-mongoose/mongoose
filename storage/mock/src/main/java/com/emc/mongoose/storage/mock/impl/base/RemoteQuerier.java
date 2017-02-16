package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.model.item.BasicMutableDataItem;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;
import com.emc.mongoose.storage.mock.impl.proto.ClientMessage;
import com.emc.mongoose.storage.mock.impl.proto.RemoteQuerierGrpc;
import com.emc.mongoose.storage.mock.impl.proto.ServerMessage;
import com.emc.mongoose.ui.log.Markers;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.BitSet;
import java.util.Optional;

/**
 * Created by reddy on 08.02.17.
 */
public class RemoteQuerier<T extends MutableDataItemMock>
        extends RemoteQuerierGrpc.RemoteQuerierImplBase {

    private StorageMock<T> storage;

    public RemoteQuerier(final StorageMock<T> storage) {
        this.storage = storage;
    }

    private static long bitSetToInt(BitSet bitSet) {
        long bitLong = 0;
        for(int i = 0 ; i < 64; i++)
            if(bitSet.get(i))
                bitLong |= (1 << i);
        return bitLong;
    }

    @Override
    public StreamObserver<ClientMessage> getRemoteObject(StreamObserver<ServerMessage> responseObserver) {
        return new StreamObserver<ClientMessage>() {
            @Override
            public void onNext(ClientMessage request) {
                Optional<BasicMutableDataItem> object = Optional.empty();
                try {
                    //there is no class cast error here; it's only type erasing, so java don't sure if it can be cast to T
                    object = Optional.ofNullable(
                            (BasicMutableDataItem) storage.getObject(
                                    request.getContainerName(), request.getId(), request.getOffset(), request.getSize()
                            )
                    );
                } catch (ContainerMockException e) {
                    //TODO think what I'am able to do
                }

                ServerMessage response;
                if (object.isPresent()) {
                    BasicMutableDataItem dataItem = object.get();
                    response = ServerMessage.newBuilder()
                            .setMaskRangesRead(bitSetToInt(dataItem.maskRangesRead))
                            .setLayerNum(dataItem.layer())
                            .setPosition(dataItem.position())
                            .setContainerName(dataItem.getName())
                            .setId(dataItem.getName().substring(dataItem.getName().lastIndexOf('/') + 1))
                            .setOffset(dataItem.offset())
                            .setSize(dataItem.size())
                            .setIsPresent(true)
                            .build();
                } else {
                    //isPresent flag is false here
                    response = ServerMessage.getDefaultInstance();
                }

                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
