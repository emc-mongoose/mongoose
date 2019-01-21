import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.coop.nio.mock.NioStorageDriverMockExtension;

module com.emc.mongoose.storage.driver.coop.nio {
  requires com.emc.mongoose.storage.driver.coop;
  requires com.emc.mongoose;
  requires confuse;
  requires fiber4j;
  requires java.commons;
  requires log4j.api;
  requires java.base;

  exports com.emc.mongoose.storage.driver.coop.nio;
  exports com.emc.mongoose.storage.driver.coop.nio.mock;

  provides Extension with
      NioStorageDriverMockExtension;
}
