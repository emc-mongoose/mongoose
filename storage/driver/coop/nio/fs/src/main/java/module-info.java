import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.coop.nio.fs.FileStorageDriverExtension;

module com.emc.mongoose.storage.driver.nio.fs {
	requires com.emc.mongoose.storage.driver.coop.nio;
	requires com.emc.mongoose.storage.driver.coop;
	requires com.emc.mongoose;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.confuse;
	requires com.github.akurilov.confuse.io.json;
	requires log4j.api;
	requires java.base;

	exports com.emc.mongoose.storage.driver.coop.nio.fs;

	provides Extension with
		FileStorageDriverExtension;
}
