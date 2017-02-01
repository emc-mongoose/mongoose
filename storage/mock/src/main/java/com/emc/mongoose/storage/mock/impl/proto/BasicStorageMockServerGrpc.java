package com.emc.mongoose.storage.mock.impl.proto;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.0.3)",
    comments = "Source: storage_mock.proto")
public class BasicStorageMockServerGrpc {

  private BasicStorageMockServerGrpc() {}

  public static final String SERVICE_NAME = "com.emc.mongoose.storage.mock.proto.BasicStorageMockServer";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ClientMessage,
      com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ServerMessage> METHOD_MANAGE_REQUEST =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "com.emc.mongoose.storage.mock.proto.BasicStorageMockServer", "ManageRequest"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ClientMessage.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ServerMessage.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static BasicStorageMockServerStub newStub(io.grpc.Channel channel) {
    return new BasicStorageMockServerStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static BasicStorageMockServerBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new BasicStorageMockServerBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static BasicStorageMockServerFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new BasicStorageMockServerFutureStub(channel);
  }

  /**
   */
  public static abstract class BasicStorageMockServerImplBase implements io.grpc.BindableService {

    /**
     */
    public void manageRequest(com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ClientMessage request,
        io.grpc.stub.StreamObserver<com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ServerMessage> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_MANAGE_REQUEST, responseObserver);
    }

    @java.lang.Override public io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_MANAGE_REQUEST,
            asyncUnaryCall(
              new MethodHandlers<
                com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ClientMessage,
                com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ServerMessage>(
                  this, METHODID_MANAGE_REQUEST)))
          .build();
    }
  }

  /**
   */
  public static final class BasicStorageMockServerStub extends io.grpc.stub.AbstractStub<BasicStorageMockServerStub> {
    private BasicStorageMockServerStub(io.grpc.Channel channel) {
      super(channel);
    }

    private BasicStorageMockServerStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BasicStorageMockServerStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new BasicStorageMockServerStub(channel, callOptions);
    }

    /**
     */
    public void manageRequest(com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ClientMessage request,
        io.grpc.stub.StreamObserver<com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ServerMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_MANAGE_REQUEST, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class BasicStorageMockServerBlockingStub extends io.grpc.stub.AbstractStub<BasicStorageMockServerBlockingStub> {
    private BasicStorageMockServerBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private BasicStorageMockServerBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BasicStorageMockServerBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new BasicStorageMockServerBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ServerMessage manageRequest(com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ClientMessage request) {
      return blockingUnaryCall(
          getChannel(), METHOD_MANAGE_REQUEST, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class BasicStorageMockServerFutureStub extends io.grpc.stub.AbstractStub<BasicStorageMockServerFutureStub> {
    private BasicStorageMockServerFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private BasicStorageMockServerFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BasicStorageMockServerFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new BasicStorageMockServerFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ServerMessage> manageRequest(
        com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ClientMessage request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_MANAGE_REQUEST, getCallOptions()), request);
    }
  }

  private static final int METHODID_MANAGE_REQUEST = 0;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final BasicStorageMockServerImplBase serviceImpl;
    private final int methodId;

    public MethodHandlers(BasicStorageMockServerImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_MANAGE_REQUEST:
          serviceImpl.manageRequest((com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ClientMessage) request,
              (io.grpc.stub.StreamObserver<com.emc.mongoose.storage.mock.impl.proto.StorageMockProto.ServerMessage>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    return new io.grpc.ServiceDescriptor(SERVICE_NAME,
        METHOD_MANAGE_REQUEST);
  }

}
