import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.preempt.mock.PreemptStorageDriverMockExtension;

module com.emc.mongoose.storage.driver.preempt {
  requires com.emc.mongoose;
  requires confuse;
  requires java.commons;
  requires log4j.api;

  exports com.emc.mongoose.storage.driver.preempt;
  exports com.emc.mongoose.storage.driver.preempt.mock;

  provides Extension with
      PreemptStorageDriverMockExtension;
}
