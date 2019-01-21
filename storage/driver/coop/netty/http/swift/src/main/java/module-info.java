import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.coop.netty.http.swift.SwiftStorageDriverExtension;

module com.emc.mongoose.storage.driver.coop.netty.http.swift {
  requires com.emc.mongoose.storage.driver.coop.netty.http;
  requires com.emc.mongoose;
  requires confuse;
  requires confuse.io.json;
  requires java.commons;
  requires io.netty.buffer;
  requires io.netty.codec.http;
  requires io.netty.common;
  requires io.netty.transport;
  requires com.fasterxml.jackson.core;
  requires log4j.api;
  requires java.base;

  exports com.emc.mongoose.storage.driver.coop.netty.http.swift;

  provides Extension with
      SwiftStorageDriverExtension;
}
