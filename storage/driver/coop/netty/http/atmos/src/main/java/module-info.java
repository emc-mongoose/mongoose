import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.coop.netty.http.atmos.AtmosStorageDriverExtension;

module com.emc.mongoose.storage.driver.coop.netty.http.atmos {
  requires com.emc.mongoose.storage.driver.coop.netty.http;
  requires com.emc.mongoose;
  requires confuse;
  requires confuse.io.json;
  requires java.commons;
  requires io.netty.buffer;
  requires io.netty.codec.http;
  requires io.netty.common;
  requires io.netty.transport;
  requires log4j.api;
  requires java.base;

  exports com.emc.mongoose.storage.driver.coop.netty.http.atmos;

  provides Extension with
      AtmosStorageDriverExtension;
}
