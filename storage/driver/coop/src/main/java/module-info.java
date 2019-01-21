import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.coop.mock.CoopStorageDriverMockExtension;

module com.emc.mongoose.storage.driver.coop {
  requires com.emc.mongoose;
  requires confuse;
  requires fiber4j;
  requires log4j.api;
  requires java.commons;
  requires java.base;

  exports com.emc.mongoose.storage.driver.coop;
  exports com.emc.mongoose.storage.driver.coop.mock;

  provides Extension with
      CoopStorageDriverMockExtension;
}
