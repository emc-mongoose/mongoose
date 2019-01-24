import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.coop.nio.mock.NioStorageDriverMockExtension;

module com.emc.mongoose.storage.driver.coop.nio {
	requires com.emc.mongoose.storage.driver.coop;
	requires com.emc.mongoose;
	requires com.github.akurilov.confuse;
	requires com.github.akurilov.fiber4j;
	requires com.github.akurilov.commons;
	requires log4j.api;
	requires java.base;

	exports com.emc.mongoose.storage.driver.coop.nio;
	exports com.emc.mongoose.storage.driver.coop.nio.mock;

	provides Extension with
		NioStorageDriverMockExtension;
}
