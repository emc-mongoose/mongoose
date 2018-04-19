import com.emc.mongoose.storage.driver.base.StorageDriverFactory;
import com.emc.mongoose.storage.driver.coop.mock.CoopStorageDriverMockFactory;

module com.emc.mongoose.storage.driver.coop.mock {

	requires com.emc.mongoose.storage.driver.coop;
	requires com.emc.mongoose.storage.driver.base;
	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.emc.mongoose.ui;
	requires com.github.akurilov.commons;

	provides StorageDriverFactory with CoopStorageDriverMockFactory;
}