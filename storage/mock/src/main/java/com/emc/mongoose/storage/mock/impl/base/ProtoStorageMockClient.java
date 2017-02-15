package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.common.concurrent.AnyNotNullSharedFutureTaskBase;
import com.emc.mongoose.common.concurrent.TaskSequencer;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.model.DaemonBase;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.impl.http.ChannelFactory;
import com.emc.mongoose.storage.mock.impl.proto.ClientMessage;
import com.emc.mongoose.storage.mock.impl.proto.RemoteQuerierGrpc;
import com.emc.mongoose.storage.mock.impl.proto.ServerMessage;
import com.emc.mongoose.storage.mock.impl.remote.MDns;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import com.emc.mongoose.ui.log.NamingThreadFactory;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.emc.mongoose.storage.mock.impl.http.Nagaina.SVC_NAME;
import static java.rmi.registry.Registry.REGISTRY_PORT;

/**
 * Created by Dmitry Kossovich on 02.02.17.
 */
public class ProtoStorageMockClient <T extends MutableDataItemMock>
        extends DaemonBase
        implements StorageMockClient<T> {

    private static final Logger LOG = LogManager.getLogger();

    private final ContentSource contentSrc;
    private final JmDNS jmDns;
    private final List<Pair<String, Integer>>
            remoteNodeList = new CopyOnWriteArrayList<>();
    private final ExecutorService executor;

    public ProtoStorageMockClient(final ContentSource contentSrc, final JmDNS jmDns) {
        this.executor = new ThreadPoolExecutor(
                ThreadUtil.getHardwareConcurrencyLevel(), ThreadUtil.getHardwareConcurrencyLevel(),
                0, TimeUnit.DAYS, new ArrayBlockingQueue<>(TaskSequencer.DEFAULT_TASK_QUEUE_SIZE_LIMIT),
                new NamingThreadFactory("storageMockClientWorker", true),
                (r, e) -> LOG.error("Task {} rejected", r.toString())
        ) {
            @Override
            public final Future<T> submit(final Runnable task) {
                final RunnableFuture<T> rf = (RunnableFuture<T>) task;
                execute(rf);
                return rf;
            }
        };
        this.contentSrc = contentSrc;
        this.jmDns = jmDns;
    }

    private static final class GetRemoteObjectTask<T extends MutableDataItemMock>
            extends AnyNotNullSharedFutureTaskBase<T> {

        private final Pair<String, Integer> node;
        private final String containerName;
        private final String id;
        private final long offset;
        private final long size;

        private GetRemoteObjectTask(
                final AtomicReference<T> resultRef, final CountDownLatch sharedLatch,
                final Pair<String, Integer> node, final String containerName, final String id,
                final long offset, final long size
        ) {
            super(resultRef, sharedLatch);
            this.node = node;
            this.containerName = containerName;
            this.id = id;
            this.offset = offset;
            this.size = size;
        }

        private class SingletonResponseWrapper {
            private volatile ServerMessage response;
            private volatile boolean isInitialised = false;

            public ServerMessage getResponse() {
                synchronized (this) {
                    return isInitialised ? response : null;
                }
            }

            public void set(ServerMessage response) {
                synchronized (this) {
                    this.response = response;
                    this.isInitialised = true;
                }
            }
        }

        @Override
        public final void run() {
            try {
                final CountDownLatch finishLatch = new CountDownLatch(1);
                final RemoteQuerierGrpc.RemoteQuerierStub asyncStub;
                asyncStub = RemoteQuerierGrpc.newStub(ChannelFactory.newChannel(
                        node.getKey(), ChannelFactory.getDefaultPort())
                );
                final SingletonResponseWrapper responseContainer = new SingletonResponseWrapper();

                final StreamObserver<ServerMessage> responseObserver = new StreamObserver<ServerMessage>() {
                    @Override
                    public void onNext(ServerMessage value) {
                            responseContainer.set(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                        Status status = Status.fromThrowable(t);
                        LOG.error(Markers.ERR, "gRPC err:" + status);
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        finishLatch.countDown();
                    }
                };

                final StreamObserver<ClientMessage> requestObserver;
                requestObserver = asyncStub.getRemoteObject(responseObserver);

                ClientMessage request = ClientMessage.newBuilder()
                        .setContainerName(containerName)
                        .setId(id)
                        .setOffset(offset)
                        .setSize(size)
                        .build();

                try {
                    requestObserver.onNext(request);
                    if (finishLatch.getCount() == 0) {
                        // RPC completed or errored before we finished sending.
                        // Sending further requests won't error, but they will just be thrown away.
                        return;
                    }
                } catch (RuntimeException e) {
                    // Cancel RPC
                    requestObserver.onError(e);
                    throw e;
                }

                requestObserver.onCompleted();

                try {
                    finishLatch.await();
                } catch (InterruptedException e) {
                    LOG.error(Markers.ERR, "gRPC request has been interrupted: " + e);
                }

                ServerMessage response = responseContainer.getResponse();

                final T remoteObject = (response == null || response.getIsPresent())
                        ? (T) new BasicMutableDataItemMock(
                                response.getContainerName(), response.getOffset(), response.getSize(),
                                response.getLayerNum(), BitSet.valueOf(new long[]{response.getMaskRangesRead()}),
                                response.getPosition(), response.getId()
                            )
                        : null;
                set(remoteObject);
            } catch (StatusRuntimeException e) {
                LOG.info(Markers.ERR, "RPC failed: " + e.getStatus());
                setException(e);
            }
        }
    }

    @Override
    public T getObject(
            final String containerName, final String id, final long offset, final long size
    ) throws ExecutionException, InterruptedException {
        final CountDownLatch sharedCountDown = new CountDownLatch(remoteNodeList.size());
        final AtomicReference<T> resultRef = new AtomicReference<>(null);
        for(final Pair<String, Integer> node : remoteNodeList) {
            executor.submit(
                    new ProtoStorageMockClient.GetRemoteObjectTask<>(
                            resultRef, sharedCountDown, node, containerName, id, offset, size
                    )
            );
        }
        T result;
        while(null == (result = resultRef.get()) && sharedCountDown.getCount() > 0) {
            Thread.sleep(1);
        }
        if(result != null) {
            result.setContentSrc(contentSrc);
        }
        return result;
    }

    @Override
    protected void doStart() {
        jmDns.addServiceListener(MDns.Type.HTTP.toString(), this);
    }

    @Override
    protected void doShutdown() {
        executor.shutdown();
    }

    @Override
    public boolean await(final long timeout, final TimeUnit timeUnit)
            throws InterruptedException {
        return executor.awaitTermination(timeout, timeUnit);
    }

    @Override
    protected void doInterrupt() {
        executor.shutdownNow();
        jmDns.removeServiceListener(MDns.Type.HTTP.toString(), this);
    }

    @Override
    protected void doClose()
            throws IOException {
        remoteNodeList.clear();
    }

    @Override
    public void serviceAdded(final ServiceEvent event) {
        jmDns.requestServiceInfo(event.getType(), event.getName(), 10);
    }

    @Override
    public void serviceRemoved(final ServiceEvent event) {
        handleServiceEvent(event, remoteNodeList::remove, "Node removed");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serviceResolved(final ServiceEvent event) {
        final Consumer<String> c = hostAddress -> {
            final Pair<String, Integer> address = new Pair<>(hostAddress, REGISTRY_PORT);
            if (!remoteNodeList.contains(address)) {
                remoteNodeList.add(address);
            }
        };
        handleServiceEvent(event, c, "Node added");
    }

    private void printNodeList() {
        final StringJoiner joiner = new StringJoiner(",");
        remoteNodeList.forEach(x -> joiner.add(x.toString()));
        LOG.info(Markers.MSG, "Detected nodes: " + joiner.toString());
    }

    private void handleServiceEvent(
            final ServiceEvent event, final Consumer<String> consumer, final String actionMsg
    ) {
        final ServiceInfo eventInfo = event.getInfo();
        if(eventInfo.getQualifiedName().contains(SVC_NAME)) {
            for (final InetAddress address: eventInfo.getInet4Addresses()) {
                try {
                    if(!address.equals(jmDns.getInetAddress())) {
                        consumer.accept(address.getHostAddress());
                        LOG.info(Markers.MSG, actionMsg + ":" + event.getName());
                        printNodeList();
                    }
                } catch(final IOException e) {
                    LogUtil.exception(LOG, Level.ERROR, e, "Failed to get own host address");
                }
            }
        }
    }
}
