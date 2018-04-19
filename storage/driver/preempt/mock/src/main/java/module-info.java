import com.emc.mongoose.storage.driver.base.StorageDriverFactory;
import com.emc.mongoose.storage.driver.preempt.mock.PreemptStorageDriverMockFactory;

module com.emc.mongoose.storage.driver.preempt.mock {

	requires com.emc.mongoose.storage.driver.base;
	requires com.emc.mongoose.storage.driver.preempt;
	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.emc.mongoose.ui;
	requires com.github.akurilov.commons;
	requires java.base;

	provides StorageDriverFactory with PreemptStorageDriverMockFactory;
}