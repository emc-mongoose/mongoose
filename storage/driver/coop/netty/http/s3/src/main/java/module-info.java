import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.coop.netty.http.s3.AmzS3StorageDriverExtension;

module com.emc.mongoose.storage.driver.coop.netty.http.s3 {
  requires com.emc.mongoose.storage.driver.coop.netty.http;
  requires com.emc.mongoose;
  requires confuse;
  requires confuse.io.json;
  requires java.commons;
  requires io.netty.buffer;
  requires io.netty.common;
  requires io.netty.codec.http;
  requires io.netty.transport;
  requires log4j.api;
  requires java.base;
  requires java.xml;

  exports com.emc.mongoose.storage.driver.coop.netty.http.s3;

  provides Extension with
      AmzS3StorageDriverExtension;
}
