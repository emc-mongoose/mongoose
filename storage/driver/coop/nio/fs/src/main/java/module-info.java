import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.coop.nio.fs.FileStorageDriverExtension;

module com.emc.mongoose.storage.driver.nio.fs {
  requires com.emc.mongoose.storage.driver.coop.nio;
  requires com.emc.mongoose.storage.driver.coop;
  requires com.emc.mongoose;
  requires confuse;
  requires confuse.io.json;
  requires java.commons;
  requires log4j.api;
  requires java.base;

  exports com.emc.mongoose.storage.driver.coop.nio.fs;

  provides Extension with
      FileStorageDriverExtension;
}
