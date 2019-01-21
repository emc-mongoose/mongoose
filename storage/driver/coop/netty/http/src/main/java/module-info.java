import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.coop.netty.http.HttpStorageDriverExtension;

module com.emc.mongoose.storage.driver.coop.netty.http {
  requires com.emc.mongoose.storage.driver.coop.netty;
  requires com.emc.mongoose.storage.driver.coop;
  requires com.emc.mongoose;
  requires confuse;
  requires confuse.io.json;
  requires java.commons;
  requires fiber4j;
  requires log4j.api;
  requires io.netty.codec.http;
  requires io.netty.buffer;
  requires io.netty.handler;
  requires io.netty.transport;
  requires java.base;

  exports com.emc.mongoose.storage.driver.coop.netty.http;

  provides Extension with
      HttpStorageDriverExtension;
}
