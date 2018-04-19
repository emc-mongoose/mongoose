import com.emc.mongoose.storage.driver.base.StorageDriverFactory;
import com.emc.mongoose.storage.driver.nio.mock.NioStorageDriverMockFactory;

module com.emc.mongoose.storage.driver.nio.mock {

	requires com.emc.mongoose.storage.driver.base;
	requires com.emc.mongoose.storage.driver.nio;
	requires com.emc.mongoose.ui;
	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.github.akurilov.commons;
	requires java.base;

	provides StorageDriverFactory with NioStorageDriverMockFactory;
}