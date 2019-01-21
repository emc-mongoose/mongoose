import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.coop.netty.NettyStorageDriverExtension;
import com.emc.mongoose.storage.driver.coop.netty.mock.NettyStorageDriverMockExtension;

module com.emc.mongoose.storage.driver.coop.netty {
  requires com.emc.mongoose.storage.driver.coop;
  requires com.emc.mongoose;
  requires confuse;
  requires confuse.io.json;
  requires fiber4j;
  requires java.commons;
  requires log4j.api;
  requires io.netty.buffer;
  requires io.netty.codec;
  requires io.netty.common;
  requires io.netty.handler;
  requires io.netty.transport;
  requires io.netty.transport.epoll;
  requires io.netty.transport.kqueue;
  requires netty.connection.pool;
  requires java.base;

  exports com.emc.mongoose.storage.driver.coop.netty;
  exports com.emc.mongoose.storage.driver.coop.netty.data;
  exports com.emc.mongoose.storage.driver.coop.netty.mock;

  provides Extension with
      NettyStorageDriverExtension,
      NettyStorageDriverMockExtension;
}
