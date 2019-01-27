import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.storage.driver.preempt.mock.PreemptStorageDriverMockExtension;

module com.emc.mongoose.storage.driver.preempt {
	requires com.emc.mongoose.base;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.confuse;
	requires log4j.api;

	exports com.emc.mongoose.storage.driver.preempt;
	exports com.emc.mongoose.storage.driver.preempt.mock;

	provides Extension with
		PreemptStorageDriverMockExtension;
}
